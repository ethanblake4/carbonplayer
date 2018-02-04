package com.carbonplayer.model.entity.skyjam

data class TopChartsGenres (
        val genres: List<Genre>
) {
    data class Genre (
            val id: String,
            val title: String
    )
}