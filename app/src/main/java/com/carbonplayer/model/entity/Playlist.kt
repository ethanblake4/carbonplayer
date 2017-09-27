package com.carbonplayer.model.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Album data class
 */

open class Playlist(

        @PrimaryKey var id: String = "",
        var kind: String = "",
        var name: String = "",
        var deleted: Boolean = false,
        var type: String? = null,
        var lastModifiedTimestamp: Date? = null,
        var recentTimestamp: Date? = null,
        var shareToken: String = "",
        var ownerProfilePhotoUrl: String? = null,
        var ownerName: String? = null,
        var accessControlled: Boolean = false,
        var shareState: String? = null,
        var creationTimestamp: Date? = null,
        var albumArtURL: String? = "",
        var description: String = "",
        var explicitType: String? = null,
        var contentType: String? = null

) : RealmObject() {

    @Throws(JSONException::class)
    constructor(json: JSONObject) : this(
            json.getString("id"),
            json.getString("kind"),
            json.getString("name"),
            if (json.has("deleted")) json.getBoolean("deleted") else false,
            if (json.has("type")) json.getString("type") else null,
            "lastModifiedTimestamp".let { if (json.has(it)) Date(json.getString(it).toLong()) else null },
            "recentTimestamp".let { if (json.has(it)) Date(json.getString(it).toLong()) else null },
            json.getString("shareToken"),
            "ownerProfilePhotoUrl".let { if (json.has(it)) json.getString(it) else null },
            "ownerName".let { if (json.has(it)) json.getString(it) else null },
            "accessControlled".let { if (json.has(it)) json.getBoolean(it) else false },
            "shareState".let { if (json.has(it)) json.getString(it) else null },
            "creationTimestamp".let { if (json.has(it)) Date(json.getString(it).toLong()) else null },
            "albumArtRef".let { if (json.has(it)) json.getJSONObject(it).getString("url") else null },
            "description".let { if (json.has(it)) json.getString(it) else "" },
            "explicitType".let { if (json.has(it)) json.getString(it) else null },
            "contentType".let { if (json.has(it)) json.getString(it) else null }
    )
}