package com.carbonplayer.utils.protocol

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import com.carbonplayer.model.entity.proto.innerjam.visuals.ColorV1Proto


object ProtoUtils {

    @JvmStatic @JvmOverloads @ColorInt
    fun colorFrom(color: ColorV1Proto.Color, alpha: Boolean = false) : Int {
        color.rgbaSpace?.let {
            return if (alpha) Color.argb(it.alpha, it.red, it.green, it.blue)
            else Color.rgb(it.red, it.green, it.blue)
        }
        return Color.DKGRAY
    }

    @JvmStatic @FloatRange(from = 0.0, to = 1.0) fun alphaFrom(color: ColorV1Proto.Color) : Float {
        color.rgbaSpace?.let {
            return it.alpha.div(255.0f)
        }
        return 1.0f
    }

}