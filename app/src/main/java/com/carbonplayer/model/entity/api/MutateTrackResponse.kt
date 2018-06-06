package com.carbonplayer.model.entity.api

import com.carbonplayer.model.entity.enums.MutateResponseCode

data class MutateTrackResponse (
        val id: String?,
        val response_code: String?
) {
    val wasSuccessful: Boolean get() =
        response_code != MutateResponseCode.CONFLICT.name
        && response_code != MutateResponseCode.INVALID_REQUEST.name
        && response_code != MutateResponseCode.TOO_MANY_ITEMS.name

    data class Batch (
            val mutate_response: List<MutateTrackResponse>
    ) {
        val wasSuccessful: Boolean get() = mutate_response.firstOrNull()?.wasSuccessful ?: false
        val responseCode: String get() = mutate_response.firstOrNull()?.response_code ?: "unknown"
    }
}