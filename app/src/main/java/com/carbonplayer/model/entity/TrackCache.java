package com.carbonplayer.model.entity;


import android.content.Context;

import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.entity.primitive.Null;

import java.io.File;
import java.io.IOException;

import io.realm.RealmList;
import io.realm.RealmObject;
import rx.Observable;
import timber.log.Timber;

/**
 * Created by ethanelshyeb on 7/20/17.
 */

public class TrackCache extends RealmObject {
    public RealmList<MusicTrack> cachedTracks;

    public static boolean has(Context context, SongID id, StreamQuality quality){
        String[] cacheFiles = context.getCacheDir().list((dir, name) -> name.startsWith(id.getId()));
        if(cacheFiles.length == 0) return false;
        if(cacheFiles.length > 1) removeLowerQualities(context, id);
        return true;
    }

    public static Observable<Null> removeLowerQualities(Context context) {
        return Observable.create(subscriber -> {
            File[] cacheFiles = context.getCacheDir().listFiles();
            for (File f: cacheFiles) {
                String id = f.getName().substring(0, f.getName().length()-2);
                removeLowerQualities(context, new SongID(id, null, null));
            }
            subscriber.onCompleted();
        });
    }


    /**
     * Removes all lower qualities of a song specified by {@param id}
     * @param context Context to use
     * @param id {@link SongID} of the song to delete
     * @return the remaining high quality file
     */
    public static File removeLowerQualities(Context context, SongID id){
        File[] cacheFiles = context.getCacheDir().listFiles((dir, name) -> name.startsWith(id.getId()));

        if(cacheFiles.length == 0) return null;

        int maxQuality = 0;
        for(File f: cacheFiles){
            int fileQuality = f.getName().charAt(f.getName().length()-1);
            if(fileQuality > maxQuality) maxQuality = fileQuality;
        }

        File maxQ = null;

        for (File f: cacheFiles) {
            int fileQuality = f.getName().charAt(f.getName().length()-1);
            if(fileQuality < maxQuality) {
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

    public static File getTrackFile(Context context, SongID id){
        return removeLowerQualities(context, id);
    }

}
