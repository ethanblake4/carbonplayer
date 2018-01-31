package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.entity.Attribution
import com.carbonplayer.model.entity.Image

data class SkyjamArtist(

        var artistId: String = "",
        var kind: String = "",
        var name: String = "",
        var artistArtRef: String? = null,
        var artistArtRefs: MutableList<Image> = mutableListOf(),
        var artistBio: String? = null,
        var albums: MutableList<SkyjamAlbum> = mutableListOf(),
        var topTracks: MutableList<SkyjamTrack> = mutableListOf(),
        var total_albums: Int = -1,
        var artist_bio_attribution: Attribution? = null,
        var related_artists: MutableList<SkyjamArtist> = mutableListOf()

)
