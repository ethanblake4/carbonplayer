package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.entity.Image

data class TopChartsResponse (
        val header: TopChartsHeader,
        val chart: TopChartsChart
) {
    data class TopChartsHeader (
            val header_image: Image
    )

    data class TopChartsChart (
            val albums: List<SkyjamAlbum>,
            val tracks: List<SkyjamTrack>
    )
}