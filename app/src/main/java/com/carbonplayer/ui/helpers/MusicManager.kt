package com.carbonplayer.ui.helpers

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.RadioFeedReason
import com.carbonplayer.model.entity.radio.RadioSeed
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

    fun artistShuffle(artistId: String) {
        mainActivity.npHelper.startRadio(RadioSeed.TYPE_ARTIST, artistId,
                RadioFeedReason.ARTIST_SHUFFLE)
    }
}