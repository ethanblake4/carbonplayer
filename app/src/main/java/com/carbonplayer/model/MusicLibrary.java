package com.carbonplayer.model;

import com.carbonplayer.model.entity.MusicTrack;

import java.util.List;

import io.realm.Realm;
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
