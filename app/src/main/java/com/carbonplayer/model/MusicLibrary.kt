package com.carbonplayer.model

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.UiThread
import android.util.Pair
import com.carbonplayer.model.entity.*
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.exception.NoNautilusException
import com.carbonplayer.model.entity.primitive.FinalBool
import com.carbonplayer.model.entity.primitive.RealmInteger
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.utils.addToAutoDispose
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.realm.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger


object MusicLibrary {

    private const val UNKNOWN_ARTIST_ID = "unknownArtistId"
    const val UNKNOWN_ALBUM_ID = "unknownAlbumId"

    fun config(context: Activity, onError: Consumer<Throwable>, onSuccess: Action) {
        val failed = FinalBool()
        Protocol.getConfig(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { entries ->
                    for (e in entries) {
                        if (e.name == "isNautilusUser")
                            if (e.value == "false") {
                                failed.set(true)
                                onError.accept(NoNautilusException())
                            }
                    }

                    if (!failed.get()) onSuccess.run()

                }, onError).addToAutoDispose()
    }

    @SuppressLint("CheckResult")
    @UiThread
    fun getMusicLibrary(context: Activity, onError: Consumer<Throwable>, update: Boolean,
                        onProgress: Consumer<Pair<Boolean, Int>>, onSuccess: Action) {
        val trackObservable = Protocol.listTracks(context)

                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        val received = AtomicInteger()

        trackObservable.subscribe(Consumer { tracks ->
            Realm.getDefaultInstance().executeTransactionAsync { realm ->
                addToDatabase(realm, tracks, update, received)
            }
            onProgress.accept(Pair(false, received.get()))
        }, onError, Action { updatePlaylists(context, onError, onProgress, onSuccess) }).addToAutoDispose()
    }


    private fun addToDatabase(realm: Realm, tracks: List<SkyjamTrack>, update: Boolean = true,
                              received: AtomicInteger? = null): List<Track> {

        val outTracks = mutableListOf<Track>()

        tracks.forEach { sjTrack ->

            received?.incrementAndGet()
            outTracks.add(addOneToDatabase(realm, sjTrack, update))
        }

        return outTracks
    }

    fun addOneToDatabase(realm: Realm, sjTrack: SkyjamTrack, update: Boolean = true,
                         inLibrary: Boolean = true): Track {
        val artistPairs = mutableMapOf<String, String>()
        var treatAsOneArtist = false

        if (sjTrack.artistId != null) {

            val featuring = sjTrack.title.substringAfter(" (feat. ", "!")
                    .takeIf { it != "!" }?.substringBefore(")")
                    ?.split(", ")?.flatMap { it.split(" & ") }
                    ?.flatMap { it.split(" and" ) } ?:

                    sjTrack.title.substringAfter(" with ", "!")
                        .takeIf { it != "!" }
                        ?.split(", ")?.flatMap { it.split(" & ") }
                        ?.flatMap { it.split(" and ") } ?: listOf()


            val _artists = sjTrack.artist.split(", ").flatMap { it.split(" & ") }
            // For most cases
            if (_artists.size == sjTrack.artistId.size) {
                (0 until _artists.size).forEach {
                    artistPairs[sjTrack.artistId[it]] = _artists[it]
                }
            } else if (_artists.size > sjTrack.artistId.size) {
                // Fixes e.g. "Tom Petty & The Heartbreakers"
                val noTheSize = _artists.size -
                        _artists.count { it.startsWith("the", true) } +
                        if (_artists[0].startsWith("The")) 1 else 0
                if (noTheSize == sjTrack.artistId.size) {
                    (0 until noTheSize).forEach {
                        if (it == 0 || !_artists[it].startsWith("the", true)) {
                            artistPairs[sjTrack.artistId[it]] = _artists[it]
                        }
                    }
                } else treatAsOneArtist = true  // Fallback: treat as a single artist
            }
        }

        val artists = mutableListOf<Artist>()
        val managedArtists = mutableListOf<Artist>()

        if (treatAsOneArtist) {
            artists.add(realm.where(Artist::class.java)
                    .equalTo(Artist.ID, sjTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID)
                    .findFirst() ?:
                    Artist(sjTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID, sjTrack.artist))
        } else {
            artistPairs.forEach { (id, name) ->
                artists.add(realm.where(Artist::class.java)
                        .equalTo(Artist.ID, id)
                        .findFirst() ?: Artist(id, name, sjTrack.artistArtRef)
                )
            }
        }

        sjTrack.inLibrary = inLibrary

        val track = if (update) insertOrUpdateTrack(realm, sjTrack)
        else realm.copyToRealm(Track(nextLocalIdForTrack(realm), sjTrack))

        val album = realm.where(Album::class.java)
                .equalTo(Album.ID, sjTrack.albumId)
                .or().beginGroup()
                .equalTo(Album.NAME, sjTrack.album)
                .contains("artists.name", sjTrack.artist)
                .endGroup()
                .findFirst()?.apply {
                    this.tracks.add(track)
                    val s = this.tracks.distinctBy { track ->
                        track.title + track.trackNumber.toString()
                    }
                    this.tracks.clear()
                    this.tracks.addAll(s)
                } ?: realm.copyToRealm(Album(sjTrack, track, inLibrary))

        artists.forEach {
            if (!it.albums.contains(album)) {
                it.albums.add(album)
            }
            managedArtists.add(if (!it.isManaged) realm.copyToRealm(it) else it)
        }

        return track
    }

