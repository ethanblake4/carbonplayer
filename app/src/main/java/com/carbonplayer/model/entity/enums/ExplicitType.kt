package com.carbonplayer.model.entity.enums

enum class ExplicitType (
        cloudType: Int,
        contentType: Int,
        innerjamType: Int
) {
    UNKNOWN(0, 0, 0),
    EXPLICIT(1, 1, 2),
    CLEAN(2, 2, 0),
    EDITED(3,3,0);

    companion object {

        fun fromCloudType(cloudType: Int) = when (cloudType) {
            1 -> EXPLICIT
            2 -> CLEAN
            3 -> EDITED
            else -> UNKNOWN
        }

        fun fromContentType(contentType: Int) = when (contentType) {
            1 -> EXPLICIT
            2 -> CLEAN
            3 -> EDITED
            else -> UNKNOWN
        }

        fun fromInnerjamType(innerjamType: Int) = when(innerjamType) {
            2 -> EXPLICIT
            else -> UNKNOWN
        }

    }


}