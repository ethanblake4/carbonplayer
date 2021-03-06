package com.carbonplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Scroller
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.carbonplayer.utils.carbonAnalytics
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import timber.log.Timber

/* A recyclerview that allows to be swiped up and down. If it is swiped up,
    it must be scrolled to the top before it can be scrolled down.
 */
class NowPlayingQueueView: RecyclerView {

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastY = 0f
    private var last2Y = 0f
    private var eventStartedOnDragHandle = false
    var initialY = -2f
    private val scroller = Scroller(context, FastOutSlowInInterpolator())
    private val dragHandleMaxX: Int
    var scrollHasControl = false
    private val maxY = IdentityUtils.getStatusBarHeight(resources)
    private var eventStartTime = 0L
    private var eventInitialY = 0f
    private var eventHasMotion = false
    var isUp = false
    var lastUpFraction = 0f

    private var runThread = true
    private lateinit var runner: Runnable
    var callback: ((Float) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, def: Int) : super(context, attrs, def)

    init {
        clipToPadding = false
        setPadding(0, 0, 0, maxY)
        dragHandleMaxX = MathUtils.dpToPx2(resources, 40)
        runner = Runnable {
            if(runThread) {
                scroller.computeScrollOffset()
                if(scrollHasControl && scroller.currY.toFloat() != translationY) {
                    translationY = scroller.currY.toFloat()
                    val upFraction = ((initialY - scroller.currY) / (
                            initialY - maxY))
                    lastUpFraction = upFraction

                    callback?.invoke(upFraction)
                    val prevWasUp = isUp
                    isUp = upFraction > 0.99f

                    if(prevWasUp != isUp) carbonAnalytics.logEvent(
                            if(isUp) "open_queue" else "close_queue", null)

                    //Timber.d("scroller: %d", scroller.currY)
                }
                post(runner)
            }
        }
        post(runner)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        //Timber.d("On touch event: %d", event?.actionMasked)

        when(event?.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                /* On a touch, set the current active pointer and remove control from
                 * the scroller */

                super.onTouchEvent(event)
                scrollHasControl = false
                val ac = event.actionIndex
                eventStartedOnDragHandle = event.rawX < dragHandleMaxX
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
                    if(eventStartedOnDragHandle){
                        super.onTouchEvent(event)
                    } else if(isUp && event.rawY < lastY) {
                        super.onTouchEvent(event)
                    } else if (isUp && computeVerticalScrollOffset() > 0) {
                        super.onTouchEvent(event)
                    } else {
                        translationY = Math.min(translationY + dy, initialY)

                        val upFrac = ((initialY - translationY) / (
                                initialY - maxY))
                        callback?.invoke(upFrac)
                        isUp = upFrac > 0.99f
                        lastUpFraction = upFrac

                        last2Y = lastY
                        lastY = event.rawY
                    }
                } else if (last2Y == lastY){
                    super.onTouchEvent(event)
                }

            }
            MotionEvent.ACTION_UP -> {

                /* On an up event:
                *   - If there was motion, start a fling with the current velocity
                *   - If there was no motion, start a scroll to the nearest position OR
                 *  - If the motion was a tap and isUp is FALSE, scroll to the top */

                val location = intArrayOf(0, 0)

                getLocationInWindow(location)

                if(eventStartedOnDragHandle){
                    super.onTouchEvent(event)
                } else if(isUp && event.rawY < lastY) {
                    super.onTouchEvent(event)
                } else if (isUp && computeVerticalScrollOffset() > 0) {
                    super.onTouchEvent(event)
                } else if(true) {

                    Timber.d("Fling from startY: %d, velocity: %d, endY: %d",
                            location[1], (lastY - last2Y).toInt(),
                            maxY)

                    val velocity = (lastY - last2Y).toInt()
                    val curY = location[1]

                    if (velocity > 10) {
                        scroller.fling(0, curY, 0,
                                velocity, 0, 0, 0, initialY.toInt())
                        scroller.finalY = initialY.toInt()
                    } else if (velocity < -10) {
                        scroller.fling(0, curY, 0,
                                velocity, 0, 0, 0, initialY.toInt())
                        scroller.finalY = maxY
                    } else {
                        if (curY > initialY / 2) {
                            scroller.fling(0, curY, 0,
                                    velocity, 0, 0, 0, initialY.toInt())
                            scroller.finalY = initialY.toInt()
                        } else {
                            scroller.fling(0, curY, 0,
                                    velocity, 0, 0, 0, initialY.toInt())
                            scroller.finalY = maxY
                        }
                    }

                    scroller.extendDuration(400)

                    scrollHasControl = true
                    eventHasMotion = false

                } else if( scroller.isFinished ){
                    activePointerId = MotionEvent.INVALID_POINTER_ID

                    /*if (location[1] <= initialY + 12) {
                        scroller.startScroll(0, initialY.toInt(), 0, maxY
                                - initialY.toInt(), 400)
                        scrollHasControl = true
                    } else {*/
                        super.onTouchEvent(event)
                    /*}*/
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                super.onTouchEvent(event)
                activePointerId = MotionEvent.INVALID_POINTER_ID
                eventStartedOnDragHandle = false
            }

            MotionEvent.ACTION_POINTER_UP -> {
                eventStartedOnDragHandle = false
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