    fun addOneToDatabase(realm: Realm, pTrack: ParcelableTrack, update: Boolean = true,
                         inLibrary: Boolean = true): Track {
        val artistPairs = mutableMapOf<String, String>()
        var treatAsOneArtist = false

        if (pTrack.artistId != null) {

            val featuring = pTrack.title.substringAfter(" (feat. ", "!")
                    .takeIf { it != "!" }?.substringBefore(")")
                    ?.split(", ")?.flatMap { it.split(" & ") }
                    ?.flatMap { it.split(" and" ) }
                    ?: pTrack.title.substringAfter(" with ", "!")
                    .takeIf { it != "!" }
                    ?.split(", ")?.flatMap { it.split(" & ") }
                    ?.flatMap { it.split(" and ") } ?: listOf()


            val _artists = pTrack.artist.split(", ").flatMap { it.split(" & ") }

            val combinedArtists = mutableListOf<String>().apply {
                addAll(_artists)
                addAll(featuring)
            }

            // For most cases
            if (_artists.size == pTrack.artistId.size) {
                (0 until _artists.size).forEach {
                    artistPairs[pTrack.artistId[it]] = _artists[it]
                }
            } else if (_artists.size > pTrack.artistId.size) {
                // Fixes e.g. "Tom Petty & The Heartbreakers"
                val noTheSize = _artists.size -
                        _artists.count { it.startsWith("the", true) } +
                        if (_artists[0].startsWith("The")) 1 else 0
                if (noTheSize == pTrack.artistId.size) {
                    (0 until noTheSize).forEach {
                        if (it == 0 || !_artists[it].startsWith("the", true)) {
                            artistPairs[pTrack.artistId[it]] = _artists[it]
                        }
                    }
                } else treatAsOneArtist = true  // Fallback: treat as a single artist
            }
        }

        val artists = mutableListOf<Artist>()
        val managedArtists = mutableListOf<Artist>()

        if (treatAsOneArtist) {
            artists.add(realm.where(Artist::class.java)
                    .equalTo(Artist.ID, pTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID)
                    .findFirst() ?:
            Artist(pTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID, pTrack.artist))
        } else {
            artistPairs.forEach { (id, name) ->
                artists.add(realm.where(Artist::class.java)
                        .equalTo(Artist.ID, id)
                        .findFirst() ?: Artist(id, name, pTrack.artistArtRef?.map { Image(
                        kind = "sj#image",
                        url = it)})
                )
            }
        }

        pTrack.inLibrary = inLibrary

        val track = if (update) insertOrUpdateTrack(realm, pTrack)
        else realm.copyToRealm(Track(nextLocalIdForTrack(realm), pTrack))

        val album = realm.where(Album::class.java)
                .equalTo(Album.ID, pTrack.albumId)
                .or().beginGroup()
                .equalTo(Album.NAME, pTrack.album)
                .contains("artists.name", pTrack.artist)
                .endGroup()
                .findFirst()?.apply {
            this.tracks.add(track)
            val s = this.tracks.distinctBy { track ->
                track.title + track.trackNumber.toString()
            }
            this.tracks.clear()
            this.tracks.addAll(s)
        } ?: realm.copyToRealm(Album(pTrack, track, inLibrary))

        artists.forEach {
            if (!it.albums.contains(album)) {
                it.albums.add(album)
            }
            managedArtists.add(if (!it.isManaged) realm.copyToRealm(it) else it)
        }

        return track
    }

