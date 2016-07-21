package com.carbonplayer.model;

import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.RealmString;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
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
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (MusicTrack track : tracks) {
                    realm.copyToRealm(track);
                    Album persistedAlbum = realm.where(Album.class).equalTo(Album.ID, track.getAlbumId()).findFirst();
                    if(persistedAlbum == null){
                        Album a = new Album(track.getAlbumId(), track.getRecentTimestamp(),
                                track.getAlbum(), track.getArtist(),
                                track.getComposer(), track.getYear(),
                                track.getGenre(), track.getAlbumArtURL(),
                                track.getArtistId(),
                                new RealmList<>(new RealmString(track.getTrackId()))
                        );

                        realm.copyToRealm(a);
                    }else{
                        persistedAlbum.addSongId(track.getTrackId());
                    }
                }
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Timber.e(error, "Could not save tracks");
            }
        });
    }
}
