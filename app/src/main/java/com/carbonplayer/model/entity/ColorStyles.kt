package com.carbonplayer.model.entity

import io.realm.RealmObject
import org.json.JSONException
import org.json.JSONObject

open class ColorStyles(
        var primary: RGBStyleColor = RGBStyleColor(),
        var scrim: RGBStyleColor = RGBStyleColor(),
        var accent: RGBStyleColor = RGBStyleColor()
) : RealmObject() {

    @Throws(JSONException::class)
    constructor(json: JSONObject) : this(
            RGBStyleColor(json.getJSONObject("primary")),
            RGBStyleColor(json.getJSONObject("scrim")),
            RGBStyleColor(json.getJSONObject("accent"))
    )
}

open class RGBStyleColor(
        var red: Int = 0,
        var green: Int = 0,
        var blue: Int = 0
) : RealmObject() {
    @Throws(JSONException::class)
    constructor(json: JSONObject) : this(
            json.getInt("red"),
            json.getInt("green"),
            json.getInt("blue")
    )
}


