package com.carbonplayer.model.entity

import com.carbonplayer.utils.maybeGetBool
import com.carbonplayer.utils.maybeGetInt
import com.carbonplayer.utils.maybeGetObj
import com.carbonplayer.utils.maybeGetString
import io.realm.RealmObject
import org.json.JSONException
import org.json.JSONObject

/**
 * Skyjam and Realm
 */
open class Image(
        var kind: String = "",
        var url: String = "",
        var height: Int = 0,
        var width: Int = 0,
        var resizeStrategy: Int = 0,
        var aspectRatio: String? = null,
        var autogen: Boolean = false,
        var colorStyles: ColorStyles? = null
) : RealmObject() {

    @Throws(JSONException::class)
    constructor(json: JSONObject) : this(
            json.getString("kind"),
            json.getString("url"),
            json.maybeGetInt("height") ?: 0,
            json.maybeGetInt("width") ?: 0,
            json.maybeGetInt("resizeStrategy") ?: 0,
            json.maybeGetString("aspectRatio"),
            json.maybeGetBool("autogen") ?: false,
            json.maybeGetObj("colorStyles")?.let { ColorStyles(it) }
    )
}