package com.carbonplayer.ui.helpers

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.ui.main.MainActivity

class MusicManager(
        val mainActivity: MainActivity
) {

    fun fromAlbum(albumId: String, pos: Int) {
        mainActivity.npHelper.newQueue(
                MusicLibrary.getInstance().getAllAlbumTracks(albumId),
                pos
        )
    }
}