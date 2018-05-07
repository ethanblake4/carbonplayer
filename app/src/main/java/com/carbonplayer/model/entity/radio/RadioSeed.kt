package com.carbonplayer.model.entity.radio

import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist

@Suppress("DataClassPrivateConstructor")
data class RadioSeed private constructor(
        val albumId: String?,
        val artistId: String?,
        val curatedStationId: String?,
        val genreId: String?,
        val playlistShareToken: String?,
        val seedType: Int,
        val trackId: String?,
        val trackLockerId: String?,
        val metadataSeed: RadioSeedMetadata? = null
) {

    data class RadioSeedMetadata(
            val artist: SkyjamArtist? = null,
            val album: SkyjamAlbum? = null,
            val playlist: SkyjamPlaylist? = null
    )

    companion object {

        const val TYPE_LIBRARY_TRACK = 1
        const val TYPE_SJ_TRACK = 2
        const val TYPE_ARTIST = 3
        const val TYPE_ALBUM = 4
        const val TYPE_GENRE = 5
        const val TYPE_PLAYLIST = 6 // and 8
        const val TYPE_ARTIST_FOR_SHUFFLE = 7
        const val TYPE_CURATED_STATION = 9

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