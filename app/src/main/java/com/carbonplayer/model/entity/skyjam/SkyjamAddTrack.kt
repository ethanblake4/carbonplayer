package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Image

data class SkyjamAddTrack (

        /* Track IDs are... a clusterf*ck (why is there FOUR?) */
        val id: String?,
        val clientId: String?,
        val nid: String?,
        val storeId: String?,

        val title: String,

        /* Relic of a simpler time, when most tracks didn't have (ft.) at the end */
        val artist: String,

        val album: String,
        val albumId: String = MusicLibrary.UNKNOWN_ALBUM_ID,
        val albumArtist: String,
        val artistArtRef: List<Image>?,

        val trackNumber: Int,
        val totalTrackCount: Int?,
        val discNumber: Int,
        val totalDiscCount: Int?,
        val estimatedSize: Int?,
        val trackType: Int?,
        val durationMillis: Int,
        val trackAvailableForSubscription: Boolean?,

        val rating: String? = null,
        val lastRatingChangeTimestamp: Long?,

        /**
         * @see com.carbonplayer.model.entity.enums.ExplicitType
         */
        val explicitType: Int?,

        val creationTimestamp: Long?,
        val recentTimestamp: Long?,
        val isDeleted: Boolean = false,
        val comment: String?,

        val composer: String?,
        val genre: String?,
        val year: Int = 0,
        val beatsPerMinute: Int?,

        val playCount: Int?
) {
    constructor(track: SkyjamTrack) : this (
            track.id, track.clientId, track.nid, track.storeId,
            track.title, track.artist, track.album, track.albumId,
            track.albumArtist, track.artistArtRef,
            track.trackNumber, track.totalTrackCount, track.discNumber,
            0, track.estimatedSize, 8,
            track.durationMillis, track.trackAvailableForSubscription,
            "0", track.lastRatingChangeTimestamp, track.explicitType,
            -1, track.recentTimestamp, false, track.comment, "", "",
            track.year, -1, 0
    )
}