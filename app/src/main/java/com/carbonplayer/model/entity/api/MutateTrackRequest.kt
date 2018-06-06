package com.carbonplayer.model.entity.api

import com.carbonplayer.model.entity.skyjam.SkyjamAddTrack
import com.carbonplayer.model.entity.skyjam.SkyjamTrack

data class MutateTrackRequest (
        val create: SkyjamAddTrack?,
        val delete: String?,
        val update: SkyjamTrack?
) {
    data class Batch (val mutations: List<MutateTrackRequest>)
}