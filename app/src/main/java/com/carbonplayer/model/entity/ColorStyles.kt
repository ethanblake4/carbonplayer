package com.carbonplayer.model.entity

import android.annotation.SuppressLint
import android.os.Parcelable
import io.realm.RealmObject
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
open class ColorStyles(
        var primary: RGBStyleColor? = RGBStyleColor(),
        var scrim: RGBStyleColor? = RGBStyleColor(),
        var accent: RGBStyleColor? = RGBStyleColor()
) : RealmObject(), Parcelable

@SuppressLint("ParcelCreator")
@Parcelize
open class RGBStyleColor(
        var red: Int = 0,
        var green: Int = 0,
        var blue: Int = 0
) : RealmObject(), Parcelable

