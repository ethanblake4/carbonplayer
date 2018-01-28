package com.carbonplayer.model.entity.enums

enum class RadioFeedReason {
    DOWNLOAD,
    AUTO_CACHE,
    START,
    EXTENSION,
    SESSION_RESET,
    TRACK_EXPIRATION,
    ARTIST_SHUFFLE,
    INSTANT_MIX;

    fun toApiValue() = when(this) {
        DOWNLOAD -> "dl"
        AUTO_CACHE -> "sc"
        START -> "start"
        EXTENSION -> "ext"
        SESSION_RESET -> "reset"
        TRACK_EXPIRATION -> "exp"
        ARTIST_SHUFFLE -> "ash"
        INSTANT_MIX -> "im"
    }
}