package com.carbonplayer.model.entity;


import android.content.Context;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.enums.PlaySource;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.entity.primitive.Null;

import java.io.File;
import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;
import timber.log.Timber;


public class TrackCache {

    public static boolean has(Context context, SongID id, StreamQuality quality) {
        String[] cacheFiles = context.getCacheDir().list((dir, name) -> name.startsWith(id.getId()));
        if (cacheFiles.length == 0) return false;
        if (cacheFiles.length > 1) removeLowerQualities(context, id);
        return true;
    }

    public static void evictCache(Context context, long targetSize) {

        Realm realm = Realm.getDefaultInstance();
        RealmResults<MusicTrack> res = realm.where(MusicTrack.class)
                .equalTo(MusicTrack.HAS_CACHED_FILE, true)
                .equalTo(MusicTrack.STORAGE_TYPE, StorageType.CACHE.ordinal())
                .findAll();

        long cacheSize = dirSize(context.getCacheDir());
        //long
        long averageSize = cacheSize / res.size();
        long averageImportance = 50;

        for(MusicTrack track : res) {
            long size = track.getLocalTrackSizeBytes();
            long importance = track.getCacheImportance(PlaySource.SONGS);



        }
    }

    private static long dirSize(File dir) {

        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for(int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                if(fileList[i].isDirectory()) {
                    result += dirSize(fileList [i]);
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


    /**
     * Removes all lower qualities of a song specified by {@param id}
     *
     * @param context Context to use
     * @param id      {@link SongID} of the song to delete
     * @return the remaining high quality file
     */
    public static File removeLowerQualities(Context context, SongID id) {
        File[] cacheFiles = context.getCacheDir().listFiles((dir, name) -> name.startsWith(id.getId()));

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

    public static File getTrackFile(Context context, SongID id, StreamQuality newQuality) {
        File existingFile = removeLowerQualities(context, id);
        if (existingFile != null) return existingFile;

        if (newQuality == null) {
            newQuality = CarbonPlayerApplication.Companion.getInstance()
                    .getPreferences().getPreferredStreamQuality(context);
        }

        return new File(context.getCacheDir(), id.getId() + "--" + String.valueOf(newQuality.ordinal()));
    }

}
