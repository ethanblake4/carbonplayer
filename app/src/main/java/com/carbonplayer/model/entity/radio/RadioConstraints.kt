package com.carbonplayer.model.entity.radio

data class RadioConstraints (
        val prefetchLeadTimeMillis: Long = 0L,
        val prefetchesAllowed: Int = -1,
        val skipEnforcementPeriodMillis: Long = 0L,
        val skipsAllowed: Int = -1
)