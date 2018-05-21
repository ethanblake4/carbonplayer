package com.carbonplayer.ui.main.library

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.support.annotation.Keep
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.ScaleAnimation
import android.widget.Toast
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Playlist
import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.LightListSongAdapter
import com.carbonplayer.utils.addToAutoDispose
import com.carbonplayer.utils.carbonAnalytics
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.logEntityEvent
import com.carbonplayer.utils.ui.ColorUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_songgroup.view.*
import kotlinx.android.synthetic.main.songgroup_details.view.*
import org.parceler.Parcels
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Displays an album
 */
class PlaylistController(
        var drawable: Observable<Drawable>?,
        var playlist: IPlaylist,
        var futurePlaylist: Observable<SkyjamPlaylist>?,
        val entries: Observable<List<ITrack>>,
        var textColor: Int,
        var mainColor: Int,
        var bodyColor: Int,
        var secondaryColor: Int,
        var secondaryTextColor: Int
) : Controller() {

    internal lateinit var root: View

    private var fabOffset: Int = 0
    private var squareHeight: Int = 0

    private lateinit var playlistProxy: Playlist
    private lateinit var realPlaylist: IPlaylist
    //private val aId = artist

    private var expanded = false
    private var ogHeight = 0

    private var swatchPair: PaletteUtil.SwatchPair? = null

    private lateinit var manager: MusicManager
    private lateinit var requestMgr: RequestManager

    @Suppress("unused") @Keep constructor(savedState: Bundle) : this (
            null,
            if(savedState.containsKey("id")) Realm.getDefaultInstance().where(Playlist::class.java)
                    .equalTo(Playlist.REMOTE_ID, savedState.getString("id")).findFirst()!!
            else Parcels.unwrap(savedState.getParcelable("playlist")),
            null,
            Observable.fromIterable(Parcels.unwrap(savedState.getParcelable("plentries"))),
            savedState.getInt("textColor"),
            savedState.getInt("mainColor"),
            savedState.getInt("bodyColor"),
            savedState.getInt("secondaryColor"),
            savedState.getInt("secondaryTextColor")
    )

    @Keep constructor(context: Context, drawable: Drawable?,
                      playlist: IPlaylist, swatchPair: PaletteUtil.SwatchPair) : this (
            drawable?.let { Observable.just(it) },
            playlist,
            null,
            MusicLibrary.loadPlentries(context, playlist),
            swatchPair.primary.titleTextColor,
            swatchPair.primary.rgb,
            swatchPair.primary.bodyTextColor,
            swatchPair.secondary.rgb,
            swatchPair.secondary.bodyTextColor
    ) {
        this.swatchPair = swatchPair
    }

    @Keep constructor(context: Context, playlist: IPlaylist, futurePlaylist: Observable<SkyjamPlaylist>,
                      swatchPair: PaletteUtil.SwatchPair) : this (
            null,
            playlist,
            futurePlaylist,
            MusicLibrary.loadPlentries(context, playlist),
            swatchPair.primary.titleTextColor,
            swatchPair.primary.rgb,
            swatchPair.primary.bodyTextColor,
            swatchPair.secondary.rgb,
            swatchPair.secondary.bodyTextColor
    ) {
        this.swatchPair = swatchPair
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("id", realPlaylist.id)
        outState.putInt("textColor", textColor)
        outState.putInt("mainColor", mainColor)
        outState.putInt("bodyColor", bodyColor)
        outState.putInt("secondaryColor", secondaryColor)
        outState.putInt("secondaryTextColor", secondaryTextColor)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        manager = MusicManager(activity as MainActivity)
        requestMgr = Glide.with(activity as Activity)

        root = inflater.inflate(R.layout.activity_songgroup, container, false)

        root.main_backdrop.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.ONE_BY_ONE
        root.parallaxSquare.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.ONE_BY_ONE

        futurePlaylist?.subscribe {
            realPlaylist = it
            setupView(root, true)
        }

        entries.subscribe {

            root.descriptionText.text = it.size.toString().plus(" songs")

            Timber.d("Recieved entries")
            root.songgroup_recycler.layoutManager = LinearLayoutManager(activity)
            root.songgroup_recycler.adapter =
                    LightListSongAdapter(it, { track, pos ->
                        manager.fromTracks(it, pos, playlist is Playlist)
                    }, { view, track ->
                        (activity as? MainActivity)?.showTrackPopup(view, track)
                    })
            root.songgroup_recycler.layoutParams.height =
                    (it.size * MathUtils.dpToPx2(resources, LightListSongAdapter.SONG_HEIGHT_DP)) +
                    IdentityUtils.getNavbarHeight(resources) + (activity as MainActivity).bottomInset
        }

        drawable?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { root.main_backdrop.setImageDrawable(it) }

        realPlaylist = playlist
        setupView(root, false)

        root.songgroup_recycler.isNestedScrollingEnabled = false

        /*root.expandDescriptionChevron.setOnClickListener {
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
        }*/

        root.secondaryText.text = playlist.ownerName ?: "Playlist"

        root.descriptionText.text = (playlist as? Playlist)?.entries?.size
                ?.toString()?.plus(" songs") ?: ""

        root.downloadButton.setOnClickListener {
            activity?.let { Toast.makeText(it,
                    "Downloading is not supported yet", Toast.LENGTH_LONG).show() }
        }
        root.overflowButton.setOnClickListener { v ->
            (activity as? MainActivity)?.showPlaylistPopup(v, realPlaylist)
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

        root.play_fab.visibility = View.INVISIBLE

        tintView(root)

        return root
    }

    private fun tintView(view: View) {
        view.primaryText.setTextColor(textColor)
        view.secondaryText.setTextColor(textColor)
        view.constraintLayout6.background = ColorDrawable(mainColor)
        view.songgroup_grad.background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(mainColor, ColorUtils.modifyAlpha(mainColor, 200),
                        ColorUtils.modifyAlpha(mainColor, 0)))

        //view.artistTracksHeader.visibility = View.VISIBLE

        view.downloadButton.imageTintList = ColorStateList.valueOf(textColor)
        view.overflowButton.imageTintList = ColorStateList.valueOf(textColor)
        view.expandDescriptionChevron.imageTintList = ColorStateList.valueOf(bodyColor)

        view.play_fab.backgroundTintList = ColorStateList.valueOf(secondaryColor)
        view.play_fab.imageTintList = ColorStateList.valueOf(secondaryTextColor)
    }

    private fun setupView(view: View, fromFuture: Boolean): View {

        carbonAnalytics.logEntityEvent(FirebaseAnalytics.Event.VIEW_ITEM, realPlaylist)

        if(realPlaylist.description == null && realPlaylist is Playlist &&
                (fromFuture || futurePlaylist == null) && realPlaylist.id != null) {
            val aId = realPlaylist.id!!
            Protocol.getNautilusPlaylist(activity!!, aId)
                    .subscribeOn(Schedulers.io())
                    .delay(300, TimeUnit.MILLISECONDS) /* Prevent animation interrupt */
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ a ->

                        val rlm = Realm.getDefaultInstance()

                        playlistProxy = rlm.where(Playlist::class.java)
                                .equalTo(Playlist.REMOTE_ID, aId)
                                .findFirst()!!

                        Timber.d("Running playlistProxy update")
                    }, {
                        err -> Timber.e(err)
                    }).addToAutoDispose()
        }

        root.albumLayoutRoot.background = ColorDrawable(Color.WHITE)
        root.songgroup_loader.visibility = View.INVISIBLE
        root.main_backdrop.visibility = View.VISIBLE
        root.songgroup_scrollview.visibility = View.VISIBLE

        val preImageWidth = IdentityUtils.displayWidthDp(activity as Activity) - 4

        /*if(!realPlaylist.artistBio.isNullOrBlank()) {
            val const = ConstraintSet().apply {
                clone(root.constraintLayout6)
                setMargin(R.id.primaryText, ConstraintSet.TOP, MathUtils.dpToPx2(resources, 32))
            }

            const.applyTo(root.constraintLayout6)

            root.descriptionText.run {
                text = realPlaylist.artistBio
                setTextColor(bodyColor)
                visibility = View.VISIBLE
                alpha = 1.0f
            }
        }*/

        if(fromFuture || futurePlaylist == null) Handler().postDelayed({
            root.play_fab.visibility = View.VISIBLE
            val anim = ScaleAnimation(0f, 1f, 0f, 1f, root.play_fab.pivotX,
                    (root.main_backdrop.height - fabOffset).toFloat())
            anim.fillAfter = true
            anim.duration = 310
            anim.interpolator = FastOutSlowInInterpolator()
            root.play_fab.startAnimation(anim)
        }, 600)

        root.play_fab.setOnClickListener {
            //manager.fromTracks(extractTopTracks(realPlaylist), 0, realPlaylist is Artist)
            carbonAnalytics.logEntityEvent("play_fab", realPlaylist)
        }

        root.primaryText.text = realPlaylist.name

        return root
    }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        root.play_fab.y = (root.main_backdrop.height - fabOffset).toFloat()
    }

    override fun onDestroyView(view: View) {
        requestMgr.clear(view.main_backdrop)
        super.onDestroyView(view)
    }
}