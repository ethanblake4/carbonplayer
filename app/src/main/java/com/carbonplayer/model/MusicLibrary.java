package com.carbonplayer.model;

import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.RealmString;
import com.carbonplayer.model.entity.StdCallback;

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

    public static LinkedList<Album> getAlbumsFromTracks(List<MusicTrack> tracks){
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
        return albums;
    }

    public void saveTracksAsync(final List<MusicTrack> tracks, @Nullable StdCallback callback){
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(tracks);
            }
        },transactionError(null, "Could not save tracks"));
    }

    public void saveAlbumsAsync(final List<Album> albums, @Nullable StdCallback callback){
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(albums);
            }
        }, transactionError(null, "Could not save albums"));
    }

    /**
     * Loads the albums.
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

    private Realm.Transaction.OnError transactionError(@Nullable final StdCallback in, final String errMessage){
        if(in != null){
            return new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    in.onError(error);
                }
            };
        }
        return new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Timber.e(error, (errMessage == null ? error.toString() : errMessage));
            }
        };
    }

    private Realm.Transaction.OnSuccess transactionSuccess(@Nullable final StdCallback in, final String message){
        if(in != null){
            return new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    in.onSuccess();
                }
            };
        }
        return new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Timber.d(message);
            }
        };
    }


}
