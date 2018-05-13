package com.carbonplayer.ui.helpers

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.RadioFeedReason
import com.carbonplayer.model.entity.radio.RadioSeed
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.ui.main.MainActivity

class MusicManager(
        private val mainActivity: MainActivity
) {

    fun fromAlbum(album: IAlbum, pos: Int) {
        mainActivity.npHelper.newQueue(
                MusicLibrary.getAllAlbumTracks(album),
                pos
        )
    }

    fun fromTracks(tracks: List<ITrack>, pos: Int, local: Boolean = true) {
        mainActivity.npHelper.newQueue(tracks, pos, local)
    }

    fun radio(album: IAlbum) {
        mainActivity.npHelper.startRadio(RadioSeed.TYPE_ALBUM, album.albumId)
    }

    fun radio(station: SkyjamStation) {
        val seedType = station.seed?.seedType ?: return
        val seed = station.seed.albumId ?: station.seed.artistId ?:
                station.seed.curatedStationId ?: station.seed.genreId ?:
                station.seed.playlistShareToken ?: station.seed.trackId ?:
                station.seed.trackLockerId ?: return
        mainActivity.npHelper.startRadio(seedType, seed)
    }

    fun artistShuffle(artistId: String) {
        mainActivity.npHelper.startRadio(RadioSeed.TYPE_ARTIST, artistId,
                RadioFeedReason.ARTIST_SHUFFLE)
    }
}