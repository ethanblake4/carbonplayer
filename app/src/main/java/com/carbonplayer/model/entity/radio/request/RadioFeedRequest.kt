package com.carbonplayer.model.entity.radio.request

import com.carbonplayer.model.entity.radio.RadioSeed
import com.carbonplayer.model.entity.radio.RecentlyPlayedEntry
import org.json.JSONObject


data class RadioFeedRequest (
        val contentFilter: Int,
        val mixes: MixRequest?,
        val stations: List<RadioStationRequest>
) {

    fun toJson() = JSONObject().apply {
        put("contentFilter", contentFilter)
        if(mixes != null) put("mixes", mixes.toJson())
    }

    data class MixRequest(
            val numEntries: Int,
            val numSeeds: Int
    ) {
        fun toJson() = JSONObject().apply {
            put("numEntries", numEntries)
            put("numSeeds", numSeeds)
        }
    }

    data class RadioStationRequest(
            val libraryContentOnly: Boolean = false,
            val numEntries: Int,
            val radioId: String?,
            val recentlyPlayed: List<RecentlyPlayedEntry>?,
            val searchEntryContext: String?,
            val seed: RadioSeed,
            val sessionToken: String?
    ) {
        fun toJson() = JSONObject().apply {
            put("libraryContentOnly", libraryContentOnly)
            put("numEntries", numEntries)
            if(radioId != null) put("radioId", radioId)
            if(recentlyPlayed != null) put("recentlyPlayed", recentlyPlayed)
            if(searchEntryContext != null) put("searchEntryContext", searchEntryContext)
            put("seed", seed.toJson())
            if(sessionToken != null) put("sessionToken", sessionToken)
        }
    }
}