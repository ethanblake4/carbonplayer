package com.carbonplayer.model.entity.radio.response

import com.carbonplayer.model.entity.radio.RadioConstraints
import com.carbonplayer.model.entity.radio.SkyjamStation

data class RadioFeedResponse(
        val currentTimestampMillis: Long,
        val endOfFeed: Boolean,
        val radioConstraints: RadioConstraints,
        val stations: List<SkyjamStation>
)