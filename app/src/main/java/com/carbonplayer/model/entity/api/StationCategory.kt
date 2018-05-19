package com.carbonplayer.model.entity.api

data class StationCategory (
        val display_name: String,
        val id: String,
        val subcategories: List<StationCategory>?
) {
    companion object {
        val DEFAULT = StationCategory("", "", listOf())
    }
}