    fun processArtists(src: List<SkyjamArtist>, realm: Realm): RealmList<Artist> {
        val lis = RealmList<Artist>()
        src.forEach {
            lis.add(realm.where(Artist::class.java)
                    .equalTo(Artist.ID, it.artistId)
                    .findFirst() ?: Artist(it, realm))
        }
        return lis
    }

    /**
     * Transforms a list of [Album]s into a RealmList of [Album]s
     */
    fun processAlbums(albums: List<Album>, realm: Realm): RealmList<Album> {
        val out = RealmList<Album>()

        albums.forEach { a ->
            out.add(realm.where(Album::class.java)
                    .equalTo(Album.ID, a.albumId)
                    .or().beginGroup()
                    .equalTo(Album.NAME, a.name)
                    .apply {
                        a.artists?.forEachIndexed { i, artist ->
                            if (i != 0) or()
                            contains("artists.name", artist.name)
                        }
                    }
                    .endGroup()
                    .findFirst()?.updateFrom(a, realm)
                    ?: realm.copyToRealm(a))
        }

        return out
    }

    /**
     * Transforms a list of [SkyjamTrack]s into a RealmList of [Track]s
     */
    fun processTracks(tracks: List<SkyjamTrack>, realm: Realm): RealmList<Track> {
        return tracks.mapTo(RealmList<Track>()) {
            realm.where(Track::class.java)
                    .equalTo(Track.TRACK_ID, it.id)
                    .or().equalTo(Track.CLIENT_ID, it.clientId)
                    .or().equalTo(Track.NAUTILUS_ID, it.nid)
                    .or().equalTo(Track.STORE_ID, it.storeId)
                    .findFirst() ?: Track(nextLocalIdForTrack(realm), it)
        }
    }

    private fun nextLocalIdForTrack(realm: Realm) =
            realm.where(Track::class.java).max(Track.LOCAL_ID)?.toLong()?.plus(1) ?: 0

    private fun nextLocalIdForPlaylist(realm: Realm) =
            realm.where(Playlist::class.java).max(Playlist.LOCAL_ID)?.toLong()?.plus(1) ?: 0


    private fun insertOrUpdateTrack(realm: Realm, src: SkyjamTrack): Track {

        var trackQ = realm.where(Track::class.java)
        var nmo = false
        src.id?.let { trackQ = trackQ.equalTo(Track.TRACK_ID, it); nmo = true }
        src.clientId?.let {
            trackQ = if (nmo) trackQ.or().equalTo(Track.CLIENT_ID, it)
            else {
                nmo = true
                trackQ.equalTo(Track.CLIENT_ID, it)
            }
        }
        src.storeId?.let {
            trackQ = if (nmo) trackQ.or().equalTo(Track.STORE_ID, it)
            else trackQ.equalTo(Track.STORE_ID, it)
        }

        return trackQ.findFirst()?.updateWith(src) ?: realm.copyToRealm(Track(nextLocalIdForTrack(realm), src))

    }

    private fun insertOrUpdateTrack(realm: Realm, src: ParcelableTrack): Track {

        var trackQ = realm.where(Track::class.java)
        var nmo = false
        src.id?.let { trackQ = trackQ.equalTo(Track.TRACK_ID, it); nmo = true }
        src.clientId?.let {
            trackQ = if (nmo) trackQ.or().equalTo(Track.CLIENT_ID, it)
            else {
                nmo = true
                trackQ.equalTo(Track.CLIENT_ID, it)
            }
        }
        src.storeId?.let {
            trackQ = if (nmo) trackQ.or().equalTo(Track.STORE_ID, it)
            else trackQ.equalTo(Track.STORE_ID, it)
        }

        return trackQ.findFirst()?.updateWith(src) ?: realm.copyToRealm(Track(nextLocalIdForTrack(realm), src))

    }

