package com.carbonplayer.ui.main.library

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.ui.main.adapters.SongListAdapter
import com.carbonplayer.ui.widget.ParallaxScrimageView
import com.carbonplayer.ui.widget.SizeFrameLayout
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView
import kotlinx.android.synthetic.main.grid_item_layout.view.*


class AlbumController(
        val albumId: String
) {
    lateinit var oLayoutRoot: FrameLayout
    lateinit var newImg: ParallaxScrimageView
    lateinit var newScroll: ObservableScrollView
    lateinit var newContainer: FrameLayout
    lateinit var newLinear: LinearLayout
    lateinit var newLinear2: LinearLayout
    lateinit var newContent: SizeFrameLayout
    lateinit var newOTitle: TextView
    lateinit var newOSubtitle: TextView
    lateinit var newTitle: TextView
    lateinit var newSubtitle: TextView
    lateinit var newRecycler: RecyclerView

    fun makeAlbum(context: Context, viewRoot: ViewGroup, gridLayoutRoot: FrameLayout) {
        val handler = Handler(context.mainLooper)

        oLayoutRoot = gridLayoutRoot
        val screenWidth = IdentityUtils.displayWidth2(context)
        val oImg = gridLayoutRoot.imgthumb
        val oContent = gridLayoutRoot.gridLayoutContentRoot

        newImg = ParallaxScrimageView(context).apply {
            setParallaxFactor(-0.5f)
        }
        newImg.setImageDrawable(oImg.drawable)
        gridLayoutRoot.postOnAnimation({
            gridLayoutRoot.alpha = 0f
        })

        viewRoot.addView(newImg)
        newScroll = ObservableScrollView(context)
        newContainer = FrameLayout(context)
        newContent = SizeFrameLayout(context)
        newLinear = LinearLayout(context)
        newLinear2 = LinearLayout(context)
        newRecycler = RecyclerView(context)
        newOTitle = TextView(context)
        newOSubtitle = TextView(context)
        newTitle = TextView(context)
        newSubtitle = TextView(context)

        val t = SpannableString(oContent.primaryText.text)
                .apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0,
                            oContent.primaryText.text.length,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
                }

        val t2 = SpannableString(oContent.detailText.text)
                .apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0,
                            oContent.detailText.text.length,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
                }

        val ogPos = intArrayOf(0, 0)
        val ogContentPos = intArrayOf(0, 0)
        oImg.getLocationInWindow(ogPos)
        oContent.getLocationInWindow(ogContentPos)

        newOTitle.text = t
        newOSubtitle.text = t2

        newTitle.text = oContent.detailText.text
        newSubtitle.text = oContent.primaryText.text

        newContent.background = oContent.background

        newContainer.addView(newContent)

        newTitle.textSize = 36f
        newSubtitle.textSize = 20f

        newLinear.orientation = LinearLayout.VERTICAL
        newLinear2.orientation = LinearLayout.VERTICAL

        val tracks = MusicLibrary.getInstance().getAllAlbumTracks(albumId)

        //height = tracks.size * MathUtils.dpToPx2(context.resources, 67)

        newRecycler.adapter = SongListAdapter(tracks) { (id, clientID, nautilusID) ->
            //manager.fromAlbum(album.id)
        }

        newContainer.addView(newRecycler)

        newRecycler.layoutParams = (newRecycler.layoutParams as FrameLayout.LayoutParams).apply {
            height = MathUtils.dpToPx2(context.resources, 60).toInt() * tracks.size
            topMargin = screenWidth + MathUtils.dpToPx2(context.resources, 150).toInt()
        }

        newRecycler.background = ColorDrawable(Color.WHITE)

        newLinear.addView(newTitle)
        newLinear2.addView(newSubtitle)
        newContainer.addView(newLinear)
        //newContainer.addView(newRecycler)
        newContainer.addView(newLinear2)

        newLinear.layoutParams = (newLinear.layoutParams as FrameLayout.LayoutParams).apply {
            width = FrameLayout.LayoutParams.MATCH_PARENT
            height = FrameLayout.LayoutParams.WRAP_CONTENT
            topMargin = screenWidth
        }

        newLinear2.layoutParams = (newLinear.layoutParams as FrameLayout.LayoutParams).apply {
            width = FrameLayout.LayoutParams.MATCH_PARENT
            height = FrameLayout.LayoutParams.WRAP_CONTENT
            topMargin = screenWidth
        }

        newTitle.layoutParams = (newTitle.layoutParams as LinearLayout.LayoutParams).apply {
            width = FrameLayout.LayoutParams.WRAP_CONTENT
            height = FrameLayout.LayoutParams.WRAP_CONTENT
            leftMargin = MathUtils.dpToPx2(context.resources, 42).toInt()
            topMargin = MathUtils.dpToPx2(context.resources, 30).toInt()
            bottomMargin = MathUtils.dpToPx2(context.resources, 40).toInt()
        }

        newSubtitle.layoutParams = (newSubtitle.layoutParams as LinearLayout.LayoutParams).apply {
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            height = LinearLayout.LayoutParams.WRAP_CONTENT
            leftMargin = MathUtils.dpToPx2(context.resources, 42).toInt()
            topMargin = MathUtils.dpToPx2(context.resources, 90).toInt()
        }

        newTitle.alpha = 0.0f
        newSubtitle.alpha = 0.0f

        newScroll.addView(newContainer)

        (newContainer.layoutParams as FrameLayout.LayoutParams).width =
                FrameLayout.LayoutParams.MATCH_PARENT
        (newContainer.layoutParams as FrameLayout.LayoutParams).height =
                FrameLayout.LayoutParams.MATCH_PARENT

        newContent.layoutParams.height = oContent.measuredHeight

        viewRoot.addView(newScroll)

        (newScroll.layoutParams as FrameLayout.LayoutParams).width =
                FrameLayout.LayoutParams.MATCH_PARENT
        (newScroll.layoutParams as FrameLayout.LayoutParams).height =
                FrameLayout.LayoutParams.MATCH_PARENT

        (newContent.layoutParams as FrameLayout.LayoutParams).topMargin = ogContentPos[1]
        (newContent.layoutParams as FrameLayout.LayoutParams).leftMargin = ogContentPos[0]

        (newImg.layoutParams as FrameLayout.LayoutParams).run {
            topMargin = ogPos[1]
            leftMargin = ogPos[0]
            width = oImg.measuredWidth
            height = oImg.measuredHeight
        }

        viewRoot.addView(newOTitle)
        viewRoot.addView(newOSubtitle)

        val ogPrimaryPos = intArrayOf(0, 0)
        val ogSecondaryPos = intArrayOf(0, 0)
        oContent.primaryText.getLocationInWindow(ogPrimaryPos)
        oContent.detailText.getLocationInWindow(ogSecondaryPos)

        newOTitle.layoutParams = (newOTitle.layoutParams as FrameLayout.LayoutParams).apply {
            width = oContent.primaryText.measuredWidth
            height = FrameLayout.LayoutParams.WRAP_CONTENT
            leftMargin = ogPrimaryPos[0]
            topMargin = ogPrimaryPos[1]
        }
        newOSubtitle.layoutParams = (newOSubtitle.layoutParams as FrameLayout.LayoutParams).apply {
            width = oContent.detailText.measuredWidth
            height = FrameLayout.LayoutParams.WRAP_CONTENT
            leftMargin = ogSecondaryPos[0]
            topMargin = ogSecondaryPos[1]
        }

        handler.postDelayed({

            newImg.animate()
                    .x(0.0f)
                    .y(0.0f)
                    .setDuration(270)
                    .setInterpolator(FastOutSlowInInterpolator()).start()

            newContent.animate()
                    .x(0.0f)
                    .y(screenWidth.toFloat())
                    .setDuration(270)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()

            val widthAnim = ValueAnimator.ofInt(oContent.measuredWidth, screenWidth)
            widthAnim.addUpdateListener { valueAnimator ->
                val animVal = valueAnimator.animatedValue as Int
                newImg.postOnAnimation({
                    newImg.layoutParams.width = animVal
                    newImg.requestLayout()
                })
                newContent.postOnAnimation({
                    val lp = newContent.layoutParams
                    lp.width = animVal
                    newContent.layoutParams = lp
                    newContent.measure(0, 0)
                    newContent.requestLayout()
                    //newScroll.requestLayout()
                })
            }

            widthAnim.interpolator = FastOutSlowInInterpolator()
            widthAnim.duration = 270
            widthAnim.start()

            newOTitle.animate()
                    .alpha(0.0f)
                    .setDuration(270)
                    .start()

            newOSubtitle.animate()
                    .alpha(0.0f)
                    .setDuration(270)
                    .start()

        }, 40)

        handler.postDelayed({

            newTitle.animate().alpha(1.0f).setDuration(130).start()
            newSubtitle.animate().alpha(1.0f).setDuration(130).start()

            val heightAnim = ValueAnimator.ofInt(oContent.measuredHeight,
                    MathUtils.dpToPx2(context.resources, 150).toInt())

            heightAnim.addUpdateListener { valueAnimator ->
                val animVal = valueAnimator.animatedValue as Int
                newContent.postOnAnimation({
                    newContent.layoutParams.height = animVal
                    newContent.measure(0, 0)
                    newContent.requestLayout()
                    newContainer.requestLayout()
                })
            }

            heightAnim.interpolator = FastOutSlowInInterpolator()
            heightAnim.duration = 320
            heightAnim.start()

        }, 140)

    }
}