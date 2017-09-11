package com.carbonplayer.ui.helpers

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.ui.main.MainActivity
import io.realm.Realm

class MusicManager (
    val mainActivity: MainActivity
) {

    fun fromAlbum (albumId: String) {
        mainActivity.npUiHelper.newQueue(
            MusicLibrary.getInstance().getAllAlbumTracks(albumId)
        )
    }
}