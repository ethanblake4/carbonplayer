package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Attribution
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.proto.identifiers.PlayableItemIdV1Proto

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
        override val composer: String? = null,
        override val year: Int = 0,
        override val genre: String = "",
        val tracks: List<SkyjamTrack>? = null,
        override val description: String? = null,
        override val description_attribution: Attribution? = null,
        override val explicitType: String? = null,
        override val contentType: String? = null,
        val source: Int = 0

) : IAlbum {
    constructor(source: SkyjamTrack) : this(
            "sj#album", source.inLibrary,
            source.albumId, source.recentTimestamp,
            if(source.album.isBlank()) "Unknown Album" else source.album,
            source.albumArtist, source.albumArtRef?.first()?.url ?: "",
            source.albumArtist, source.artistId ?: listOf(),
            source.composer ?: "", source.year, source.genre ?: "",
            null, null, null, source.explicitType?.toString(), null, 1)

    constructor(protoId: PlayableItemIdV1Proto.PlayableItemId,
                title: String, artist: String, artUrl: String) : this (
            "sj#album", null, protoId.audioList.albumRelease.catalog.metajamCompactKey,
                    null, title, artist, artUrl, artist, listOf(), source = 2
    )
}