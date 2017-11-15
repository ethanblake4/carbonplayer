package com.carbonplayer.ui.widget

import android.content.Context
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Scroller
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import timber.log.Timber


class NowPlayingFrame : FrameLayout {

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
    var runThread = true

    var thread: Thread = Thread(Runnable {
        while(runThread) {
            scroller.computeScrollOffset()
            if(scrollHasControl && scroller.currY != layoutParams.height) {
                layoutParams.height = scroller.currY
                postOnAnimation { requestLayout() }
                val upFraction = ((scroller.currY - initialHeight).toFloat() / (
                        maxHeight - initialHeight).toFloat())
                callback?.invoke(upFraction)
                isUp = upFraction > 0.99f

                //Timber.d("scroller: %d", scroller.currY)
            }
            Thread.sleep(16)
        }
    })
    var callback: ((Float) -> Unit)? = null

    init {
        thread.start()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if(isUp) {
            if(ev.rawY > IdentityUtils.displayWidth2(context)) return false
        } else {
            if (ev.rawX > IdentityUtils.displayWidth2(context) -
                    MathUtils.dpToPx2(context.resources, 134)) return false
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runThread = false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        //Timber.d("On touch event: %d", event?.actionMasked)
        when(event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                scrollHasControl = false
                val ac = event.actionIndex
                last2Y = lastY
                lastY = event.rawY
                eventInitialY = event.rawY
                activePointerId = event.getPointerId(ac)
                eventStartTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_MOVE -> {

                val dy = event.rawY - lastY
                if(!eventHasMotion && (event.rawY > eventInitialY + 1
                        || event.rawY < eventInitialY - 1)) eventHasMotion = true
                if(eventHasMotion) {
                    layoutParams.height = Math.max(layoutParams.height - dy.toInt(), initialHeight)
                    postOnAnimation { requestLayout() }
                    callback?.invoke(((layoutParams.height - initialHeight).toFloat() / (
                            maxHeight - initialHeight).toFloat()))
                }
                last2Y = lastY
                lastY = event.rawY
            }
            MotionEvent.ACTION_UP -> {

                val location = intArrayOf(0, 0)

                getLocationInWindow(location)

                if(eventHasMotion) {

                    Timber.d("Fling from startY: %d, velocity: %d, endY: %d",
                            measuredHeight, (last2Y - lastY).toInt(),
                            IdentityUtils.displayHeight2(context))

                    val velocity = (last2Y - lastY).toInt()
                    val curHeight = measuredHeight

                    if (velocity > 10) {
                        scroller.fling(0, curHeight, 0,
                                velocity, 0, 0, curHeight, Integer.MAX_VALUE)
                        scroller.finalY = maxHeight
                    } else if (velocity < -10) {
                        scroller.fling(0, curHeight, 0,
                                velocity, 0, 0, initialHeight, Integer.MAX_VALUE)
                        scroller.finalY = initialHeight
                    } else {
                        if (curHeight > maxHeight / 2) {
                            scroller.fling(0, curHeight, 0,
                                    velocity, 0, 0, curHeight, Integer.MAX_VALUE)
                            scroller.finalY = maxHeight
                        } else {
                            scroller.fling(0, curHeight, 0,
                                    velocity, 0, 0, initialHeight, Integer.MAX_VALUE)
                            scroller.finalY = initialHeight
                        }
                    }

                    scroller.extendDuration(500)

                    scrollHasControl = true
                    eventHasMotion = false

                } else if( scroller.isFinished ){
                    activePointerId = MotionEvent.INVALID_POINTER_ID

                    if (measuredHeight <= initialHeight + 12) {
                        scroller.startScroll(0, initialHeight, 0, maxHeight
                                - initialHeight, 500)
                        scrollHasControl = true
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {

                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if(event.actionIndex == 0) 1 else 0
                    lastY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }

            }

        }

        return true
    }

}