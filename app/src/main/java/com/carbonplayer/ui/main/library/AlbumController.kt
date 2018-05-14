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
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.SongListAdapter
import com.carbonplayer.utils.addToAutoDispose
import com.carbonplayer.utils.carbonAnalytics
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.logEntityEvent
import com.carbonplayer.utils.ui.ColorUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.github.florent37.glidepalette.GlidePalette
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_songgroup.view.*
import kotlinx.android.synthetic.main.songgroup_details.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Displays an album
 */
class AlbumController(
        var album: IAlbum,
        var textColor: Int,
        var mainColor: Int,
        var bodyColor: Int,
        val secondaryColor: Int,
        val secondaryTextColor: Int
) : Controller() {

    internal lateinit var root: View

    private var fabOffset: Int = 0
    private var squareHeight: Int = 0

    private var expanded = false
    private var ogHeight = 0

    private var dp56: Int = 0

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private var swatchPair: PaletteUtil.SwatchPair? = null
    private var aId: String
    private var albumProxy = album

    private lateinit var tracks: List<ITrack>
    private lateinit var sjTracks: List<SkyjamTrack>

    private lateinit var manager: MusicManager
    private lateinit var requestMgr: RequestManager

    @Suppress("unused") @Keep constructor(savedState: Bundle) : this (
            Realm.getDefaultInstance().where(Album::class.java)
                    .equalTo(Album.ID, savedState.getString("albumId")).findFirst()!!,
            savedState.getInt("textColor"),
            savedState.getInt("mainColor"),
            savedState.getInt("bodyColor"),
            savedState.getInt("secondaryColor"),
            savedState.getInt("secondaryTextColor")
    ) { this.swatchPair = swatchPair }

    @Keep constructor(album: IAlbum, swatchPair: PaletteUtil.SwatchPair) : this (
            album,
            swatchPair.primary.titleTextColor,
            swatchPair.primary.rgb,
            swatchPair.primary.bodyTextColor,
            swatchPair.secondary.rgb,
            swatchPair.secondary.bodyTextColor
    ) {
        this.swatchPair = swatchPair
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("albumId", album.albumId)
        outState.putInt("textColor", textColor)
        outState.putInt("mainColor", mainColor)
        outState.putInt("bodyColor", bodyColor)
        outState.putInt("secondaryColor", secondaryColor)
        outState.putInt("secondaryTextColor", secondaryTextColor)
        super.onSaveInstanceState(outState)
    }

    init {
        aId = album.albumId
        carbonAnalytics.logEntityEvent(FirebaseAnalytics.Event.VIEW_ITEM, album)
    }

    private fun setupRecycler(view: View, forceRemote: Boolean = false) {
        val params = view.songgroup_recycler.layoutParams
        params.height = (tracks.size * MathUtils.dpToPx2(resources, SongListAdapter.SONG_HEIGHT_DP)) +
                IdentityUtils.getNavbarHeight(resources) + (activity as MainActivity).bottomInset

        mAdapter = SongListAdapter(tracks) { (_, pos) ->
            if(album is Album && !forceRemote) manager.fromAlbum(album, pos)
            else manager.fromTracks(tracks, pos, false)
        }

        view.songgroup_recycler.adapter = mAdapter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        root = inflater.inflate(R.layout.activity_songgroup, container, false)

        dp56 = MathUtils.dpToPx(activity, 56)

        root.underlayAppbar.background = ColorDrawable(mainColor)

        root.main_backdrop.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.ONE_BY_ONE
        root.parallaxSquare.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.ONE_BY_ONE

        root.underlayAppbar.setPadding(0, IdentityUtils.getStatusBarHeight(resources), 0, 0)

        tracks = MusicLibrary.getAllAlbumTracks(album)

        if(album is Album && (album.description == null || tracks.isEmpty())) {

            Timber.d("album is Album and we're getting a SkyjamAlbum")

            Protocol.getNautilusAlbum(activity!!, album.albumId)
                    .subscribeOn(Schedulers.io())
                    .delay(300, TimeUnit.MILLISECONDS) /* Prevent animation interrupt */
                    .observeOn(AndroidSchedulers.from(this.activity!!.mainLooper))
                    .subscribe({ a ->

                        val rlm = Realm.getDefaultInstance()

                        albumProxy = rlm.where(Album::class.java)
                                .equalTo(Album.ID, aId)
                                .findFirst()!!

                        rlm.executeTransaction {
                            (albumProxy as Album).updateFrom(a, it)
                        }

                        if(tracks.isEmpty()) {
                            tracks = a.tracks ?: listOf()
                            setupRecycler(view!!, true)
                        }

                        if (view != null
                                && album.description == null
                                && albumProxy.description != null
                                && albumProxy.description!!.isNotBlank()) {
                            val const = ConstraintSet().apply {
                                clone(view!!.constraintLayout6)
                                setMargin(R.id.primaryText, ConstraintSet.TOP,
                                        MathUtils.dpToPx2(resources, 32))
                            }

                            val t = AutoTransition().apply {
                                interpolator = FastOutSlowInInterpolator()
                                duration = 250
                            }

                            TransitionManager.beginDelayedTransition(view!!.constraintLayout6, t)

                            val desc = albumProxy.description

                            const.applyTo(view!!.constraintLayout6)

                            view!!.descriptionText.run {
                                text = desc
                                setTextColor(bodyColor)
                                visibility = View.VISIBLE
                                alpha = 0.0f
                                animate().alpha(1f).setDuration(250).start()
                            }
                        }
                    }, {
                        err -> Timber.e(err)
                    }).addToAutoDispose()
        }

        manager = MusicManager(activity as MainActivity)

        Timber.d("album %s", album.albumId)

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
            (activity as? MainActivity)?.showAlbumPopup(v, album)
        }

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

        requestMgr = Glide.with(activity as Activity)

        requestMgr.load(album.albumArtRef)
                .apply(RequestOptions.overrideOf(preImageWidth, preImageWidth)
                                .diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate())
                .apply { if(swatchPair == PaletteUtil.DEFAULT_SWATCH_PAIR) {
                    listener(GlidePalette.with(album.albumArtRef)
                            .use(0)
                            .intoCallBack({ palette -> if (palette != null) {
                                val pair = PaletteUtil.getSwatches(activity!!, palette)

                                PaletteUtil.crossfadeBackground(
                                        root.constraintLayout6, pair.primary)
                                PaletteUtil.crossfadeTitle(
                                        root.primaryText, pair.primary, false)
                                PaletteUtil.crossfadeSubtitle(root.secondaryText, pair.primary,
                                        false)

                                if(ColorUtils.isDark(pair.primary.bodyTextColor)) {
                                    root.overflowButton.imageTintList =
                                            CarbonPlayerApplication.instance.darkCSL
                                }

                                textColor = pair.primary.titleTextColor
                                bodyColor = pair.primary.bodyTextColor
                                mainColor = pair.primary.rgb

                                root.downloadButton.imageTintList =
                                        ColorStateList.valueOf(textColor)
                                root.overflowButton.imageTintList =
                                        ColorStateList.valueOf(textColor)
                                root.expandDescriptionChevron.imageTintList =
                                        ColorStateList.valueOf(bodyColor)

                                root.songgroup_grad.background =
                                        GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                        intArrayOf(mainColor,
                                                ColorUtils.modifyAlpha(mainColor, 200),
                                                ColorUtils.modifyAlpha(mainColor, 0)))

                                root.play_fab.backgroundTintList =
                                        ColorStateList.valueOf(pair.secondary.rgb)
                                root.play_fab.imageTintList =
                                        ColorStateList.valueOf(pair.secondary.bodyTextColor)
                            } })
                    )
                } }.into(root.main_backdrop)

        if(album.description != null && album.description!!.isNotBlank()) {

            val const = ConstraintSet().apply {
                clone(root.constraintLayout6)
                setMargin(R.id.primaryText, ConstraintSet.TOP, MathUtils.dpToPx2(resources, 32))
            }

            const.applyTo(root.constraintLayout6)

            root.descriptionText.run {
                text = album.description
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


        root.secondaryText.text = if (album is Album) (album as Album).artists?.firstOrNull()?.name ?:
                album.albumArtist else album.albumArtist
        root.primaryText.text = album.name

        root.songgroup_recycler.isNestedScrollingEnabled = false

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(activity)

        root.songgroup_recycler.layoutManager = mLayoutManager

        if(album is SkyjamAlbum && tracks.isEmpty()) {
            Timber.d("album is SkyjamAlbum and we're getting a SkyjamAlbum")
            Protocol.getNautilusAlbum(root.context, album.albumId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.from(this.activity!!.mainLooper))
                    .subscribe({ a -> a.tracks?.let {
                            tracks = it
                            setupRecycler(root)
                        }})
        } else if(tracks.isNotEmpty()) setupRecycler(root)

        root.play_fab.setOnClickListener {
            if(album is Album) manager.fromAlbum(album, 0)
            else manager.fromTracks(tracks, 0, false)

            carbonAnalytics.logEntityEvent("play_fab", album)
        }

        root.expandDescriptionChevron.setOnClickListener {

            if(!expanded) ogHeight = root.constraintLayout6.measuredHeight

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

        if(root.main_backdrop.height <= dp56 && root.underlayAppbar.visibility == View.GONE) {
            root.underlayAppbar.visibility = View.VISIBLE
            root.underlayAppbar.alpha = 0.0f
            root.underlayAppbar.animate()
                    .alpha(1.0f).setDuration(300)
                    .start()
        } else if(root.main_backdrop.height > dp56
                && root.underlayAppbar.visibility == View.VISIBLE) {
            root.underlayAppbar.animate()
                    .alpha(0.0f).setDuration(300).withEndAction {
                        root.underlayAppbar.visibility = View.GONE
                    }
                    .start()
        }
    }

    override fun onDestroyView(view: View) {
        requestMgr.clear(view.main_backdrop)
        super.onDestroyView(view)
    }
}