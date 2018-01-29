package com.carbonplayer.model.entity;


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.LongSparseArray;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.enums.PlaySource;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.entity.primitive.Null;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;


public class TrackCache {

    public static boolean has(Context context, SongID id, StreamQuality quality) {

        Timber.d("Does trackCache have %d?", id.getLocalId());

        File[] cacheFiles = context.getCacheDir()
                .listFiles((dir, name) -> name.startsWith(String.valueOf(id.getLocalId())));

        if (cacheFiles.length == 0) {
            Timber.d("Will run!");
            return false;
        }
        if (cacheFiles.length > 1) {
            File newFile = removeLowerQualities(cacheFiles);
            String name = newFile == null ? null : newFile.getName();

            if (name != null && Integer.parseInt(
                    String.valueOf(name.charAt(name.length() - 1))) >= quality.ordinal()) {
                Timber.d("We're good!");
                return true;
            }

            Timber.d("Escaped!!");
        }

        return cacheFiles.length == 1;
    }

    public static void evictCache(Context context, long targetSize) {

        Realm realm = Realm.getDefaultInstance();
        RealmResults<Track> res = realm.where(Track.class)
                .equalTo(Track.HAS_CACHED_FILE, true)
                .equalTo(Track.STORAGE_TYPE, StorageType.CACHE.ordinal())
                .findAll();

        File cacheDir = context.getCacheDir();
        File[] fileList = cacheDir.listFiles();

       LongSparseArray<LinkedList<File>> fileMap = new LongSparseArray<>();
        for (File f : fileList) {
            Long id = Long.parseLong(f.getName().split("--")[0]);
            if (!(fileMap.get(id) == null)) {
                LinkedList<File> lis = new LinkedList<>();
                lis.add(f);
                fileMap.put(id, lis);
            } else {
                fileMap.get(id).add(f);
            }
        }

        long cacheSize = dirSize(cacheDir);
        long spaceNeeded = Math.max(0, cacheSize - targetSize);
        long averageSize = (cacheSize / res.size()) / 100;
        long averageImportance = 50;
        float needClearingPercent = spaceNeeded / cacheSize;

        float clearNumber = (averageImportance * averageSize) * needClearingPercent;

        if (spaceNeeded == 0) return;

        realm.executeTransaction(rlm -> {
            for (Track track : res) {
                long size = track.getLocalTrackSizeBytes();
                long importance = track.getCacheImportance(PlaySource.SONGS);

                if (size * importance < clearNumber) {
                    track.setHasCachedFile(false);
                    for (File f2 : fileMap.get(track.getLocalId())) {
                        Timber.d("Deleting cached track %s (quality %s), success: %b",
                                track.toString(),
                                String.valueOf(f2.getName().charAt(f2.getName().length() - 1)),
                                f2.delete());
                    }
                } else {
                    int highestQuality = 0;
                    for (File f2 : fileMap.get(track.getLocalId())) {
                        int qual = Integer.parseInt(String.valueOf(
                                f2.getName().charAt(f2.getName().length() - 1)));
                        if (qual > highestQuality) {
                            highestQuality = qual;
                        }
                    }

                    for (File f2 : fileMap.get(track.getLocalId())) {
                        int qual = Integer.parseInt(String.valueOf(
                                f2.getName().charAt(f2.getName().length() - 1)));
                        if (qual < highestQuality) {
                            Timber.d("Deleting cached track %s (quality %s), success: %b",
                                    track.toString(),
                                    String.valueOf(f2.getName().charAt(f2.getName().length() - 1)),
                                    f2.delete());
                        }
                    }
                }
            }
        });
    }

    private static long dirSize(File dir) {

        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                if (fileList[i].isDirectory()) {
                    result += dirSize(fileList[i]);
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length();
                }
            }
            return result; // return the file size
        }
        return 0;
    }

    public static Observable<Null> removeLowerQualities(Context context) {
        return Observable.create(subscriber -> {
            File[] cacheFiles = context.getCacheDir().listFiles();
            for (File f : cacheFiles) {
                String id = f.getName().substring(0, f.getName().length() - 2);
                removeLowerQualities(context, new SongID(Long.parseLong(id), null, null, null, null));
            }
            subscriber.onComplete();
        });
    }

    private static File removeLowerQualities(File[] cacheFiles) {
        if (cacheFiles.length == 0) return null;

        int maxQuality = 0;
        for (File f : cacheFiles) {
            int fileQuality = f.getName().charAt(f.getName().length() - 1);
            if (fileQuality > maxQuality) maxQuality = fileQuality;
        }

        File maxQ = null;

        for (File f : cacheFiles) {
            int fileQuality = f.getName().charAt(f.getName().length() - 1);
            if (fileQuality < maxQuality) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    f.getCanonicalFile().delete();
                } catch (IOException e) {
                    Timber.e(e, "Error deleting file %s", f.getName());
                }
            } else {
                maxQ = f;
            }
        }

        return maxQ;
    }

    /**
     * Removes all lower qualities of a song specified by {@param id}
     *
     * @param context Context to use
     * @param id      {@link SongID} of the song to delete
     * @return the remaining high quality file
     */
    public static File removeLowerQualities(Context context, SongID id) {
        File[] cacheFiles = context.getCacheDir().listFiles(
                (dir, name) -> name.startsWith(String.valueOf(id.getLocalId())));
        return removeLowerQualities(cacheFiles);
    }

    @NonNull public static File getTrackFile(Context context, SongID id, StreamQuality newQuality) {
        File existingFile = removeLowerQualities(context, id);
        if (existingFile != null) return existingFile;

        if (newQuality == null) {
            newQuality = CarbonPlayerApplication.Companion.getInstance()
                    .getPreferences().getPreferredStreamQuality(context);
        }

        return new File(context.getCacheDir(), id.getLocalId() + "--" +
                String.valueOf(newQuality.ordinal()));
    }

}
