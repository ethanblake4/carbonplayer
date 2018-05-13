package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.entity.Attribution
import com.carbonplayer.model.entity.Image
import com.carbonplayer.model.entity.base.IArtist

data class SkyjamArtist(

        override var artistId: String = "",
        override var kind: String = "",
        override var name: String = "",
        override var artistArtRef: String? = null,
        var artistArtRefs: MutableList<Image> = mutableListOf(),
        override var artistBio: String? = null,
        var albums: MutableList<SkyjamAlbum> = mutableListOf(),
        var topTracks: MutableList<SkyjamTrack> = mutableListOf(),
        var total_albums: Int = -1,
        var artist_bio_attribution: Attribution? = null,
        var related_artists: MutableList<SkyjamArtist> = mutableListOf()

) : IArtist {
    override val bestArtistArtUrl: String?
        get() = artistArtRef ?: artistArtRefs.firstOrNull()?.url
}
