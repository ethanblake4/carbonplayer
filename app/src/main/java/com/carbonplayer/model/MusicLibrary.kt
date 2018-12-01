package com.carbonplayer.model

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Pair
import androidx.annotation.UiThread
import com.carbonplayer.model.entity.*
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.MutateOperation
import com.carbonplayer.model.entity.exception.NoNautilusException
import com.carbonplayer.model.entity.primitive.FinalBool
import com.carbonplayer.model.entity.primitive.RealmInteger
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.utils.addToAutoDispose
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.Sort
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


object MusicLibrary {

    private const val UNKNOWN_ARTIST_ID = "unknownArtistId"
    const val UNKNOWN_ALBUM_ID = "unknownAlbumId"

    private var cachedLocalTrackId: AtomicLong? = null

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
            if(tracks.isPresent) {
                Realm.getDefaultInstance().executeTransactionAsync { realm ->
                    addToDatabase(realm, tracks.get(), update, received)
                }
                received.addAndGet(tracks.get().size)
                onProgress.accept(Pair(false, received.get()))
            }
        }, onError, Action { updatePlaylists(context, onError, onProgress, onSuccess) }).addToAutoDispose()
    }


    private fun addToDatabase(realm: Realm, tracks: List<SkyjamTrack>, update: Boolean = true,
                              received: AtomicInteger? = null): List<Track> {

        val outTracks = mutableListOf<Track>()

        tracks.forEach { sjTrack ->

            //received?.incrementAndGet()
            outTracks.add(addOneToDatabase(realm, sjTrack, update))
        }

        return outTracks
    }

    private fun extractFeaturing(track: ITrack): List<String> {

        // Song (feat. Artist)
        return track.title.substringAfter(" (feat. ", "!")
                .takeIf { it != "!" }?.substringBefore(")")
                ?.split(", ")?.flatMap { it.split(" & ") }
                ?.flatMap { it.split(" and" ) } ?:

        // Song with Artist
        track.title.substringAfter(" with ", "!")
                .takeIf { it != "!" }
                ?.split(", ")?.flatMap { it.split(" & ") }
                ?.flatMap { it.split(" and ") } ?:

        // Song feat. Artist
        track.title.substringAfter(" feat. ", "!")
                .takeIf { it != "!" }
                ?.split(", ")?.flatMap { it.split(" & ") }
                ?.flatMap { it.split(" and" ) } ?:

        listOf()
    }

    fun addOneToDatabase(realm: Realm, sjTrack: SkyjamTrack, update: Boolean = true,
                         inLibrary: Boolean = true): Track {
        val artistPairs = mutableMapOf<String, String>()
        var treatAsOneArtist = false

        if (sjTrack.artistId != null) {

            val featuring = extractFeaturing(sjTrack)

            val _artists = sjTrack.artist.split(", ").flatMap { it.split(" & ") }

            val combinedArtists = mutableListOf<String>().apply {
                addAll(_artists)
                addAll(featuring)
            }

            // For most cases
            if (combinedArtists.size == sjTrack.artistId.size) {
                (0 until combinedArtists.size).forEach {
                    artistPairs[sjTrack.artistId[it]] = combinedArtists[it]
                }
            } else if (combinedArtists.size > sjTrack.artistId.size) {
                // If there are more artists detected than there are artist IDs
                // Fixes e.g. "Tom Petty & The Heartbreakers"
                val noTheSize = combinedArtists.size -
                        combinedArtists.count { it.startsWith("the", true) } +
                        if (combinedArtists[0].startsWith("The")) 1 else 0
                if (noTheSize == sjTrack.artistId.size) {
                    (0 until noTheSize).forEach {
                        if (it == 0 || !combinedArtists[it].startsWith("the", true)) {
                            artistPairs[sjTrack.artistId[it]] = combinedArtists[it]
                        } else {
                            val carp = artistPairs[sjTrack.artistId[it-1]]
                            artistPairs[sjTrack.artistId[it-1]] = carp + " & " + combinedArtists[it]
                        }
                    }
                } else treatAsOneArtist = true  // Fallback: treat as a single artist
            } else treatAsOneArtist = true
        }

        Timber.d(artistPairs.toList().joinToString())

        val artists = mutableListOf<Artist>()
        val managedArtists = mutableListOf<Artist>()

        if (treatAsOneArtist) {
            artists.add(realm.where(Artist::class.java)
                    .equalTo(Artist.ID, sjTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID)
                    .findFirst() ?:
                    Artist(sjTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID, sjTrack.artist,
                            inLibrary))
        } else {
            var fst = true
            artistPairs.forEach {  (id, name) ->

                artists.add(realm.where(Artist::class.java)
                        .equalTo(Artist.ID, id)
                        .findFirst() ?: Artist(id, name, sjTrack.artistArtRef,
                        inLibrary && fst)
                )

                fst = false
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
                    this.inLibrary = this.inLibrary || inLibrary
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
            if(!it.artistTracks.contains(track)) {
                it.artistTracks.add(track)
            }
            it.inLibrary = it.inLibrary || inLibrary
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
            var nmo = false
            realm.where(Track::class.java)
                    // Attempt to find the track in the database using its store or library ID
                    .let { q -> if(it.storeId != null) {
                            nmo = true
                            q.equalTo(Track.STORE_ID, it.storeId)
                        } else q }
                    .let { q -> if(it.id != null) {
                            if (nmo) q.or().equalTo(Track.TRACK_ID, it.id)
                            else q.equalTo(Track.TRACK_ID, it.id)
                        } else q }
                    // If we can't find it, construct a new track
                    .findFirst() ?: Track(nextLocalIdForTrack(realm), it)
        }
    }

    private fun nextLocalIdForTrack(realm: Realm) =
            cachedLocalTrackId?.incrementAndGet() ?:
            { (realm.where(Track::class.java).max(Track.LOCAL_ID)?.toLong()
                    ?.plus(1) ?: 0).let { cachedLocalTrackId = AtomicLong(it); it }}()

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
        val netsecComplete = AtomicBoolean(false)
        plObservable.subscribe(Consumer { plList -> if(plList.isPresent) {
            received.set(received.value() + plList.get().size)
            Realm.getDefaultInstance().executeTransactionAsync { realm ->
                plList.get().forEach { pl ->
                    pl.albumArtRef?.forEach {
                        Timber.d(it.url)
                    }
                    if (pl.albumArtRef == null) Timber.d("NULL albumartref")
                    insertOrUpdatePlaylist(realm, pl)
                }
                if (netsecComplete.get()) updatePlentries(context, onError, onSuccess)
            }
            onProgress.accept(Pair(true, received.value()))
        } else {
            onProgress.accept(Pair(true, received.value()))
            updatePlentries(context, onError, onSuccess)
        } }, onError, Action { netsecComplete.set(true) }).addToAutoDispose()
    }

    private fun updatePlentries(context: Activity, onError: Consumer<Throwable>, onSuccess: Action) {
        val plentryObservable = Protocol.listPlaylistEntries(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        plentryObservable.subscribe(Consumer { lis -> if(lis.isPresent) {
            Realm.getDefaultInstance().executeTransactionAsync { realm ->

                lis.get().forEach { sjPlentry ->

                    val track = sjPlentry.track?.let {
                        addOneToDatabase(realm, it, true, it.inLibrary)
                    }

                    val plentry = PlaylistEntry(sjPlentry, track)

                    val playlist = realm.where(Playlist::class.java)
                            .equalTo(Playlist.REMOTE_ID, sjPlentry.playlistId!!)
                            .findFirst()

                    if (playlist == null) Timber.d(
                            "updatePlentries could not find PLID ${sjPlentry.playlistId}")

                    playlist?.entries?.add(realm.copyToRealmOrUpdate(plentry))
                }

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

    fun loadSampleAlbums(): List<Album> = SampleMusicLibrary.albums()

    fun loadSampleTracks(artist:String = "Bellevue", album:String = "In Mediums") =
            SampleMusicLibrary.tracks(artist, album)

    @UiThread
    fun loadPlaylists(): Flowable<RealmResults<Playlist>> {
        return Realm.getDefaultInstance().where(Playlist::class.java)
                .sort("name", Sort.ASCENDING)
                .findAllAsync()
                .asFlowable()
    }

    @UiThread
    fun loadSongs(): Flowable<RealmResults<Track>> {
        return Realm.getDefaultInstance().where(Track::class.java)
                .sort("title", Sort.ASCENDING)
                .equalTo("inLibrary", true)
                .findAllAsync()
                .asFlowable()
    }

    @UiThread
    fun loadPlentries(context: Context, playlist: IPlaylist): Observable<List<ITrack>> {
        Timber.d("Loading plentries")
        if(playlist is SkyjamPlaylist) {
            Timber.d("Loading from network")
            return Protocol.getSharedPlentries(context, playlist.shareToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { Observable.fromIterable(it) }
                    .filter { it.track != null }
                    .map { it.track!! as ITrack }
                    .toList()
                    .toObservable()

        } else {
            Timber.d("Loading from db")
            val realm = Realm.getDefaultInstance()
            return (playlist as Playlist).entries.asFlowable()
                    .toObservable()
                    .flatMap { Observable.fromIterable(it) }
                    .map { it.trackId }.take(playlist.entries.size.toLong())
                    .toList()
                    .map {
                        Timber.d(it.toString())
                        val arr = it.toTypedArray()

                        realm.where(Track::class.java)
                            .`in`(Track.STORE_ID, arr)
                            .or()
                            .`in`(Track.TRACK_ID, arr)
                            .findAll()
                            .asFlowable()
                            .toObservable()
                    }.flatMapObservable { it }
        }
    }

    fun addToLibrary(context: Context, track: SkyjamTrack): Completable {
        track.inLibrary = true
        return Protocol.mutateEntity(context, track, MutateOperation.CREATE)
                .andThen(Completable.fromCallable {
                    Realm.getDefaultInstance().executeTransaction { realm ->
                        addOneToDatabase(realm, track, true, true)
                    }
                })
                .doOnError { track.inLibrary = false }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun addToLibrary(context: Context, album: SkyjamAlbum): Completable {
        album.inLibrary = true
        return Protocol.mutateEntity(context, album, MutateOperation.CREATE)
                .flatMapCompletable { entity -> Completable.fromCallable {
                    Realm.getDefaultInstance().executeTransaction { realm ->
                        (entity as SkyjamAlbum).tracks?.forEach {
                            addOneToDatabase(realm, it, true, true)
                        }
                    }
                } }
                .doOnError { album.inLibrary = false }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun removeFromLibrary(context: Context, album: IAlbum): Completable {

        val albumId = album.albumId

        return Protocol.mutateEntity(context, album, MutateOperation.DELETE)
                .flatMapCompletable { _ -> Completable.fromCallable {
                    Realm.getDefaultInstance().executeTransaction { realm ->
                        getDbAlbumTracks(albumId, realm).forEach {
                            it.inLibrary = false
                        }
                        getAlbum(albumId, realm)?.apply {
                            inLibrary = false
                            artists?.forEach { determineArtistShouldBeInLibrary(it) }
                        }
                    }
                } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    private fun determineArtistShouldBeInLibrary(artist: Artist) {
        artist.inLibrary = artist.albums.any { it.inLibrary }
    }

    fun removeFromLibrary(context: Context, track: ITrack): Completable {

        track.inLibrary = false
        return Protocol.mutateEntity(context, track, MutateOperation.DELETE)
                .andThen(Completable.fromCallable {
                    Realm.getDefaultInstance().executeTransaction { realm ->
                        get(track, realm)?.apply {
                            inLibrary = false
                            albums?.firstOrNull()?.let {
                                if(it.tracks.all { !it.inLibrary  }) {
                                    it.inLibrary = false
                                    it.artists?.forEach { determineArtistShouldBeInLibrary(it) }
                                }
                            }
                        }

                    }
                })
                .doOnError { track.inLibrary = true }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    @UiThread
    fun loadArtists(): Flowable<RealmResults<Artist>> {
        return Realm.getDefaultInstance().where(Artist::class.java)
                .equalTo("inLibrary", true)
                .sort(Artist.NAME, Sort.ASCENDING)
                .findAllAsync()
                .asFlowable()
    }

    operator fun get(track: ITrack, realm: Realm? = null): Track? {
        if(track is Track) return track
        var trackQ = (realm ?: Realm.getDefaultInstance()).where(Track::class.java)
        var nmo = false
        track.id?.let { trackQ = trackQ.equalTo(Track.TRACK_ID, it); nmo = true }
        track.clientId?.let {
            trackQ = if (nmo) trackQ.or().equalTo(Track.CLIENT_ID, it)
            else {
                nmo = true
                trackQ.equalTo(Track.CLIENT_ID, it)
            }
        }
        track.storeId?.let {
            trackQ = if (nmo) trackQ.or().equalTo(Track.STORE_ID, it)
            else trackQ.equalTo(Track.STORE_ID, it)
        }

        return trackQ.findFirst()
    }

    fun has(track: ITrack): Boolean {
        return get(track)?.let { if(!it.inLibrary) null else it } != null
    }

    fun getAllAlbumTracks(album: IAlbum): List<ITrack> {
        return if (album is Album){
            if(album.kind == Album.SAMPLE) loadSampleTracks(album.albumArtist, album.name)
            else album.tracks.sort(Track.TRACK_NUMBER)
        }

        else (album as SkyjamAlbum).tracks?.sortedBy {
            it.trackNumber
        } ?: listOf()
    }

    fun get(album: IAlbum, realm: Realm? = null): Album? {
        return getAlbum(album.albumId, realm)

    }

    fun getAlbum(albumId: String, realm: Realm? = null): Album? {
        return (realm ?: Realm.getDefaultInstance())
                .where(Album::class.java)
                .equalTo(Album.ID, albumId)
                .findFirst()
    }

    infix fun has(album: IAlbum) = get(album)?.let { if(!it.inLibrary) null else it } != null

    private fun getDbAlbumTracks(album: IAlbum, realm: Realm = Realm.getDefaultInstance()): List<Track> {
        return get(album, realm)?.tracks?.mapNotNull { get(it, realm) } ?: listOf()
    }

    private fun getDbAlbumTracks(albumId: String, realm: Realm = Realm.getDefaultInstance()): List<Track> {
        return getAlbum(albumId, realm)?.tracks?.mapNotNull { get(it, realm) } ?: listOf()
    }

    fun getTrack(id: String): Track? {
        return Realm.getDefaultInstance().where(Track::class.java)
                .equalTo(Track.LOCAL_ID, id)
                .findFirst()
    }


}
