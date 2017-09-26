package com.carbonplayer.model.entity

import io.realm.RealmObject
import org.json.JSONObject
import java.util.*

open class PlaylistEntry (
        var kind: String = "",
        var id: String = "",
        var clientId: String = "",
        var playlistId: String = "",
        var absolutePosition: String = "",
        var trackId: String = "",
        var creationTimestamp: Date = Date(),
        var lastModifiedTimestamp: Date = Date(),
        var deleted: Boolean = false,
        var source: String = "",
        var track: MusicTrack? = null
) : RealmObject() {
        constructor(json: JSONObject, track: MusicTrack? = null) : this (
                json.getString("kind"),
                json.getString("id"),
                json.getString("clientId"),
                json.getString("playlistId"),
                json.getString("absolutePosition"),
                json.getString("trackId"),
                Date(json.getString("creationTimestamp").toLong()),
                Date(json.getString("lastModifiedTimestamp").toLong()),
                json.getBoolean("deleted"),
                json.getString("source"),
                track ?: if(json.has("track"))
                    MusicTrack(json.getJSONObject("track"))
                else null
        )
}