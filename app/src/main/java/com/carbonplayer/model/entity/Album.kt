package com.carbonplayer.model.entity

import java.util.Date

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Album data class
 */
data class Album(@PrimaryKey val id: String?, var recentTimestamp: Date, private var _title: String?, var artist: String,
                 var composer: String, var year: Int?, var genre: String, var albumArtURL: String,
                 var artistId: RealmList<RealmString>, var songIds: RealmList<RealmString>) : RealmObject() {

    constructor(track: MusicTrack) : this(track.albumId, track.recentTimestamp,
            if (track.album != "") track.album else "Unknown album", track.artist,
            track.composer, track.year, track.genre, track.albumArtURL,
            track.artistId, RealmList(RealmString(track.trackId)))


    var title = _title
        set(title) {
            field = if (title != "") title else "Unknown album"
        }


    infix fun addSong(songId: String) {
        this.songIds.add(RealmString(songId))
    }

    companion object Keys {
        var id = "id"
        var title = "title"
        var artist = "artist"
    }
}