package com.carbonplayer.ui.widget.helpers

import android.content.Context
import android.support.annotation.Keep
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

@Keep
class ScrolleyViewBehavior : CoordinatorLayout.Behavior<View> {

    var shown: Boolean = true

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val dependsOn = dependency is FrameLayout
        return dependsOn
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout,
                                     child: View, directTargetChild: View,
                                     target: View, nestedScrollAxes: Int,
                                     type: Int): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout,
                                   child: View, target: View, dx: Int, dy: Int,
                                   consumed: IntArray, type: Int) {
        child.postOnAnimation {
            child.offsetTopAndBottom(minOf(0, maxOf(-dy, -128)))
        }
    }

    override fun onNestedPreFling(
            coordinatorLayout: CoordinatorLayout, child: View,
            target: View, velocityX: Float, velocityY: Float): Boolean {
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
    }



    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View,
                                target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                dyUnconsumed: Int, type: Int) {

        child.postOnAnimation {
            child.offsetTopAndBottom(minOf(0, maxOf(-dyUnconsumed, -128)))
        }

    }



}