package com.carbonplayer.ui.main.library

import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.support.annotation.Keep
import android.support.constraint.ConstraintSet
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.ScaleAnimation
import android.widget.Toast
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.R
import com.carbonplayer.model.entity.Artist
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.SongListAdapter
import com.carbonplayer.utils.addToAutoDispose
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.ui.ColorUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_songgroup.view.*
import kotlinx.android.synthetic.main.nowplaying.*
import kotlinx.android.synthetic.main.songgroup_details.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Displays an album
 */
class ArtistController(
        var artist: Artist,
        val textColor: Int,
        val mainColor: Int,
        val bodyColor: Int,
        val secondaryColor: Int,
        val secondaryTextColor: Int
) : Controller() {

    internal lateinit var root: View

    private var fabOffset: Int = 0
    private var squareHeight: Int = 0

    private var artistProxy = artist
    private val aId = artist.artistId

    private var expanded = false
    private var ogHeight = 0

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private lateinit var manager: MusicManager
    private lateinit var requestMgr: RequestManager

    @Suppress("unused") @Keep constructor(savedState: Bundle) : this (
            Realm.getDefaultInstance().where(Artist::class.java)
                    .equalTo(Artist.ID, savedState.getString("artistId")).findFirst()!!,
            savedState.getInt("textColor"),
            savedState.getInt("mainColor"),
            savedState.getInt("bodyColor"),
            savedState.getInt("secondaryColor"),
            savedState.getInt("secondaryTextColor")
    )

    @Keep constructor(artist: Artist, swatchPair: PaletteUtil.SwatchPair) : this (
            artist,
            swatchPair.primary.titleTextColor,
            swatchPair.primary.rgb,
            swatchPair.primary.bodyTextColor,
            swatchPair.secondary.rgb,
            swatchPair.secondary.bodyTextColor
    )

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("artistId", artist.artistId)
        outState.putInt("textColor", textColor)
        outState.putInt("mainColor", mainColor)
        outState.putInt("bodyColor", bodyColor)
        outState.putInt("secondaryColor", secondaryColor)
        outState.putInt("secondaryTextColor", secondaryTextColor)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        root = inflater.inflate(R.layout.activity_songgroup, container, false)

        if(artist.artistBio == null) {
            Protocol.getNautilusArtist(activity!!, artist.artistId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .delay(300, TimeUnit.MILLISECONDS) /* Prevent animation interrupt */
                    .subscribe({ a ->

                        val rlm = Realm.getDefaultInstance()

                        artistProxy = rlm.where(Artist::class.java)
                                .equalTo(Artist.ID, aId)
                                .findFirst()!!

                        rlm.executeTransaction {
                            artistProxy.updateFrom(a, it)
                        }

                        if(view != null && artistProxy.artistBio != null && artistProxy.artistBio!!.isNotBlank()) {
                            val const = ConstraintSet().apply {
                                clone(view!!.constraintLayout6)
                                setMargin(R.id.primaryText, ConstraintSet.TOP,
                                        MathUtils.dpToPx2(resources, 32))
                            }

                            val t = AutoTransition().apply {
                                interpolator = FastOutSlowInInterpolator()
                                duration = 250
                            }

                            val bioText = artistProxy.artistBio

                            view!!.post {
                                TransitionManager.beginDelayedTransition(view!!.constraintLayout6, t)
                                const.applyTo(view!!.constraintLayout6)

                                view!!.descriptionText.run {
                                    text = bioText
                                    setTextColor(bodyColor)
                                    visibility = View.VISIBLE
                                    alpha = 0.0f
                                    animate().alpha(1f).setDuration(250).start()
                                }
                            }

                        }

                        if(view != null && artistProxy.topTracks.isNotEmpty()) {

                            val tpTracks = artistProxy.topTracks.toList()

                            view!!.post {
                                val params = root.songgroup_recycler.layoutParams
                                params.height = (tpTracks.take(5).size * MathUtils.dpToPx2(resources, 59)) +
                                        IdentityUtils.getNavbarHeight(resources) +
                                        MathUtils.dpToPx2(resources,
                                                if ((activity as MainActivity).nowplaying_frame.visibility == View.VISIBLE)
                                                    56 else 0)

                            }

                            this.activity!!.runOnUiThread {
                                mAdapter = SongListAdapter(artist.topTracks.take(5), {
                                    Timber.d("clicked ${it.second}")
                                })

                                // use a linear layout manager
                                mLayoutManager = LinearLayoutManager(activity)

                                root.songgroup_recycler.layoutManager = mLayoutManager

                                root.songgroup_recycler.adapter = mAdapter
                            }
                        }
                    }, {
                        err -> Timber.e(err)
                    }).addToAutoDispose()
        }

        manager = MusicManager(activity as MainActivity)

        Timber.d("artist ${artist.artistId}")

        root.primaryText.setTextColor(textColor)
        root.secondaryText.setTextColor(textColor)
        root.constraintLayout6.background = ColorDrawable(mainColor)
        root.songgroup_grad.background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(mainColor, ColorUtils.modifyAlpha(mainColor, 200),
                        ColorUtils.modifyAlpha(mainColor, 0)))

        root.downloadButton.imageTintList = ColorStateList.valueOf(textColor)
        root.overflowButton.imageTintList = ColorStateList.valueOf(textColor)
        root.expandDescriptionChevron.imageTintList = ColorStateList.valueOf(bodyColor)

        root.play_fab.backgroundTintList = ColorStateList.valueOf(secondaryColor)
        root.play_fab.imageTintList = ColorStateList.valueOf(secondaryTextColor)

        root.downloadButton.setOnClickListener {
            activity?.let { Toast.makeText(it, "Downloading is not supported yet", Toast.LENGTH_LONG).show() }
        }
        root.overflowButton.setOnClickListener { v ->
            //(activity as? MainActivity)?.showAlbumPopup(v, artist)
        }

        root.main_backdrop.transitionName = artist.artistId + "i"
        root.constraintLayout6.transitionName = artist.artistId + "cr"
        root.primaryText.transitionName = artist.artistId + "t"
        root.secondaryText.transitionName = artist.artistId + "d"

        root.songgroup_scrollview.setScrollViewCallbacks(object : ObservableScrollViewCallbacks {
            override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {}

            override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
                root.main_backdrop.offset = (-scrollY).toFloat()
            }

            override fun onDownMotionEvent() {}
        })

        root.albumLayoutRoot.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        squareHeight = root.main_backdrop.height
        fabOffset = MathUtils.dpToPx(activity, 28)

        val preImageWidth = IdentityUtils.displayWidthDp(activity as Activity) - 4

        //val handler = DetailSharedElementEnterCallback(this)
        //handler.addTextViewSizeResource(root.primaryText,
        //        R.dimen.small_text_size, R.dimen.large_text_size)
        //handler.addTextViewSizeResource(root.secondaryText,
        //        R.dimen.small_text_2, R.dimen.large_text_2)

        if(artist.topTracks.isNotEmpty()) {
            val params = root.songgroup_recycler.layoutParams
            params.height = (artist.topTracks.take(5).size * MathUtils.dpToPx2(resources, 59)) +
                    IdentityUtils.getNavbarHeight(resources) +
                    MathUtils.dpToPx2(resources,
                            if ((activity as MainActivity).nowplaying_frame.visibility == View.VISIBLE)
                                56 else 0)

            mAdapter = SongListAdapter(artist.topTracks.take(5), {
                Timber.d("clicked ${it.second}")
            })

            // use a linear layout manager
            mLayoutManager = LinearLayoutManager(activity)

            root.songgroup_recycler.layoutManager = mLayoutManager

            root.songgroup_recycler.adapter = mAdapter
        }

        requestMgr = Glide.with(activity as Activity)

        artist.artistArtRefs.first()?.let {
            requestMgr.load(it.url)
                    .apply(
                            RequestOptions.overrideOf(preImageWidth, preImageWidth)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate())
                    .into(root.main_backdrop)
        }


        if(!artist.artistBio.isNullOrBlank()) {

            val const = ConstraintSet().apply {
                clone(root.constraintLayout6)
                setMargin(R.id.primaryText, ConstraintSet.TOP, MathUtils.dpToPx2(resources, 32))
            }

            const.applyTo(root.constraintLayout6)

            root.descriptionText.run {
                text = artist.artistBio
                setTextColor(bodyColor)
                visibility = View.VISIBLE
                alpha = 1.0f
            }
        }

        root.play_fab.visibility = View.INVISIBLE
        Handler().postDelayed({
            root.play_fab.visibility = View.VISIBLE
            val anim = ScaleAnimation(0f, 1f, 0f, 1f, root.play_fab.pivotX,
                    (root.main_backdrop.height - fabOffset).toFloat())
            anim.fillAfter = true
            anim.duration = 310
            anim.interpolator = FastOutSlowInInterpolator()
            root.play_fab.startAnimation(anim)
        }, 600)


        root.primaryText.text = artist.name

        root.songgroup_recycler.isNestedScrollingEnabled = false



        root.expandDescriptionChevron.setOnClickListener {

            ogHeight = root.constraintLayout6.measuredHeight

            val anim = ValueAnimator.ofInt(
                    root.constraintLayout6.measuredHeight,
                    if(!expanded) root.constraintLayout6.measuredHeight + root.descriptionText.height
                    else ogHeight
            )
            anim.addUpdateListener({
                val int = it.animatedValue as Int
                val layoutParams = root.constraintLayout6.layoutParams
                layoutParams.height = int
                root.constraintLayout6.layoutParams = layoutParams
            })
            anim.duration = 300
            anim.interpolator = FastOutSlowInInterpolator()
            anim.start()

            expanded = !expanded
        }

        return root
    }

    fun setTransformedTextPosition(transform: Int) {
        root.secondaryText.layout(root.secondaryText.left, root.secondaryText.top + transform,
                root.secondaryText.right, root.secondaryText.bottom + transform)
    }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        root.play_fab.y = (root.main_backdrop.height - fabOffset).toFloat()
    }

    override fun onDestroyView(view: View) {
        requestMgr.clear(view.main_backdrop)
        super.onDestroyView(view)
    }
}
