package com.carbonplayer.model.entity.radio.response

import com.carbonplayer.model.entity.radio.RadioConstraints
import com.carbonplayer.model.entity.radio.SkyjamStation

data class RadioFeedResponse(
        val data: RadioFeed
)

data class RadioFeed(
        val currentTimestampMillis: Long = 0,
        val endOfFeed: Boolean = false,
        val radioConstraints: RadioConstraints = RadioConstraints(),
        val stations: List<SkyjamStation>
)