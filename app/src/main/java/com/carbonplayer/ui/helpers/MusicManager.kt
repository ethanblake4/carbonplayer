package com.carbonplayer.ui.helpers

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.ui.main.MainActivity

class MusicManager(
        val mainActivity: MainActivity
) {

    fun fromAlbum(album: IAlbum, pos: Int) {
        mainActivity.npHelper.newQueue(
                MusicLibrary.getAllAlbumTracks(album),
                pos
        )
    }

    fun artistShuffle(artistId: String) {

    }
}