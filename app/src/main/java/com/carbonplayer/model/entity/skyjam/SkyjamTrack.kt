package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Image
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.base.ITrack
import io.realm.Realm

/**
 * 1-to-1 mapping of JSON track from sj API
 *
 * SkyjamTracks not in your library are converted to Tracks
 * and stored when they are added to the queue
 */
data class SkyjamTrack(

        val kind: String, /* Should be "sj#track" */
        override val audioAd: Boolean?, /* If it is an ad, we will skip it <evil laughter> */
        override var inLibrary: Boolean = false,

        /* Track IDs are... a clusterf*ck (why is there FOUR?) */
        override val id: String?,
        override val clientId: String?,
        override val nid: String?,
        override val storeId: String?,

        override val title: String,

        /* Relic of a simpler time, when most tracks didn't have (ft.) at the end */
        override val artist: String,
        val artistId: List<String>?, // Ha psych!

        override val album: String,
        override val albumId: String = MusicLibrary.UNKNOWN_ALBUM_ID,
        override val albumArtist: String,
        val albumArtRef: List<Image>?,
        val artistArtRef: List<Image>?,

        override val trackNumber: Int,
        override val totalTrackCount: Int?,
        override val discNumber: Int,
        override val totalDiscCount: Int?,
        override val estimatedSize: Int?,
        override val trackType: Int?,
        override val durationMillis: Int,

        override val trackAvailableForPurchase: Boolean?,
        val albumAvailableForPurchase: Boolean?,
        override val trackAvailableForSubscription: Boolean?,

        override val rating: String? = null,
        override val lastRatingChangeTimestamp: Long?,

        /**
         * @see com.carbonplayer.model.entity.enums.ExplicitType
         */
        override val explicitType: Int?,

        override val creationTimestamp: Long?,
        override val recentTimestamp: Long?,
        override val isDeleted: Boolean = false,
        override val comment: String?,

        override val composer: String?,
        override val genre: String?,
        override val year: Int = 0,
        override val beatsPerMinute: Int?,

        override val playCount: Int?
) : ITrack {

    override val albumArtURL: String?
        get() = albumArtRef?.first()?.url

    fun remoteParcelable() = ParcelableTrack(this)

    override fun syncable() = this

    fun forAdd() = SkyjamAddTrack(this)

    constructor(local: Track, trackType: String?) : this (
            "sj#track", local.audioAd, local.inLibrary, local.id,
            local.clientId, local.nid, local.storeId, local.title, local.artist,
            local.artists?.map { it.artistId }, local.album, local.albumId,
            local.albumArtist, null, null, local.trackNumber, local.totalTrackCount,
            local.discNumber, local.totalDiscCount, local.estimatedSize, local.trackType,
            local.durationMillis, local.trackAvailableForPurchase,
            null, local.trackAvailableForSubscription, local.rating,
            local.lastRatingChangeTimestamp, local.explicitType,
            local.creationTimestamp, local.recentTimestamp, local.isDeleted,
            local.comment, local.composer, local.genre, local.year, local.beatsPerMinute,
            local.playCount
    )


    override fun parcelable(realm: Realm?) : ParcelableTrack {

        var t: ParcelableTrack? = null

        if(realm != null) { // Already in a transaction
            t = MusicLibrary.addOneToDatabase(realm, this, true, false).parcelable()
        } else Realm.getDefaultInstance().executeTransaction { rlm ->
            t = MusicLibrary.addOneToDatabase(rlm, this, true, false).parcelable()
        }

        return t!!
    }

    companion object {
        fun sample(name: String, pos: Int, artist: String, album: String) = SkyjamTrack (
                "sj#track", false, false, "999-999-999",
                "888-888-888", "000-000-000", "000-000-000", name,
                artist, listOf("000-000-000"), album, MusicLibrary.UNKNOWN_ALBUM_ID,
                artist, listOf(), listOf(), pos, 10, 1, 1,
                420120, -1, 0, null,
                false, false, null,
                null, null, null, null,
                false, null, null, null, 2000,
                null, 0
        )
    }

}