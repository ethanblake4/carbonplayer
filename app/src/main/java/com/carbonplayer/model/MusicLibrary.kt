package com.carbonplayer.model

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.support.annotation.UiThread
import android.util.Pair
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.*
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.IPlaylist
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
                        if (e.name == "isNautilusUser"
                                && !CarbonPlayerApplication.instance.preferences.isCarbonTester)
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
            managedArtists.add(if (!it.isManaged) realm.copyToRealm(it) else it)
        }

        return track
    }

    fun addOneToDatabase(realm: Realm, pTrack: ParcelableTrack, update: Boolean = true,
                         inLibrary: Boolean = true): Track {
        val artistPairs = mutableMapOf<String, String>()
        var treatAsOneArtist = false

        if (pTrack.artistId != null) {

            val featuring = extractFeaturing(pTrack)

            val _artists = pTrack.artist.split(", ").flatMap { it.split(" & ") }

            val combinedArtists = mutableListOf<String>().apply {
                addAll(_artists)
                addAll(featuring)
            }

            // For most cases
            if (combinedArtists.size == pTrack.artistId.size) {
                (0 until combinedArtists.size).forEach {
                    artistPairs[pTrack.artistId[it]] = combinedArtists[it]
                }
            } else if (combinedArtists.size > pTrack.artistId.size) {
                // Fixes e.g. "Tom Petty & The Heartbreakers"
                val noTheSize = combinedArtists.size -
                        combinedArtists.count { it.startsWith("the", true) } +
                        if (combinedArtists[0].startsWith("The")) 1 else 0
                if (noTheSize == pTrack.artistId.size) {
                    (0 until noTheSize).forEach {
                        if (it == 0 || !_artists[it].startsWith("the", true)) {
                            artistPairs[pTrack.artistId[it]] = _artists[it]
                        } else {
                            val carp = artistPairs[pTrack.artistId[it-1]]
                            artistPairs[pTrack.artistId[it-1]] = carp + " & " + combinedArtists[it]
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
                    .equalTo(Artist.ID, pTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID)
                    .findFirst() ?:
            Artist(pTrack.artistId?.first() ?: UNKNOWN_ARTIST_ID, pTrack.artist, inLibrary))
        } else {
            var fst = true
            artistPairs.forEach { (id, name) ->
                artists.add(realm.where(Artist::class.java)
                        .equalTo(Artist.ID, id)
                        .findFirst() ?: Artist(id, name, pTrack.artistArtRef?.map { Image(
                        kind = "sj#image",
                        url = it)}, inLibrary && fst)
                )
                fst = false
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
            if(!it.artistTracks.contains(track)) {
                it.artistTracks.add(track)
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
                        insertOrUpdateTrack(realm, it)
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



    fun loadSampleAlbums(): List<Album> {
        return listOf(
                Album(Album.SAMPLE, true, "", null,
                        "2753", "Blake",
                        "",2018, "Electronic",
                        "https://lh3.googleusercontent.com/FL9C8t0fI0w8OcFR6oowOnjWgGMeJdrrnFYHm3745kpOPzo8HmT78XHmQIS3hUuWwSDNm1y" +
                                "YZNUyj8MepwGujGKy8pqKGRtuSd3Hj0rvYX52UbEx7vtzDDtvtUJiV9Z67y1CWj5NnRuiKVNx-Pec0JBy2peFtQgjAuOF-_i44EpvFnX7UfSg2tp_T2zDemhP2NQkz4b6_VuddP" +
                                "fsD_Ad8uiWb6PsvMFzfTR3T0N7jE-Nt2hp1HVLxD0lBfLwc8VOZg5ALdA_QdVZaPIXgGU8MgM5r0ABvLUTSbSyIhf-8xlibmWSd34R7RefFswt4_btqK5Mw_XfXDuOozuMDQbtGDidCQl41" +
                                "KUAHoKNPwfb1NycAAIBo8eDU7rY7maCI0c7eIgrfjUpdh2qwPVzaQMTU6k1XdxRLrvLXSYdbCbLQxyRfFgALJ_-zXgib157bA1LfUo_jkny_t7pUGsnmZEKUjpaaSEAIgDlqfBSe92RUPrlze" +
                                "JhQJsMPqQv6UiUz8TxyO3nqqaJNkDYDGUgpKZK7wDlTvwuxnfOTL0tA24Px-tvzXrmq0yvnCkhc38rIjpJFDFPIhfNcA_0-Yi0QmKtvQcMDtwS-kyCddi5js8X=s1732-no"
                ),
                Album(Album.SAMPLE, true, "", null,
                        "Archea", "Defenders of Saturn",
                        "", 2005, "Hip-Hop",
                        "https://lh3.googleusercontent.com/4aZHUwrcn9FVhPZ-RuS1za4QamOngIY3ndPgcGTYvA2WO1mM3WcfVLJ_Lv1Mm97KXkrJjTuNkHuhlaGRqmMH_ouTWxXfnp2KOWxAMKzl08hRH0lt8_hKuGxTdW4dRK" +
                                "XmDH68aWlNQo2Sj6K9SUFrFSpyFMjGmB1V-BowIQeLaYu_nTTe7G1S1PNAlH6TyUTEBDBZl5cRT0J_FVXgtNnHANyvMtfo_RKRz2eMpbQCLXyVz8DhCZHf2FHv9A4vJybgE_pd6B_JP4joBb_WJGwDkeefHc5fSFI" +
                                "3nc4EucYo5rNgincDJm1T3kHRWzVcq165f4fMYHlmiM4s118fNJ9vA5m2uPmzYWC64tMEkARjRgbOO-8t5xDUOmbUcIWPgPDZm0ZKs7j-gvx8Kkju5Y1fW_FpahIfiE3gGpC2aZoJbghZX2N7uLvB0bVE7zTrX_" +
                                "XUhHh6S9BdFJ-8iLnXqxjb3dwVjmqDDzpXwbCBYJjxGmYiD-rdRq9S2X3yYOSAPiEQvX1HEHxfviqJzmuknlgKZ-dQwbCHtWvO8yl3UIH1AXna2E9AH37aEKDNS3x-RmNynyzV32tOpKnxRyM7fuWbHJD_FqrH3gt_lU_R_Qpg=s804-no"),
                Album(Album.SAMPLE, true, "", null,
                        "In Mediums", "Bellevue",
                        "",2007, "Indie",
                        "https://lh3.googleusercontent.com/bt2niRoK0GOoKUTsB7KBDVAyxpX-du7udK8ZYegBw_wBGO71W11eJCslSi8_TzK4eSh-DmkqENCQUyCMgNmXft2006FoyqHWkZP71Ev5heXtFa283skPM_" +
                                "yUH1CV_KVPizwiYW2FgYtpwyZkhVuAhwrAH5yaviE8QNg3WUhQuIY5H-w0P_qB-YNlrEJRZWdaOK6Dbmdr1qhxSFBkPBHVI_2XL80iCmNeXVbKgSPh8VxoH_gbcQO_bXbCQNdwfi_LVSJ9wVxbG52bNjMyWpDD" +
                                "iPXxasKWb9S5WXl6n5o-p3cshAl5fNre_-BCY_0YJwV2HJ1kUow5l0iJZ2gBXb6Q9eafWIUrt22BPhZ7D45Wg3y63Dr2d48NWcQv_DK6BciN60uwAnjS27g5yM0RNHkxARgIiMTP0SILBGujFxFIAZSj-dwJDFNhm" +
                                "UU8YeWEDorOcKNaovV1EUqSlNTJhOsyIg70tpg4ZCMVVTpk0XMKZAqBwSSYUU3mvrJFQZaSAeRMnruNhz6zTnIJ1fnb0vXRqvxE4MxBL_5EaJyfTe713j3PacG-4GO7rIdIJFz1NfSdNUb3Za0iJse0cH2lrhswjDaY" +
                                "YhubEmj2yoKtvJi0=w1080-h1079-no"),
                Album(Album.SAMPLE, true, "", null,
                        "Today", "Point Four",
                        "",2011, "Pop",
                        "https://lh3.googleusercontent.com/pEVVJckVeTWazrz-HQTvK5J0VVpVgOVUEdg8w8FLjAWNkp2fqkuAkSFzqY02rreXjsBeIb0Eq5l3_lkZJcOjDoMTpevCkceMMOSZKhQQU-tJpkZ8dJ2ew" +
                                "ECdN6P79BtUISUNNa79yTOGwuhCu4fiihz5zLd4iCQDD0EC2RwsnkKenFpt5oOecJl4F92mVu-y5JlFCeRrJNuZqKCf0Y44RU5tq8mmej5WMdDXynH0n244gL_VM-oRp-IykPpsfjFnTt1I7QZUCrARwbfRc" +
                                "XTjBHp_9kGf-2As9t3Y8t7tpeo9NiLUdI3t_fkfFZJw0CBF2yQd-GWhKVPlN6K8st3RhbRT7dpws1eRqQkQCl9NZTgCkFjkMTEa2IW65JaATI2-3bBg6_M_0nuK_iSQkjifk8T8HxoQDDK3NtWkJs17UydLPE8" +
                                "RwNcWGXYOGVBwMo9FEc8T6e6xFQ3Gpq8hOOlbeSnOU3cVj7_dp1ugrlvPR5hz73smi-3-Kb6-RY0sKMcx9e6Medy7jKSR4JdKK3TsMX76wS6312zEXrBBtTfjOXe312_QD4-lJZFTW1BWbiu-C_p-GJaSFMXu" +
                                "YEHPVVrwbuCKxY79ugwJyr-xk00t=s1062-no"),
                Album(Album.SAMPLE, true, "", null,
                        "Under Duress", "Indigo",
                        "",2013, "Rock",
                        "https://lh3.googleusercontent.com/okraJMPVXyVCHnjKaH2QfTSniWtP0ty4piP9IO5XdrVndBj-NAUJd-ov1wC4GRvrHwSpLQWPmUnWXs70geYEOzbo7EKLuFyeIooX5DenAGGOUpa4-ZBkz" +
                                "92TMWTNAz38_u1fl7Ci51SbOG1i1YcFXaJhB9qjGnxvzFiv-OqotBxs9C-sLIgsoKVsC31JLu8IViO97yyRzBn5D5c46HGBekSj6sQZejD08McjIrQAU-o-9dFKs-F8kSO5i4U94DU_CrpZWtPknNb4MRi9G" +
                                "BtYDJt3Fr8DPUIn4KOAQ5zmh673CZPmB9YtM4T-y1c8rt7nlY5IwMhHlvn2p2QsR2c1C3y3Oq9HYpdIGmjz9TaWzLJOT7sjX8ceOYTLf-Y4YZDx3mwvDyHTs_SMuOGohEFavySp8YNef1X5l7YQmWuOXXz7j" +
                                "SW-MyWgu7YTvKzzjzzp2os-y6KJOJaBU0FsuwSiG9Hpw7n0EcDrjh6PgYDY0nKGE--6uqX7rT162myGPSDGvcUEs4WTT2HG0BQjecx1ljotp63clNTH8d3M4X3pHiVLKcINs-uxn3hMC4iUXxSqfJP8RBb3ewoaf" +
                                "E7d-Nt_SQd9ap3e8VMQq7pz3zjw=w245-h244-no"),
                Album(Album.SAMPLE, true, "", null,
                        "Travelers", "Caracal",
                        "",2013, "Rock",
                        "https://lh3.googleusercontent.com/9NDte0EsMo0vciJzo39lhrG_HqKwfHXjiMXaL1t391yc1J3NRzlXEdnysJCoY_oG1dbrEaQaRV-s5TxMkhc313oNbYi9LiqJFHTgup0PPvsdx" +
                                "oZTZuh0MIqq7gBP7z2CoG3GGSIuO_GmcuV-5cGIgShmPNOUrT-g9oOdHmTr1rozI4TKMqRebe7lmJGViq2MrJfuEnftu6965CpJTfIWVmV0qWttQWubd-6Ihp7TajwOG--fmNfuH0o9kAdOJ" +
                                "JWit_TjEr5F-vVaboA_Bf_NgoScQ8biXi3iwoH9O95G9a7JuUcmkhngRinxuVKYey9ygkLuMthJ3Ati4RrbUa-lP-ShP6ereCFTlYICigzZCsSTpqLJBfFj4Pq-HM5lHcg-sQMTtjrNSWQjTEJ" +
                                "5fJydA2E9y80rKccCljg-Ko7uyYHhGxwsjkNM6uitZGEKVcVS2HRMaQiu8fMwmqac7xicIaQMyj4L_AofoIxKZ52LRRuRudAPG3Xw-DaJpdQFpWb_9EDUGkGAORSZ6i5HR_60ZGwfbJV68eakZ" +
                                "DMekaM0wmlP0QLrKD_HvJr11KMcWqz9fWS0HOQgYI1xPI79sXKr67g5JeRAoBhEFdWDr1PF=w1440-h1439-no")
        )
    }

    fun loadSampleTracks(artist:String = "Bellevue", album:String = "In Mediums"): List<SkyjamTrack> {
        return listOf(
                SkyjamTrack.sample("Intro", 1, artist, album),
                SkyjamTrack.sample("Fire Eyes", 2, artist, album),
                SkyjamTrack.sample("Remedial", 1, artist, album),
                SkyjamTrack.sample("Tapestry", 1, artist, album),
                SkyjamTrack.sample("Saltwater (Interlude)", 1, artist, album),
                SkyjamTrack.sample("Virus", 1, artist, album),
                SkyjamTrack.sample("Devil", 1, artist, album)
        )
    }

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

    @UiThread
    fun loadArtists(): Flowable<RealmResults<Artist>> {
        return Realm.getDefaultInstance().where(Artist::class.java)
                .equalTo("inLibrary", true)
                .sort(Artist.NAME, Sort.ASCENDING)
                .findAllAsync()
                .asFlowable()
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

    fun getTrack(id: String): Track? {
        return Realm.getDefaultInstance().where(Track::class.java)
                .equalTo(Track.LOCAL_ID, id)
                .findFirst()
    }


}
