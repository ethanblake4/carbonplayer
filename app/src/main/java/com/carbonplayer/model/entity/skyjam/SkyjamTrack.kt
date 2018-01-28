package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Image
import com.carbonplayer.model.entity.ParcelableTrack
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

        override val trackNumber: Int,
        override val totalTrackCount: Int?,
        override val discNumber: Int,
        override val totalDiscCount: Int?,
        override val estimatedSize: Int?,
        override val trackType: String?,
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

    override fun parcelable() : ParcelableTrack {
        return MusicLibrary.insertOrUpdateTrack(Realm.getDefaultInstance(), this).parcelable()
    }

}