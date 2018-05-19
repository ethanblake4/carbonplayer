package com.carbonplayer.model.entity.api

import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamTrack

data class ExploreEntity (
        val album: SkyjamAlbum?,
        val playlist: SkyjamPlaylist?,
        val track: SkyjamTrack?
)