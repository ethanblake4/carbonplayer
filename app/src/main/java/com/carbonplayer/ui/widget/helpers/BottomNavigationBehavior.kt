package com.carbonplayer.ui.widget.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.carbonplayer.ui.helpers.NowPlayingHelper
import kotlinx.android.synthetic.main.controller_main.view.*
import kotlinx.android.synthetic.main.nowplaying.view.*

@Keep
class BottomNavigationBehavior : CoordinatorLayout.Behavior<View> {

    @Keep
    constructor() : super()

    @Keep
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val dependsOn = dependency is FrameLayout
        return dependsOn
    }

    @Suppress("OverridingDeprecatedMember")
    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout,
                                     child: View, directTargetChild: View,
                                     target: View, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    @Suppress("OverridingDeprecatedMember")
    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout,
                                   child: View, target: View, dx: Int, dy: Int,
                                   consumed: IntArray) {
        if (dy < 0) {
            showBottomNavigationView(child)
        } else if (dy > 0) {
            hideBottomNavigationView(child)
        }
    }

    private fun hideBottomNavigationView(view: View) {

        val height = if(view.nowplaying_frame.visibility == View.VISIBLE) view.bottom_nav.height
                        else view.height



        view.animate().translationY(height.toFloat()).interpolator = FastOutSlowInInterpolator()
        view.bottom_nav.animate().alpha(0.0f).withEndAction {
            NowPlayingHelper.bottomBarIsUp = false
        }.interpolator = FastOutSlowInInterpolator()

    }

    private fun showBottomNavigationView(view: View) {
        view.animate().translationY(0f).interpolator = FastOutSlowInInterpolator()
        view.bottom_nav.animate().withEndAction {
            NowPlayingHelper.bottomBarIsUp = true
        }.alpha(1.0f)
    }
}