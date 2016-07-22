package com.carbonplayer.model;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.RealmString;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observable;
import timber.log.Timber;

public final class MusicLibrary {

    private static MusicLibrary instance;

    private Realm realm;

    private MusicLibrary(){
        realm = Realm.getDefaultInstance();
    }

    public static MusicLibrary getInstance(){
        if(instance == null) instance = new MusicLibrary();
        return instance;
    }

    public void saveTracks(final List<MusicTrack> tracks){
        final LinkedList<Album> albums = new LinkedList<>();
        for(MusicTrack track : tracks){
            boolean exists = false;
            for(Album album : albums){
                if(track.getAlbumId().equals(album.getId())){
                    exists = true;
                    break;
                }
            }
            if(!exists){
                Album a = new Album(track.getAlbumId(), track.getRecentTimestamp(),
                        track.getAlbum(), track.getArtist(),
                        track.getComposer(), track.getYear(),
                        track.getGenre(), track.getAlbumArtURL(),
                        track.getArtistId(),
                        new RealmList<>(new RealmString(track.getTrackId()))
                );
                albums.addFirst(a);
            }
        }
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (MusicTrack track : tracks) {
                    realm.copyToRealm(track);
                    Timber.d("copied %s", track.toString());
                }
                realm.copyToRealm(albums);
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Timber.e(error, "Could not save tracks");
            }
        });
    }

    /**
     * Loads the news feed as well as all future updates.
     */
    @UiThread
    public Observable<RealmResults<Album>> loadAlbums() {

        // Return the data in Realm.
        return realm.where(Album.class)
                .findAllSortedAsync(Album.TITLE, Sort.DESCENDING)
                .asObservable();
    }
}
