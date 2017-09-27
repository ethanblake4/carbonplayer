package com.carbonplayer.model.entity

import io.realm.RealmObject
import org.json.JSONException
import org.json.JSONObject

open class Image(
        var kind: String = "",
        var url: String = "",
        var aspectRatio: String? = null,
        var autogen: Boolean = false,
        var colorStyles: ColorStyles? = null
) : RealmObject() {

    @Throws(JSONException::class)
    constructor(json: JSONObject) : this(
            json.getString("kind"),
            json.getString("url"),
            "aspectRatio".let { if (json.has(it)) json.getString(it) else null },
            "autogen".let { if (json.has(it)) json.getBoolean(it) else false },
            "colorStyles".let { if (json.has(it)) ColorStyles(json.getJSONObject(it)) else null }
    )
}