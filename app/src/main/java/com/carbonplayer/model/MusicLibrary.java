package com.carbonplayer.model;

import android.app.Activity;
import android.support.annotation.UiThread;
import android.util.Pair;

import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.Artist;
import com.carbonplayer.model.entity.ConfigEntry;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.Playlist;
import com.carbonplayer.model.entity.PlaylistEntry;
import com.carbonplayer.model.entity.exception.NoNautilusException;
import com.carbonplayer.model.entity.primitive.FinalBool;
import com.carbonplayer.model.entity.primitive.RealmInteger;
import com.carbonplayer.model.network.Protocol;

import java.util.List;

import io.realm.Realm;
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

    private MusicLibrary() {
        realm = Realm.getDefaultInstance();
    }

    public static MusicLibrary getInstance() {
        if (instance == null) instance = new MusicLibrary();
        return instance;
    }

    public void config(Activity context, Action1<Throwable> onError, Action0 onSuccess) {
        final FinalBool failed = new FinalBool();
        Protocol.INSTANCE.getConfig(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(entries -> {
                            for (ConfigEntry e : entries) {
                                if (e.getName().equals("isNautilusUser"))
                                    if (e.getValue().equals("false")) {
                                        failed.set(true);
                                        onError.call(new NoNautilusException());
                                    }
                            }

                            if (!failed.get()) onSuccess.call();

                        },
                        onError);
    }

    @UiThread
    public void updateMusicLibrary(Activity context, Action1<Throwable> onError,
                                   Action1<Pair<Boolean, Integer>> onProgress, Action0 onSuccess) {
        Observable<List<MusicTrack>> trackObservable = Protocol.listTracks(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        final RealmInteger received = new RealmInteger();
        trackObservable.subscribe(trackList -> {
                    realm.executeTransactionAsync(realm -> {
                        realm.insertOrUpdate(trackList);
                        Album a = null;
                        Artist ar;
                        for (MusicTrack track : trackList) {

                            received.increment();

                            if (track.getArtistId() == null) {
                                ar = realm.where(Artist.class)
                                        .equalTo("artistId", "unknownID")
                                        .findFirst();
                                if (ar == null) {
                                    ar = new Artist("unknownID", "Unknown Artist");
                                    realm.insert(ar);
                                }
                            } else {
                                ar = realm.where(Artist.class)
                                        .equalTo("artistId", track.getArtistId().first().getValue())
                                        .findFirst();
                                if (ar == null) {
                                    ar = new Artist(track.getArtistId().first().getValue(), track);
                                    realm.insert(ar);
                                }
                            }

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
                                else {
                                    Album ma = new Album(track);
                                    realm.insert(ma);
                                    if ((ar != null ? ar.getAlbums() : null) != null)
                                        ar.getAlbums().add(ma);
                                }
                            }

                        }
                    });
                    onProgress.call(new Pair<>(false, received.value()));
                },
                onError,
                () -> updatePlaylists(context, onError, onProgress, onSuccess));
    }

    private void updatePlaylists(Activity context, Action1<Throwable> onError,
                                 Action1<Pair<Boolean, Integer>> onProgress, Action0 onSuccess) {
        Observable<List<Playlist>> plObservable = Protocol.listPlaylists(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        final RealmInteger received = new RealmInteger();
        plObservable.subscribe(plList -> {
                    received.set(received.value() + plList.size());
                    realm.executeTransactionAsync(realm -> realm.insertOrUpdate(plList));
                    onProgress.call(new Pair<>(true, received.value()));
                },
                onError,
                () -> updatePlentries(context, onError, onSuccess));
    }

    private void updatePlentries(Activity context, Action1<Throwable> onError, Action0 onSuccess) {
        Observable<List<PlaylistEntry>> plentryObservable = Protocol.listPlaylistEntries(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        plentryObservable.subscribe(plentryList ->
                        realm.executeTransactionAsync(realm -> realm.insertOrUpdate(plentryList)),
                onError, onSuccess);
    }

    private static boolean albumMatchesTrack(Album a, MusicTrack t) {
        if (a == null || t == null) return false;
        return (t.getAlbumId() != null && a.getId() != null && a.getId().equals(t.getAlbumId())) ||
                (t.getAlbum() != null && a.getArtist().equals(t.getArtist()) && a.getTitle().equals(t.getAlbum()));
    }

    /**
     * Loads the albums.
     */
    @UiThread
    public Observable<RealmResults<Album>> loadAlbums() {
        return realm.where(Album.class)
                .findAllSortedAsync("title", Sort.ASCENDING)
                .asObservable();
    }

    @UiThread
    public Observable<RealmResults<Playlist>> loadPlaylists() {
        return realm.where(Playlist.class)
                .findAllSortedAsync("name", Sort.ASCENDING)
                .asObservable();
    }

    @UiThread
    public Observable<RealmResults<Artist>> loadArtists() {
        return realm.where(Artist.class)
                .findAllSortedAsync(Artist.Companion.getNAME(), Sort.ASCENDING)
                .asObservable();
    }

    public RealmResults<MusicTrack> getAllAlbumTracks(String albumId) {
        return realm.where(MusicTrack.class)
                .equalTo("albumId", albumId)
                .findAllSorted("trackNumber", Sort.ASCENDING);
    }

    public MusicTrack getTrack(String id) {
        return realm.where(MusicTrack.class)
                .equalTo(MusicTrack.ID, id)
                .findFirst();
    }


}
