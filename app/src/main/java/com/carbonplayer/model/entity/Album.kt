package com.carbonplayer.model.entity

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.json.JSONObject
import java.util.*

/**
 * Album data class
 */

open class Album(
        var kind: String = "",
        @PrimaryKey var id: String = "",
        var recentTimestamp: Date? = null,
        @Ignore private var _title: String = "",
        var albumArtist: String = "",
        var artist: String = "",
        var composer: String = "",
        var year: Int = 0,
        var genre: String = "",
        var albumArtURL: String = "",
        var artistId: RealmList<RealmString> = RealmList(),
        var songIds: RealmList<RealmString> = RealmList(),
        var description: String? = null,
        var descAttribution: Attribution? = null,
        var explicitType: String? = null,
        var contentType: String? = null

) : RealmObject() {

    constructor(json: JSONObject) : this(
            json.getString("kind"),
            json.getString("albumId"),
            "recentTimestamp".let { if (json.has(it)) Date(json.getString(it).toLong()) else null },
            json.getString("name"),
            json.getString("albumArtist"),
            json.getString("artist"),
            "composer".let { if (json.has(it)) json.getString(it) else "" },
            "year".let { if (json.has(it)) json.getInt(it) else 0 },
            "genre".let { if (json.has(it)) json.getString(it) else "" },
            "albumArtRef".let { if (json.has(it)) json.getString(it) else "" },
            json.getJSONArray("artistId").let {
                val ret = RealmList<RealmString>()
                (0..it.length()).mapTo(ret) { n -> RealmString(it.getString(n)) }
                ret
            },
            if (json.has("tracks")) json.getJSONArray("tracks").let {
                val ret = RealmList<RealmString>()
                (0..it.length()).mapTo(ret) { n ->
                    RealmString(it.getJSONObject(n).getString(MusicTrack.ID))
                }
                ret
            } else RealmList(),
            "description".let { if (json.has(it)) json.getString(it) else null },
            "description_attribution".let {
                if (json.has(it)) Attribution(json.getJSONObject(it)) else null
            },
            "explicitType".let { if (json.has(it)) json.getString(it) else null },
            "contentType".let { if (json.has(it)) json.getString(it) else null }
    )

    constructor(track: MusicTrack) : this("", track.albumId ?: "unknownID", track.recentTimestamp,
            if (track.album != "") (track.album ?: "") else "Unknown album",
            "", track.artist ?: "",
            track.composer ?: "", track.year ?: 0, track.genre ?: "", track.albumArtURL ?: "",
            track.artistId ?: RealmList(), RealmList(RealmString(track.trackId)))


    var title = _title
        set(title) {
            field = if (title != "") title else "Unknown album"
        }

    infix fun addSong(songId: String) {
        this.songIds.add(RealmString(songId))
    }

    companion object {
        val ID = "id"
    }
}