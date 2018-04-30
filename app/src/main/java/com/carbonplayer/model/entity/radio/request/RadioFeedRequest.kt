package com.carbonplayer.model.entity.radio.request

import com.carbonplayer.model.entity.radio.RadioSeed
import com.carbonplayer.model.entity.radio.RecentlyPlayedEntry


data class RadioFeedRequest (
        val contentFilter: Int,
        val mixes: MixRequest?,
        val stations: List<RadioStationRequest>
) {

    data class MixRequest(
            val numEntries: Int,
            val numSeeds: Int
    )

    data class RadioStationRequest(
            val libraryContentOnly: Boolean = false,
            val numEntries: Int,
            val radioId: String?,
            val recentlyPlayed: List<RecentlyPlayedEntry>?,
            val searchEntryContext: String?,
            val seed: RadioSeed,
            val sessionToken: String?
    )
}