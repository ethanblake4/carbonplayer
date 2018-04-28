package com.carbonplayer.model.entity.utils

import android.content.res.Resources
import com.carbonplayer.R

object MediaTypeUtil {

    const val TYPE_SONG = 1
    const val TYPE_ARTIST = 2
    const val TYPE_ALBUM = 3
    const val TYPE_PLAYLIST = 4
    const val TYPE_GENRE = 5
    const val TYPE_STATION = 6
    const val TYPE_SITUATION = 7
    const val TYPE_VIDEO = 8
    const val TYPE_PODCAST = 9

    fun getMediaTypeString(res: Resources, mediaType: Int): String = res.getString(when (mediaType) {
        TYPE_SONG -> R.string.media_type_song
        TYPE_ARTIST -> R.string.media_type_artist
        TYPE_ALBUM -> R.string.media_type_album
        TYPE_PLAYLIST -> R.string.media_type_playlist
        TYPE_GENRE -> R.string.media_type_genre
        TYPE_STATION -> R.string.media_type_station
        TYPE_SITUATION -> R.string.media_type_situation
        TYPE_VIDEO -> R.string.media_type_video
        TYPE_PODCAST -> R.string.media_type_podcast
        else -> R.string.media_type_unknown
    })
}