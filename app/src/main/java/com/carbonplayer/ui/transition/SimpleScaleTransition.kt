package com.carbonplayer.ui.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.support.annotation.Keep
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler
import com.carbonplayer.CarbonPlayerApplication

class SimpleScaleTransition (
        val context: Context//,
        /*val albumId: String*/
) : AnimatorChangeHandler() {

    @Suppress("unused") @Keep constructor() : this(CarbonPlayerApplication.instance)

    override fun resetFromView(from: View) {
        from.alpha = 1f
    }

    override fun getAnimator(container: ViewGroup, from: View?, to: View?, isPush: Boolean,
                             toAddedToContainer: Boolean): Animator {
        return AnimatorSet().apply {
            playTogether(
                    ObjectAnimator.ofFloat(from, View.SCALE_X, 1f, if(isPush) 1.35f else 0.5f),
                    ObjectAnimator.ofFloat(from, View.SCALE_Y, 1f, if(isPush) 1.35f else 0.5f),
                    ObjectAnimator.ofFloat(from, View.ALPHA, 1f, 0f),
                    ObjectAnimator.ofFloat(to, View.SCALE_X, if(isPush) 0.5f else 1.35f, 1f),
                    ObjectAnimator.ofFloat(to, View.SCALE_Y, if(isPush) 0.5f else 1.35f, 1f),
                    ObjectAnimator.ofFloat(to, View.ALPHA, 0f, 1f)
            )
            interpolator = FastOutSlowInInterpolator()
        }


    }
}