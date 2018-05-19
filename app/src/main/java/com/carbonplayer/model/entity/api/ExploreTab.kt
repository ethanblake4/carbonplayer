package com.carbonplayer.model.entity.api

data class ExploreTab (
        val data_status: String?,
        val groups: List<ExploreEntityGroup>,
        val tab_type: String
)