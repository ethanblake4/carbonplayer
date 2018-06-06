package com.carbonplayer.model.entity.utils

enum class MetajamIdUtil(private val idPrefix: String) {

    ALBUM("B"),
    ARTIST("A"),
    CURATED_STATION("L"),
    TRACK("T"),
    PODCAST_EPISODE("D"),
    PODCAST_SERIES("I"),
    SITUATION("N");

    fun normalizeMetajamId(id: String): String {
        if(id.isEmpty() || id.startsWith(idPrefix)) return id
        return idPrefix + id
    }
}