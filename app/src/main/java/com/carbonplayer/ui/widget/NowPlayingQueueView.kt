package com.carbonplayer.ui.widget

import android.content.Context
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Scroller
import com.carbonplayer.utils.general.IdentityUtils
import timber.log.Timber

/* A recyclerview that allows to be swiped up and down. If it is swiped up,
    it must be scrolled to the top before it can be scrolled down.
 */
class NowPlayingQueueView: RecyclerView {

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastY = 0f
    private var last2Y = 0f
    private var initialY = -2f
    private val scroller = Scroller(context, FastOutSlowInInterpolator())
    private var scrollHasControl = false
    private val maxY = IdentityUtils.displayHeight2(context) -
            initialY
    private var eventStartTime = 0L
    private var eventInitialY = 0f
    private var eventHasMotion = false
    var isUp = false

    var callback: ((Float) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, def: Int) : super(context, attrs, def)

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        //Timber.d("On touch event: %d", event?.actionMasked)


        when(event?.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                /* On a touch, set the current active pointer and remove control from
                 * the scroller */

                super.onTouchEvent(event)


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
                if(eventHasMotion && dy > 0 && scrollY <= 0) {
                    y = Math.max(y - dy, initialY)
                    postOnAnimation { requestLayout() }
                    callback?.invoke(((y - initialY) / (
                            maxY - initialY)))
                } else {
                    super.onTouchEvent(event)
                }
                last2Y = lastY
                lastY = event.rawY
            }
            MotionEvent.ACTION_UP -> {

                /* On an up event:
                *   - If there was motion, start a fling with the current velocity
                *   - If there was no motion, start a scroll to the nearest position OR
                 *  - If the motion was a tap and isUp is FALSE, scroll to the top */

                super.onTouchEvent(event)

                val location = intArrayOf(0, 0)

                getLocationInWindow(location)

                if(eventHasMotion) {

                    Timber.d("Fling from startY: %d, velocity: %d, endY: %d",
                            location[1], (last2Y - lastY).toInt(),
                            IdentityUtils.displayHeight2(context))

                    val velocity = (last2Y - lastY).toInt()
                    val curY = location[1]

                    if (velocity > 10) {
                        scroller.fling(0, curY, 0,
                                velocity, 0, 0, curY, Integer.MAX_VALUE)
                        scroller.finalY = maxY.toInt()
                    } else if (velocity < -10) {
                        scroller.fling(0, curY, 0,
                                velocity, 0, 0, initialY.toInt(), Integer.MAX_VALUE)
                        scroller.finalY = initialY.toInt()
                    } else {
                        if (curY > maxY / 2) {
                            scroller.fling(0, curY, 0,
                                    velocity, 0, 0, curY, Integer.MAX_VALUE)
                            scroller.finalY = maxY.toInt()
                        } else {
                            scroller.fling(0, curY, 0,
                                    velocity, 0, 0, initialY.toInt(), Integer.MAX_VALUE)
                            scroller.finalY = initialY.toInt()
                        }
                    }

                    scroller.extendDuration(500)

                    scrollHasControl = true
                    eventHasMotion = false

                } else if( scroller.isFinished ){
                    activePointerId = MotionEvent.INVALID_POINTER_ID

                    if (measuredHeight <= initialY + 12) {
                        scroller.startScroll(0, initialY.toInt(), 0, maxY.toInt()
                                - initialY.toInt(), 500)
                        scrollHasControl = true
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                super.onTouchEvent(event)
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                super.onTouchEvent(event)
                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if(event.actionIndex == 0) 1 else 0
                    lastY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)


                }

            }
            else -> super.onTouchEvent(event)

        }

        return true
    }


}