    private fun insertOrUpdatePlaylist(realm: Realm, src: SkyjamPlaylist): Playlist {

        var playlistQ = realm.where(Playlist::class.java)
        var nmo = false
        src.id?.let { playlistQ = playlistQ.equalTo(Playlist.REMOTE_ID, it); nmo = true }
        src.clientId?.let {
            playlistQ = if (nmo) playlistQ.or().equalTo(Playlist.CLIENT_ID, it)
            else playlistQ.equalTo(Playlist.CLIENT_ID, it)
        }

        val existingPl = playlistQ.findFirst()

        if (existingPl != null) {
            realm.insertOrUpdate(Playlist(src, existingPl.localId))
        }

        return existingPl ?: realm.copyToRealm(Playlist(src, nextLocalIdForPlaylist(realm)))
    }

    private fun updatePlaylists(context: Activity, onError: Consumer<Throwable>,
                                onProgress: Consumer<Pair<Boolean, Int>>, onSuccess: Action) {
        val plObservable = Protocol.listPlaylists(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        val received = RealmInteger()
        plObservable.subscribe(Consumer { plList ->
            received.set(received.value() + plList.size)
            Realm.getDefaultInstance().executeTransactionAsync { realm ->
                plList.forEach { pl ->
                    insertOrUpdatePlaylist(realm, pl)
                }
            }
            onProgress.accept(Pair(true, received.value()))
        }, onError, Action { updatePlentries(context, onError, onSuccess) }).addToAutoDispose()
    }

    private fun updatePlentries(context: Activity, onError: Consumer<Throwable>, onSuccess: Action) {
        val plentryObservable = Protocol.listPlaylistEntries(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        plentryObservable.subscribe(Consumer { lis ->
            Realm.getDefaultInstance().executeTransactionAsync { realm ->

                lis.forEach { sjPlentry ->

                    val track = sjPlentry.track?.let {
                        insertOrUpdateTrack(realm, it)
                    }

                    val plentry = PlaylistEntry(sjPlentry, track)

                    val playlist = realm.where(Playlist::class.java)
                            .equalTo(Playlist.REMOTE_ID, sjPlentry.playlistId)
                            .findFirst()

                    if (playlist == null) Timber.d("updatePlentries could not find PLID ${sjPlentry.playlistId}")

                    playlist?.entries?.add(realm.copyToRealm(plentry))
                }

            }
        }, onError, onSuccess).addToAutoDispose()
    }

    /**
     * Loads the albums.
     */
    @UiThread
    fun loadAlbums(): Flowable<RealmResults<Album>> {
        return Realm.getDefaultInstance().where(Album::class.java)
                .equalTo("inLibrary", true)
                .sort("name", Sort.ASCENDING)
                .findAllAsync()
                .asFlowable()
    }

    @UiThread
    fun loadPlaylists(): Flowable<RealmResults<Playlist>> {
        return Realm.getDefaultInstance().where(Playlist::class.java)
                .sort("name", Sort.ASCENDING)
                .findAllAsync()
                .asFlowable()
    }

    @UiThread
    fun loadArtists(): Flowable<RealmResults<Artist>> {
        return Realm.getDefaultInstance().where(Artist::class.java)
                .sort(Artist.NAME, Sort.ASCENDING)
                .findAllAsync()
                .asFlowable()
    }

    fun getAllAlbumTracks(album: IAlbum): List<ITrack> {
        return if (album is Album)
            album.tracks.sort(Track.TRACK_NUMBER)
        else (album as SkyjamAlbum).tracks?.sortedBy {
            it.trackNumber
        } ?: listOf()

    }

    fun getTrack(id: String): Track? {
        return Realm.getDefaultInstance().where(Track::class.java)
                .equalTo(Track.LOCAL_ID, id)
                .findFirst()
    }


}
