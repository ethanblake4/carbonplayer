package com.carbonplayer.model.entity

import io.realm.RealmObject

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
) : RealmObject()
