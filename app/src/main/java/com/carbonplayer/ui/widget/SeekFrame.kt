package com.carbonplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.carbonplayer.utils.general.IdentityUtils

class SeekFrame : FrameLayout {

    var callback: ((Float) -> Unit)? = null
    var upCallback: ((Float) -> Unit)? = null

    var dispW = IdentityUtils.displayWidth2(context)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent) = true

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        (if(event?.actionMasked == MotionEvent.ACTION_UP) upCallback else callback)?.let{
            (event?.rawX)?.div(dispW)?.let { f ->
                it((maxOf(minOf(f, 0.925f), 0.075f) - 0.075f) * (1.0f/0.85f))
            }
        }
        return super.onTouchEvent(event)
    }
}