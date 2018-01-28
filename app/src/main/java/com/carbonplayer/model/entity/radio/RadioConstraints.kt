package com.carbonplayer.model.entity.radio

import com.carbonplayer.utils.maybeGetInt
import com.carbonplayer.utils.maybeGetLong
import org.json.JSONObject

data class RadioConstraints (
        val prefetchLeadTimeMillis: Long,
        val prefetchesAllowed: Int,
        val skipEnforcementPeriodMillis: Long,
        val skipsAllowed: Int
) {
    constructor(json: JSONObject) : this (
            json.maybeGetLong("prefetchLeadTimeMillis") ?: 0L,
            json.maybeGetInt("prefetchesAllowed") ?: -1,
            json.maybeGetLong("skipEnforcementPeriodMillis") ?: 0L,
            json.maybeGetInt("skipsAllowed") ?: -1
    )
}