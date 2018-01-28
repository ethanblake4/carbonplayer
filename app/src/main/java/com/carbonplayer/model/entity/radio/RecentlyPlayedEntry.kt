package com.carbonplayer.model.entity.radio

import org.json.JSONObject


data class RecentlyPlayedEntry (
        val id: String,
        val type: Int
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("type", type)
    }
}