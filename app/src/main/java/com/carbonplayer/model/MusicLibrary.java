package com.carbonplayer.model;

import android.app.Activity;
import android.support.annotation.UiThread;

import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.ConfigEntry;
import com.carbonplayer.model.entity.primitive.FinalBool;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.exception.NoNautilusException;
import com.carbonplayer.model.entity.primitive.RealmInteger;
import com.carbonplayer.model.network.Protocol;

import java.util.LinkedList;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

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

    public void config(Activity context, Action1<Throwable> onError, Action0 onSuccess){
        final FinalBool failed = new FinalBool();
        Protocol.getConfig(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(entries -> {
                    for (ConfigEntry e : entries)
                        if (e.getName().equals("isNautilusUser"))
                            if (e.getValue().equals("false")) {
                                failed.set(true);
                                onError.call(new NoNautilusException());
                            }
                },
                onError,
                () -> {if (!failed.get()) onSuccess.call();} );
    }

    @UiThread
    public void updateMusicLibrary(Activity context, Action1<Throwable> onError, Action1<Integer> onProgress, Action0 onSuccess) {
        Observable<LinkedList<MusicTrack>> trackObservable = Protocol.listTracks(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        final RealmInteger received = new RealmInteger();
        trackObservable.subscribe(trackList -> {
            realm.executeTransactionAsync(realm-> {
                    realm.insertOrUpdate(trackList);
                    Album a = null;
                    for (MusicTrack track : trackList) {
                        received.increment();
                        String mAlbumID;
                        mAlbumID = track.getAlbumId() == null ? "unknownID" : track.getAlbumId();
                        if (albumMatchesTrack(a, track))
                            a.addSong(track.getTrackId());
                        else {
                            a = realm.where(Album.class).equalTo("id", mAlbumID)
                                    .or().beginGroup()
                                    .equalTo("id", track.getAlbum())
                                    .equalTo("artist", track.getArtist())
                                    .endGroup()
                                    .findFirst();
                            if (a != null) a.addSong(track.getTrackId());
                            else realm.insert(new Album(track));
                        }
                    }
            });
            onProgress.call(received.value());
        },
        onError,
        onSuccess);
    }

    private static boolean albumMatchesTrack(Album a, MusicTrack t){
        if(a==null || t==null) return false;
        return ( t.getAlbumId() != null && a.getId() != null && a.getId().equals(t.getAlbumId()) ) ||
                (t.getAlbum() != null && a.getArtist().equals(t.getArtist()) && a.getTitle().equals(t.getAlbum()));
    }

    /**
     * Loads the albums.
     */
    @UiThread
    public Observable<RealmResults<Album>> loadAlbums() {

        // Return the data in Realm.
        return realm.where(Album.class)
                .findAllSortedAsync("title", Sort.ASCENDING)
                .asObservable();
    }

    public RealmResults<MusicTrack> getAllAlbumTracks(String albumId){
        return realm.where(MusicTrack.class)
                .equalTo("albumId", albumId)
                .findAllSorted("trackNumber", Sort.ASCENDING);
    }

    public MusicTrack getTrack(String id){
        return realm.where(MusicTrack.class)
                .equalTo(MusicTrack.ID, id)
                .findFirst();
    }


}
