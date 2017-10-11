package com.carbonplayer.model.entity;


import android.content.Context;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.enums.PlaySource;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.entity.primitive.Null;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;
import timber.log.Timber;


public class TrackCache {

    public static boolean has(Context context, SongID id, StreamQuality quality) {

        File[] cacheFiles = context.getCacheDir()
                .listFiles((dir, name) -> name.startsWith(id.getId()));

        if (cacheFiles.length == 0) return false;
        if (cacheFiles.length > 1) {
            File newFile = removeLowerQualities(cacheFiles);
            String name = newFile == null ? null : newFile.getName();

            if (name != null && Integer.parseInt(
                    String.valueOf(name.charAt(name.length() - 1))) >= quality.ordinal()) {
                return true;
            }
        }

        return false;
    }

    public static void evictCache(Context context, long targetSize) {

        Realm realm = Realm.getDefaultInstance();
        RealmResults<MusicTrack> res = realm.where(MusicTrack.class)
                .equalTo(MusicTrack.HAS_CACHED_FILE, true)
                .equalTo(MusicTrack.STORAGE_TYPE, StorageType.CACHE.ordinal())
                .findAll();

        File cacheDir = context.getCacheDir();
        File[] fileList = cacheDir.listFiles();

        HashMap<String, LinkedList<File>> fileMap = new HashMap<>();
        for (File f : fileList) {
            String id = f.getName().split("--")[0];
            if (!fileMap.containsKey(id)) {
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
            for (MusicTrack track : res) {
                long size = track.getLocalTrackSizeBytes();
                long importance = track.getCacheImportance(PlaySource.SONGS);

                if (size * importance < clearNumber) {
                    track.setHasCachedFile(false);
                    for (File f2 : fileMap.get(track.getTrackId())) {
                        Timber.d("Deleting cached track %s (quality %s), success: %b",
                                track.toString(),
                                String.valueOf(f2.getName().charAt(f2.getName().length() - 1)),
                                f2.delete());
                    }
                } else {
                    int highestQuality = 0;
                    for (File f2 : fileMap.get(track.getTrackId())) {
                        int qual = Integer.parseInt(String.valueOf(
                                f2.getName().charAt(f2.getName().length() - 1)));
                        if (qual > highestQuality) {
                            highestQuality = qual;
                        }
                    }

                    for (File f2 : fileMap.get(track.getTrackId())) {
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
                removeLowerQualities(context, new SongID(id, null, null));
            }
            subscriber.onCompleted();
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
                (dir, name) -> name.startsWith(id.getId()));
        return removeLowerQualities(cacheFiles);
    }

    public static File getTrackFile(Context context, SongID id, StreamQuality newQuality) {
        File existingFile = removeLowerQualities(context, id);
        if (existingFile != null) return existingFile;

        if (newQuality == null) {
            newQuality = CarbonPlayerApplication.Companion.getInstance()
                    .getPreferences().getPreferredStreamQuality(context);
        }

        return new File(context.getCacheDir(), id.getId() + "--" +
                String.valueOf(newQuality.ordinal()));
    }

}
