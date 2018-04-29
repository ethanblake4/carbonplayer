package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Attribution
import com.carbonplayer.model.entity.base.IAlbum

/**
 * 1-to-1 mapping of JSON album from sj API
 */
data class SkyjamAlbum (

        override val kind: String /* should be "sj#album" */,
        override val inLibrary: Boolean?,
        override val albumId: String = MusicLibrary.UNKNOWN_ALBUM_ID,
        override val recentTimestamp: Long?,
        override val name: String = "",
        override val albumArtist: String,
        override val albumArtRef: String = "",
        val artist: String,
        override val artistId: List<String>,
        override val composer: String?,
        override val year: Int = 0,
        override val genre: String = "",
        val tracks: List<SkyjamTrack>?,
        override val description: String?,
        override val description_attribution: Attribution?,
        override val explicitType: String?,
        override val contentType: String?

) : IAlbum