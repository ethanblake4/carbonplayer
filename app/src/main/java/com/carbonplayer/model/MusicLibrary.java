package com.carbonplayer.model;

import android.support.annotation.UiThread;

import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.RealmString;

import java.util.LinkedList;
import java.util.List;

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

    public void saveTracksAsync(final List<MusicTrack> tracks){
        final LinkedList<Album> albums = new LinkedList<>();
        for(MusicTrack track : tracks){
            if(track.getAlbumId() == null) continue;

            Album exists = null;
            for(Album album : albums){
                if(track.getAlbumId().equals(album.getId()) ||
                        (track.getArtist().equals(album.getArtist()) && track.getAlbum().equals(album.getTitle())) ){
                    exists = album;
                    break;
                }
            }
            if(exists == null){
                Album a = new Album(track.getAlbumId(), track.getRecentTimestamp(),
                        track.getAlbum(), track.getArtist(),
                        track.getComposer(), track.getYear(),
                        track.getGenre(), track.getAlbumArtURL(),
                        track.getArtistId(),
                        new RealmList<>(new RealmString(track.getTrackId()))
                );
                albums.addFirst(a);
            }else{
                exists.addSongId(track.getTrackId());
            }
        }
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                /*for (MusicTrack track : tracks) {
                    realm.copyToRealm(track);
                }*/
                realm.copyToRealm(tracks);
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
     * Loads the tracks.
     */
    @UiThread
    public Observable<RealmResults<Album>> loadAlbums() {

        // Return the data in Realm.
        return realm.where(Album.class)
                .findAllSortedAsync(Album.TITLE, Sort.ASCENDING)
                .asObservable();
    }

    public MusicTrack getTrack(String id){
        return realm.where(MusicTrack.class)
                .equalTo(MusicTrack.ID, id)
                .findFirst();
    }
}
