package com.carbonplayer.model.entity.api

data class TopChartsGenres (
        val genres: List<Genre>
) {
    data class Genre (
            val id: String,
            val title: String
    )
}