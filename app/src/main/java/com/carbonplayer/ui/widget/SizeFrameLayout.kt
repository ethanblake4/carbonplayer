package com.carbonplayer.ui.widget

import android.content.Context
import android.widget.FrameLayout
import timber.log.Timber

class SizeFrameLayout (
        context: Context
) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Timber.d("onmeause")
        super.onMeasure(MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY))
    }
}