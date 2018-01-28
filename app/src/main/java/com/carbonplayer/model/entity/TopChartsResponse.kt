package com.carbonplayer.model.entity

import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamTrack

data class TopChartsResponse (
        val image: Image,
        val albums: List<SkyjamAlbum>,
        val tracks: List<SkyjamTrack>
)