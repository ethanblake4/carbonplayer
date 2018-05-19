package com.carbonplayer.model.entity.api


data class ExploreEntityGroup (
        val title: String,
        val description: String?,
        val entities: List<ExploreEntity>
)