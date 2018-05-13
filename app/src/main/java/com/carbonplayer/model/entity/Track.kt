package com.carbonplayer.model.entity

import android.support.v4.media.MediaMetadataCompat
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.PlaySource
import com.carbonplayer.model.entity.enums.StorageType
import com.carbonplayer.model.entity.enums.StreamQuality
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.utils.nullIfEmpty
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

/**
 * Playable track object
 */
open class Track(

        @PrimaryKey var localId: Long = 0,
        override var inLibrary: Boolean = false,
        @Index override var id: String? = null,
        override var clientId: String? = null,
        override var nid: String? = null,
        @Index override var storeId: String? = null,
        override var audioAd: Boolean? = null,
        override var recentTimestamp: Long? = null,
        override var isDeleted: Boolean = false,
        override var title: String = "",
        override var composer: String? = null,
        @LinkingObjects(Album.TRACKS) val albums: RealmResults<Album>? = null,
        @LinkingObjects("artistTracks") val artists: RealmResults<Artist>? = null,
        override var artist: String = "",
        override var year: Int = 0,
        override var comment: String? = null,
        override var trackNumber: Int = 0,
        override var genre: String? = null,
        override var durationMillis: Int = 0,
        override var beatsPerMinute: Int? = null,
        override var discNumber: Int = 0,
        override var explicitType: Int? = null,
        override var creationTimestamp: Long? = 0,
        override var trackAvailableForPurchase: Boolean? = null,
        override var trackAvailableForSubscription: Boolean? = null,
        override var totalDiscCount: Int? = null,
        override var totalTrackCount: Int? = null,
        override var trackType: String? = null,
        override var lastRatingChangeTimestamp: Long? = null,
        override var playCount: Int = 0,
        override var rating: String? = null,
        override var estimatedSize: Int? = null,
        private var localPlays: RealmList<Long> = RealmList(),
        var localTrackSizeBytes: Long = 0,
        var hasCachedFile: Boolean = false,
        private var cachedFileQuality: Int = 0,
        private var storageType: Int = 0

) : RealmObject(), ITrack {

    val calculatedArtistName: String
        get() = artists?.map { it.name }?.nullIfEmpty()?.run { reduceIndexed {
            i, acc, n -> if(i==this.size) "$acc & $n" else "$acc, $n"
        } } ?: "Error"

    override val album: String
        get() = albums?.first(null)?.name ?: ""

    override val albumId: String
        get() = albums?.first(null)?.albumId ?: ""

    override val albumArtist: String
        get() = albums?.first(null)?.albumArtist ?: ""

    override val albumArtURL: String?
        get() = albums?.first(null)?.albumArtRef

    val mediaMetadata: MediaMetadataCompat
        get() = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMillis.toLong())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArtURL)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber.toLong())
                .build()


    constructor(localId: Long, source: SkyjamTrack) : this (
            localId,
            source.inLibrary,
            source.id,
            source.clientId,
            source.nid,
            source.storeId,
            source.audioAd,
            source.recentTimestamp,
            source.isDeleted,
            source.title,
            source.composer,
            null,
            null,
            source.artist,
            source.year,
            source.comment,
            source.trackNumber,
            source.genre,
            source.durationMillis,
            source.beatsPerMinute,
            source.discNumber,
            source.explicitType,
            source.creationTimestamp,
            source.trackAvailableForPurchase,
            source.trackAvailableForSubscription,
            source.totalDiscCount,
            source.totalTrackCount,
            source.trackType,
            source.lastRatingChangeTimestamp,
            source.playCount ?: 0,
            source.rating,
            source.estimatedSize
    )

    constructor(localId: Long, source: ParcelableTrack) : this (
            localId,
            source.inLibrary,
            source.id,
            source.clientId,
            source.nid,
            source.storeId,
            source.audioAd,
            source.recentTimestamp,
            source.isDeleted,
            source.title,
            source.composer,
            null,
            null,
            source.artist,
            source.year,
            source.comment,
            source.trackNumber,
            source.genre,
            source.durationMillis,
            source.beatsPerMinute,
            source.discNumber,
            source.explicitType,
            source.creationTimestamp,
            source.trackAvailableForPurchase,
            source.trackAvailableForSubscription,
            source.totalDiscCount,
            source.totalTrackCount,
            source.trackType,
            source.lastRatingChangeTimestamp,
            source.playCount ?: 0,
            source.rating,
            source.estimatedSize
    )

    fun updateWith(source: ITrack) : Track {
        this.inLibrary = source.inLibrary
        this.id = source.id ?: this.id
        this.trackNumber = source.trackNumber
        this.artist = source.artist
        this.clientId = source.clientId ?: this.clientId
        this.nid = source.nid ?: this.nid
        this.beatsPerMinute = source.beatsPerMinute ?: this.beatsPerMinute
        this.durationMillis = source.durationMillis
        this.estimatedSize = source.estimatedSize ?: this.estimatedSize
        this.playCount = source.playCount ?: this.playCount
        this.comment = source.comment ?: this.comment
        this.composer = source.composer ?: this.composer
        this.rating = source.rating ?: this.rating
        this.year = source.year
        this.isDeleted = source.isDeleted
        this.title = source.title
        this.genre = source.genre ?: this.genre
        return this
    }

    fun addPlay() {
        localPlays.add(System.currentTimeMillis())
        playCount++
    }

    fun getCachedFileQuality(): StreamQuality {
        return StreamQuality.values()[cachedFileQuality]
    }

    fun setCachedFileQuality(cachedFileQuality: StreamQuality) {
        this.cachedFileQuality = cachedFileQuality.ordinal
    }

    fun getStorageType(): StorageType {
        return StorageType.values()[storageType]
    }

    fun setStorageType(storageType: StorageType) {
        this.storageType = storageType.ordinal
    }


    fun getCacheImportance(source: PlaySource): Long {

        var cacheImportance = maxOf(0, Math.round(Math.log(playCount.toDouble()) * 8.0))

        localPlays.forEach {
            cacheImportance += maxOf(0,
                    Math.round(10 - Math.pow(((System.currentTimeMillis() - it) / 864000000L).toDouble(), 2.0)))
        }

        cacheImportance += when (source) {
            PlaySource.RECENTS -> 20
            PlaySource.ALBUM, PlaySource.PLAYLIST -> 18
            PlaySource.ARTIST -> 16
            PlaySource.SONGS, PlaySource.EXTERNAL -> 10
            PlaySource.RADIO -> 0
        }

        return cacheImportance
    }

    override fun parcelable(realm: Realm?): ParcelableTrack {
        return ParcelableTrack(this)
    }

    override fun toString() = "Track { id: $id, name: $title, album: $album, trackNumber: $trackNumber, " +
                "albumId: ${albums?.first()?.albumId ?: ""}"

    companion object {

        const val LOCAL_ID = "localId"
        const val TRACK_ID = "id"
        const val CLIENT_ID = "clientId"
        const val NAUTILUS_ID = "nid"
        const val STORE_ID = "storeId"
        const val HAS_CACHED_FILE = "hasCachedFile"
        const val CACHED_FILE_QUALITY = "cachedFileQuality"
        const val STORAGE_TYPE = "storageType"
        const val TRACK_NUMBER = "trackNumber"

    }

}
