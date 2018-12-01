package com.carbonplayer.ui.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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

            if(from == null && to == null) return@apply

            val builder =
                    if (from != null) {
                        play(ObjectAnimator.ofFloat(
                            from, View.SCALE_X, 1f, if(isPush) 1.35f else 0.5f))
                    } else play(ObjectAnimator.ofFloat(
                            to, View.SCALE_X, if(isPush) 0.5f else 1.35f, 1f))

            if(from != null) {
                if(to != null) builder.with(ObjectAnimator.ofFloat(
                        to, View.SCALE_X, if(isPush) 0.5f else 1.35f, 1f))
                builder.with(ObjectAnimator.ofFloat(
                        from, View.SCALE_Y, 1f, if(isPush) 1.35f else 0.5f))
                builder.with(ObjectAnimator.ofFloat(from, View.ALPHA, 1f, 0f))
            }
            if(to != null) {
                builder.with(ObjectAnimator.ofFloat(to, View.SCALE_Y, if(isPush) 0.5f else 1.35f, 1f))
                builder.with(ObjectAnimator.ofFloat(to, View.ALPHA, 0f, 1f))
            }
            interpolator = FastOutSlowInInterpolator()
        }
    }
}