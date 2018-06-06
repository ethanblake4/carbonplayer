package com.carbonplayer.model.entity


import android.annotation.SuppressLint
import android.os.Parcelable
import android.support.v4.media.MediaMetadataCompat
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import io.realm.Realm
import kotlinx.android.parcel.Parcelize

/**
 * Simplified Track object for passing to/from background service
 */
@SuppressLint("ParcelCreator")
@Parcelize
class ParcelableTrack (

        var localId: Long? = null,
        var position: Int = 0,
        override var id: String? = null,
        override var clientId: String? = null,
        override var nid: String? = null,
        override var storeId: String? = null,
        override var title: String,
        override var artist: String,
        val artistId: List<String>?,
        val artistArtRef:List<String>?,
        override var album: String,
        override var year: Int = 0,
        override var trackNumber: Int = 0,
        override var genre: String? = null,
        override var durationMillis: Int = 0,
        override var albumArtURL: String?,
        override var rating: String? = null,
        override val albumArtist: String,
        override val albumId: String,
        override val audioAd: Boolean?,
        override val beatsPerMinute: Int?,
        override val comment: String?,
        override val composer: String?,
        override val creationTimestamp: Long?,
        override val discNumber: Int,
        override val explicitType: Int?,
        override var inLibrary: Boolean,
        override val isDeleted: Boolean,
        override val lastRatingChangeTimestamp: Long?,
        override val playCount: Int?,
        override val recentTimestamp: Long?,
        override val totalDiscCount: Int?,
        override val totalTrackCount: Int?,
        override var estimatedSize: Int = 0,
        override val trackAvailableForPurchase: Boolean?,
        override val trackAvailableForSubscription: Boolean?,
        override val trackType: Int?,
        var queuePosition: Int = 0

) : ITrack, Parcelable {

    override fun parcelable(realm: Realm?) = this

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

    constructor(source: Track) : this (
        source.localId, 0, source.id, source.clientId, source.nid, source.storeId,
            source.title, source.artist, source.artists?.map {it.artistId},
            source.artists?.firstOrNull()?.artistArtRef?.let { listOf(it) },
            source.album, source.year, source.trackNumber,
            source.genre, source.durationMillis, source.albums?.firstOrNull()?.albumArtRef,
            source.rating, source.albumArtist, source.albumId, source.audioAd,
            source.beatsPerMinute, source.comment, source.composer, source.creationTimestamp,
            source.discNumber, source.explicitType, source.inLibrary, source.isDeleted,
            source.lastRatingChangeTimestamp, source.playCount, source.recentTimestamp,
            source.totalDiscCount, source.totalTrackCount, source.estimatedSize ?: -1,
            source.trackAvailableForPurchase, source.trackAvailableForSubscription, source.trackType
    )

    constructor(source: SkyjamTrack) : this (
            null, 0, source.id, source.clientId, source.nid, source.storeId,
            source.title, source.artist, source.artistId, source.artistArtRef?.map {it.url},
            source.album, source.year, source.trackNumber,
            source.genre, source.durationMillis, source.albumArtRef?.firstOrNull()?.url,
            source.rating, source.albumArtist, source.albumId, source.audioAd,
            source.beatsPerMinute, source.comment, source.composer, source.creationTimestamp,
            source.discNumber, source.explicitType, source.inLibrary, source.isDeleted,
            source.lastRatingChangeTimestamp, source.playCount, source.recentTimestamp,
            source.totalDiscCount, source.totalTrackCount, source.estimatedSize ?: -1,
            source.trackAvailableForPurchase, source.trackAvailableForSubscription, source.trackType
    )

    override fun syncable(): SkyjamTrack {
        TODO("not implemented")
    }

}