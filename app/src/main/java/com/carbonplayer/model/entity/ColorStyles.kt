package com.carbonplayer.model.entity

import android.annotation.SuppressLint
import android.os.Parcelable
import io.realm.RealmObject
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

open class ColorStyles(
        var primary: RGBStyleColor? = RGBStyleColor(),
        var scrim: RGBStyleColor? = RGBStyleColor(),
        var accent: RGBStyleColor? = RGBStyleColor()
) : RealmObject()

open class RGBStyleColor(
        var red: Int = 0,
        var green: Int = 0,
        var blue: Int = 0
) : RealmObject()

