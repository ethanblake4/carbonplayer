package com.carbonplayer.model.entity.radio

import org.json.JSONObject

@Suppress("DataClassPrivateConstructor")
data class RadioSeed private constructor(
        val albumId: String?,
        val artistId: String?,
        val curatedStationId: String?,
        val genreId: String?,
        val playlistShareToken: String?,
        val seedType: Int,
        val trackId: String?,
        val trackLockerId: String?
) {

    fun toJson() = JSONObject().apply {
        put("seedType", seedType)
        if(albumId != null) put("albumId", albumId)
        if(artistId != null) put("artistId", artistId)
        if(curatedStationId != null) put("curatedStationId", curatedStationId)
        if(genreId != null) put("genreId", genreId)
        if(playlistShareToken != null) put("playlistShareToken", playlistShareToken)
        if(trackId != null) put("trackId", trackId)
        if(trackLockerId != null) put("trackLockerId", trackLockerId)
    }

    companion object {

        fun create(sourceId: String, seedType: Int) : RadioSeed {

            fun seed(type: Int) =
                    if (type == seedType) sourceId else null

            return RadioSeed(
                    seed(4),
                    seed(3) ?: seed(7),
                    seed(9),
                    seed(5),
                    seed(6) ?: seed(8),
                    seedType,
                    seed(2),
                    seed(1)
            )
        }
    }
}