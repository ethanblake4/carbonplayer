package com.carbonplayer.model.network.entity

import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.entity.enums.StreamQuality


data class DownloadRequest(
        var id: SongID? = null,
        var trackTitle: String,
        val priority: Int,
        val seekMillis: Long,
        var fileLocation: FileLocation,
        val explicit: Boolean,
        val requestedQuality: StreamQuality,
        val existingQuality: StreamQuality
) {

    var state: State? = null

    private val minPriority: Int get() = PRIORITY_AUTOCACHE
    private val maxPriority: Int get() = PRIORITY_STREAM

    enum class State {
        NOT_STARTED,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        CANCELED
    }

    init {
        if (seekMillis < 0) {
            throw IllegalArgumentException("Negative seek time: " + seekMillis)
        } else if (requestedQuality == StreamQuality.UNDEFINED) {
            throw IllegalArgumentException("Requested quality cannot be unknown")
        } else if (existingQuality != StreamQuality.UNDEFINED || requestedQuality <= existingQuality) {
            throw IllegalArgumentException(
                    "Existing quality ${existingQuality.name} must exceed requested quality ${requestedQuality.name}"
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DownloadRequest) return false
        return this.id === other.id && this.seekMillis == other.seekMillis
    }

    companion object {
        var PRIORITY_AUTOCACHE = 200
        var PRIORITY_KEEPON = 100
        var PRIORITY_PREFETCH1 = 1
        var PRIORITY_PREFETCH2 = 2
        var PRIORITY_PREFETCH3 = 3
        var PRIORITY_PREFETCH4 = 4
        var PRIORITY_STREAM = 0
    }
}
