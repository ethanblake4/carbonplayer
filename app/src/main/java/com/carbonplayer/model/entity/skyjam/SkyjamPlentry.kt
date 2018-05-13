package com.carbonplayer.model.entity.skyjam

data class SkyjamPlentry(
        val kind: String,
        val id: String,
        val clientId: String?,
        val playlistId: String?,
        val absolutePosition: String,
        val creationTimestamp: Long,
        val lastModifiedTimestamp: Long,
        val deleted: Boolean,
        val source: String, /* 1 = locker, 2 = nautilus */
        val track: SkyjamTrack?,
        val trackId: String
)