package com.carbonplayer.ui.helpers

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.ITrack
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

    fun artistShuffle(artistId: String) {

    }
}