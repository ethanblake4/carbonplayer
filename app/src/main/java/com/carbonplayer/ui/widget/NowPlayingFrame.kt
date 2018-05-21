package com.carbonplayer.ui.widget

import android.content.Context
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Scroller
import com.carbonplayer.utils.carbonAnalytics
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import kotlinx.android.synthetic.main.nowplaying.view.*
import timber.log.Timber

/**
 * A FrameLayout that can be expanded via swipe to increase its height and
 * reduce its vertical screen position, creating the appearance of swiping up
 *
 * It also sends its current position via a callback to allow for animations
 */
class NowPlayingFrame : FrameLayout {

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastY = 0f
    private var last2Y = 0f
    var initialHeight = -2
    private val scroller = Scroller(context, FastOutSlowInInterpolator())
    private var scrollHasControl = false
    private val maxHeight = IdentityUtils.displayHeight2(context) -
            initialHeight
    private var eventStartTime = 0L
    private var eventInitialY = 0f
    private var eventHasMotion = false
    var isUp = false
    private var runThread = true

    private lateinit var runner: Runnable
    var callback: ((Float) -> Unit)? = null

    init {
        //thread.start()
        runner = Runnable {
            if(runThread) {
                scroller.computeScrollOffset()
                if(scrollHasControl && scroller.currY != layoutParams.height) {
                    layoutParams.height = scroller.currY
                    requestLayout()
                    val upFraction = ((scroller.currY - initialHeight).toFloat() / (
                            maxHeight - initialHeight).toFloat())
                    callback?.invoke(upFraction)
                    val prevWasUp = isUp
                    isUp = upFraction > 0.99f

                    if(prevWasUp != isUp) carbonAnalytics.logEvent(
                            if(isUp) "open_npui" else "close_npui", null)

                    //Timber.d("scroller: %d", scroller.currY)
                }
                postOnAnimation(runner)
            }
        }
        postOnAnimation(runner)
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

        if(!npui_recycler.scrollHasControl || npui_recycler.lastUpFraction > 0.75f) return false

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
                /* On a touch, set the current active pointer and remove control from
                 * the scroller */
                scrollHasControl = false
                val ac = event.actionIndex
                last2Y = lastY
                lastY = event.rawY
                eventInitialY = event.rawY
                activePointerId = event.getPointerId(ac)
                eventStartTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_MOVE -> {
                /*
                 * When movement occurs, change our height and callback the new position
                 */
                val dy = event.rawY - lastY
                if(!eventHasMotion && (event.rawY > eventInitialY + 1
                        || event.rawY < eventInitialY - 1)) eventHasMotion = true
                if(eventHasMotion) {
                    layoutParams.height = minOf (
                            maxOf(layoutParams.height - dy.toInt(), initialHeight),
                            maxHeight)
                    postOnAnimation {
                        requestLayout()
                        callback?.invoke(((layoutParams.height - initialHeight).toFloat() / (
                                maxHeight - initialHeight).toFloat()))
                    }

                }
                last2Y = lastY
                lastY = event.rawY
            }
            MotionEvent.ACTION_UP -> {

                /* On an up event:
                *   - If there was motion, start a fling with the current velocity
                *   - If there was no motion, start a scroll to the nearest position OR
                 *  - If the motion was a tap and isUp is FALSE, scroll to the top */

                val location = intArrayOf(0, 0)

                getLocationInWindow(location)

                if(eventHasMotion) {

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

                    Timber.d("Fling from startY: $curHeight," +
                            " velocity: $velocity, endY: ${scroller.finalY}")

                    scroller.extendDuration(400)

                    scrollHasControl = true
                    eventHasMotion = false

                } else if( scroller.isFinished ) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID

                    if (measuredHeight <= initialHeight + 12) {
                        scroller.startScroll(0, initialHeight, 0, maxHeight
                                - initialHeight, 400)
                        Timber.d("Scroll from startY: $initialHeight," +
                                "dy: ${ maxHeight - initialHeight }")
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