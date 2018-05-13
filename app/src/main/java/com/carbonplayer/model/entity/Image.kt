package com.carbonplayer.model.entity

import android.annotation.SuppressLint
import android.os.Parcelable
import io.realm.RealmObject
import kotlinx.android.parcel.Parcelize

/**
 * Skyjam and Realm
 */
@SuppressLint("ParcelCreator")
@Parcelize
open class Image(
        var kind: String = "",
        var url: String = "",
        var height: Int = 0,
        var width: Int = 0,
        var resizeStrategy: Int = 0,
        var aspectRatio: String? = null,
        var autogen: Boolean = false,
        var colorStyles: ColorStyles? = null
) : RealmObject(), Parcelable
