package com.carbonplayer.ui.widget.helpers

import android.support.v4.view.ViewCompat.animate
import android.R.attr.translationY
import android.content.Context
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.controller_main.view.*
import kotlinx.android.synthetic.main.nowplaying.view.*


class BottomNavigationBehavior : CoordinatorLayout.Behavior<View> {

    constructor() : super() {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val dependsOn = dependency is FrameLayout
        return dependsOn
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (dy < 0) {
            showBottomNavigationView(child)
        } else if (dy > 0) {
            hideBottomNavigationView(child)
        }
    }

    private fun hideBottomNavigationView(view: View) {

        view.animate().translationY(view.bottom_nav.height.toFloat()).interpolator = FastOutSlowInInterpolator()
        view.bottom_nav.animate().alpha(0.0f).interpolator = FastOutSlowInInterpolator()

    }

    private fun showBottomNavigationView(view: View) {
        view.animate().translationY(0f).interpolator = FastOutSlowInInterpolator()
        view.bottom_nav.animate().alpha(1.0f)
    }
}