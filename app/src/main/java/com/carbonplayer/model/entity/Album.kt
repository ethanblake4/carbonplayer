package com.carbonplayer.model.entity

import android.media.MediaMetadata
import java.util.Date

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey

/**
 * Album data class
 */

open class Album(

        @PrimaryKey var id: String = "",
        var recentTimestamp: Date? = null,
        @Ignore private var _title: String = "",
        var artist: String = "",
        var composer: String = "",
        var year: Int = 0,
        var genre: String = "",
        var albumArtURL: String = "",
        var artistId: RealmList<RealmString> = RealmList(RealmString("")),
        var songIds: RealmList<RealmString> = RealmList(RealmString(""))

) : RealmObject() {

    constructor(track: MusicTrack) : this(track.albumId?: "unknownID", track.recentTimestamp,
            if (track.album != "") (track.album?: "") else "Unknown album", track.artist?: "",
            track.composer?: "", track.year?: 0, track.genre?: "", track.albumArtURL?: "",
            track.artistId?: RealmList(), RealmList(RealmString(track.trackId)))


    var title = _title
        set(title) {
            field = if (title != "") title else "Unknown album"
        }

    infix fun addSong(songId: String) {
        this.songIds.add(RealmString(songId))
    }
}