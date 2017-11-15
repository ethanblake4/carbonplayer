package com.carbonplayer.ui.widget

import android.content.Context
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Scroller
import com.carbonplayer.utils.general.IdentityUtils

class NowPlayingQueueView: RecyclerView {

    var activePointerId = MotionEvent.INVALID_POINTER_ID
    var lastY = 0f
    var last2Y = 0f
    var initialHeight = -2
    val scroller = Scroller(context, FastOutSlowInInterpolator())
    var scrollHasControl = false
    val maxHeight = IdentityUtils.displayHeight2(context) -
            initialHeight
    var eventStartTime = 0L
    var eventInitialY = 0f
    var eventHasMotion = false
    var isUp = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, def: Int) : super(context, attrs, def)


}