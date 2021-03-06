package com.carbonplayer.ui.main.library

import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintSet
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.R
import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.Artist
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.IArtist
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.helpers.NowPlayingHelper
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.*
import com.carbonplayer.ui.main.dataui.AlbumListController
import com.carbonplayer.ui.main.dataui.SongListController
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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_songgroup.view.*
import kotlinx.android.synthetic.main.nowplaying.*
import kotlinx.android.synthetic.main.songgroup_details.view.*
import org.apache.http.client.utils.CloneUtils.clone
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Displays an album
 */
class ArtistController(
        var artist: IArtist,
        var futureArtist: Observable<IArtist>?,
        var textColor: Int,
        var mainColor: Int,
        var bodyColor: Int,
        var secondaryColor: Int,
        var secondaryTextColor: Int
) : Controller() {

    internal lateinit var root: View

    private var fabOffset: Int = 0
    private var squareHeight: Int = 0

    private lateinit var realArtist: IArtist
    private var retrievedFuture = false
    //private val aId = artist

    private var expanded = false
    private var ogHeight = 0

    private var swatchPair: PaletteUtil.SwatchPair? = null

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private lateinit var manager: MusicManager
    private lateinit var requestMgr: RequestManager

    @Suppress("unused") @Keep constructor(savedState: Bundle) : this (
            Realm.getDefaultInstance().where(Artist::class.java)
                    .equalTo(Artist.ID, savedState.getString("artistId")).findFirst()!!,
            null,
            savedState.getInt("textColor"),
            savedState.getInt("mainColor"),
            savedState.getInt("bodyColor"),
            savedState.getInt("secondaryColor"),
            savedState.getInt("secondaryTextColor")
    )

    @Keep constructor(artist: IArtist, swatchPair: PaletteUtil.SwatchPair) : this (
            artist,
            null,
            swatchPair.primary.titleTextColor,
            swatchPair.primary.rgb,
            swatchPair.primary.bodyTextColor,
            swatchPair.secondary.rgb,
            swatchPair.secondary.bodyTextColor
    ) {
        this.swatchPair = swatchPair
    }

    @Keep constructor(artist: IArtist, futureArtist: Observable<IArtist>,
                      swatchPair: PaletteUtil.SwatchPair) : this (
            artist,
            futureArtist,
            swatchPair.primary.titleTextColor,
            swatchPair.primary.rgb,
            swatchPair.primary.bodyTextColor,
            swatchPair.secondary.rgb,
            swatchPair.secondary.bodyTextColor
    ) {
        this.swatchPair = swatchPair
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("artistId", realArtist.artistId)
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

        root.main_backdrop.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.TWO_BY_ONE
        root.parallaxSquare.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.TWO_BY_ONE

        if(!retrievedFuture) {
            futureArtist?.subscribe {
                retrievedFuture = true
                realArtist = it
                setupView(root, true)
            }
        }

        root.secondaryText.setText(R.string.media_type_artist)

        realArtist = if(retrievedFuture) realArtist else artist
        setupView(root, false)

        root.songgroup_recycler.isNestedScrollingEnabled = false

        root.expandDescriptionChevron.setOnClickListener {
            ogHeight = root.constraintLayout6.measuredHeight
            val anim = ValueAnimator.ofInt(
                    root.constraintLayout6.measuredHeight,
                    if(!expanded) root.constraintLayout6.measuredHeight + root.descriptionText.height
                    else ogHeight
            )
            anim.addUpdateListener {
                val int = it.animatedValue as Int
                val layoutParams = root.constraintLayout6.layoutParams
                layoutParams.height = int
                root.constraintLayout6.layoutParams = layoutParams
            }
            anim.duration = 300
            anim.interpolator = FastOutSlowInInterpolator()
            anim.start()

            expanded = !expanded
        }

        root.downloadButton.setOnClickListener {
            activity?.let { Toast.makeText(it,
                    "Downloading is not supported yet", Toast.LENGTH_LONG).show() }
        }
        root.overflowButton.setOnClickListener { v ->
            (activity as? MainActivity)?.showArtistPopup(v, realArtist)
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

        view.artistTracksHeader.visibility = View.VISIBLE

        view.downloadButton.imageTintList = ColorStateList.valueOf(textColor)
        view.overflowButton.imageTintList = ColorStateList.valueOf(textColor)
        view.expandDescriptionChevron.imageTintList = ColorStateList.valueOf(bodyColor)

        view.play_fab.backgroundTintList = ColorStateList.valueOf(secondaryColor)
        view.play_fab.imageTintList = ColorStateList.valueOf(secondaryTextColor)
    }

    private fun setupView(view: View, fromFuture: Boolean): View {

        carbonAnalytics.logEntityEvent(FirebaseAnalytics.Event.VIEW_ITEM, realArtist)

        if(realArtist.artistBio == null && realArtist is Artist &&
                (fromFuture || futureArtist == null)) {
            val aId = realArtist.artistId
            Protocol.getNautilusArtist(activity!!, realArtist.artistId)
                    .subscribeOn(Schedulers.io())
                    .delay(300, TimeUnit.MILLISECONDS) /* Prevent animation interrupt */
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ a ->

                        if(a.artistBio != null && a.artistBio!!.isNotBlank()) {
                            val const = ConstraintSet().apply {
                                clone(view.constraintLayout6)
                                setMargin(R.id.primaryText, ConstraintSet.TOP,
                                        MathUtils.dpToPx2(resources, 32))
                            }

                            val t = AutoTransition().apply {
                                interpolator = FastOutSlowInInterpolator()
                                duration = 250
                            }

                            val bioText = a.artistBio

                            view.post {
                                TransitionManager.beginDelayedTransition(view.constraintLayout6, t)
                                const.applyTo(view.constraintLayout6)

                                view.descriptionText.run {
                                    text = bioText
                                    setTextColor(bodyColor)
                                    visibility = View.VISIBLE
                                    alpha = 0.0f
                                    animate().alpha(1f).setDuration(250).start()
                                }
                            }
                        }

                        setupWithArtist(root, a)
                    }, {
                        err -> Timber.e(err)
                    }).addToAutoDispose()
        }

        Timber.d("artist ${realArtist.artistId}")
        root.albumLayoutRoot.background = ColorDrawable(Color.WHITE)
        root.songgroup_loader.visibility = View.INVISIBLE
        root.main_backdrop.visibility = View.VISIBLE
        root.songgroup_scrollview.visibility = View.VISIBLE

        val preImageWidth = IdentityUtils.displayWidthDp(activity as Activity) - 4

        realArtist.bestArtistArtUrl?.let {
            requestMgr.load(it)
                    .apply { if(swatchPair == PaletteUtil.DEFAULT_SWATCH_PAIR) {
                        listener(GlidePalette.with(it)
                                .use(0)
                                .intoCallBack({ palette -> if (palette != null) {
                                    val pair = PaletteUtil.getSwatches(activity!!, palette)
                                    mainColor = pair.primary.rgb
                                    bodyColor = pair.primary.bodyTextColor
                                    secondaryColor = pair.secondary.rgb
                                    textColor = pair.primary.titleTextColor
                                    secondaryTextColor = pair.secondary.bodyTextColor
                                    tintView(root)
                            }
                        }))
                    }}.into(root.main_backdrop)
        }

        setupWithArtist(root, realArtist)

        if(!realArtist.artistBio.isNullOrBlank()) {
            val const = ConstraintSet().apply {
                clone(root.constraintLayout6)
                setMargin(R.id.primaryText, ConstraintSet.TOP, MathUtils.dpToPx2(resources, 32))
            }

            const.applyTo(root.constraintLayout6)

            root.descriptionText.run {
                text = realArtist.artistBio
                setTextColor(bodyColor)
                visibility = View.VISIBLE
                alpha = 1.0f
            }
        }

        if(fromFuture || futureArtist == null) Handler().postDelayed({
            root.play_fab.visibility = View.VISIBLE
            val anim = ScaleAnimation(0f, 1f, 0f, 1f, root.play_fab.pivotX,
                    (root.main_backdrop.height - fabOffset).toFloat())
            anim.fillAfter = true
            anim.duration = 310
            anim.interpolator = FastOutSlowInInterpolator()
            root.play_fab.startAnimation(anim)
        }, 600)

        root.play_fab.setOnClickListener {
            manager.fromTracks(extractTopTracks(realArtist), 0, realArtist is Artist)
            carbonAnalytics.logEntityEvent("play_fab", realArtist)
        }

        root.primaryText.text = realArtist.name

        return root
    }

    private fun setupWithArtist(view: View, artist: IArtist) {

        if(extractTopTracks(artist).isNotEmpty()) {
            val params = root.songgroup_recycler.layoutParams
            params.height = (extractTopTracks(artist)
                    .take(5).size * MathUtils.dpToPx2(resources, SongListAdapter.SONG_HEIGHT_DP)) +
                    IdentityUtils.getNavbarHeight(resources) + (activity as MainActivity).bottomInset

            mAdapter = LightListSongAdapter(extractTopTracks(artist).take(5), { t, i ->
                manager.fromTracks(extractTopTracks(artist), i, artist is Artist)
            }, { v, track ->  (activity as MainActivity).showTrackPopup(v, track) })

            // use a linear layout manager
            mLayoutManager = LinearLayoutManager(activity)

            activity!!.runOnUiThread {
                root.songgroup_recycler.layoutManager = mLayoutManager
                root.songgroup_recycler.adapter = mAdapter
            }

            root.artistSeeMore.setOnClickListener {
                (activity as MainActivity).goto(SongListController(
                        extractTopTracks(artist),
                        swatchPair ?: PaletteUtil.DEFAULT_SWATCH_PAIR
                ))
            }
        }

        if(extractRelatedArtists(artist).isNotEmpty()) {
            val params = view.songgroup_recycler.layoutParams
            params.height = (extractTopTracks(artist)
                    .take(5).size *
                    MathUtils.dpToPx2(resources, SongListAdapter.SONG_HEIGHT_DP))
            view.artistgroup_recycler.setPadding(0, 0, 0,
                    IdentityUtils.getNavbarHeight(resources) +
                            MathUtils.dpToPx2(resources,
                                    if ((activity as MainActivity).nowplaying_frame
                                            .visibility == View.VISIBLE)
                                        NowPlayingHelper.HEIGHT_DP else 0))
            view.artistArtistsHeader.visibility = View.VISIBLE
            view.artistgroup_recycler.visibility = View.VISIBLE
            view.artistgroup_recycler.layoutManager = LinearLayoutManager(activity)
            view.artistgroup_recycler.adapter = LinearArtistAdapter(
                    activity!!,
                    extractRelatedArtists(artist).take(5),
                    { (artist, swPair) ->
                        (activity as MainActivity).gotoArtist(artist,
                                swPair ?: PaletteUtil.DEFAULT_SWATCH_PAIR)
                    }, true)

        }

        if(extractAllAlbums(artist).isNotEmpty()) {
            val params = root.songgroup_recycler.layoutParams
            params.height = (extractTopTracks(artist)
                    .take(5).size *
                    MathUtils.dpToPx2(resources, SongListAdapter.SONG_HEIGHT_DP))
            root.artistgroup_recycler.setPadding(0, 0, 0, 0)
            root.albumgroup_recycler.setPadding(0, 0, 0,
                    IdentityUtils.getNavbarHeight(resources) +
                            (activity as MainActivity).bottomInset)
            root.artistAlbumsHeader.visibility = View.VISIBLE
            root.albumgroup_recycler.visibility = View.VISIBLE
            root.albumgroup_recycler.layoutManager = GridLayoutManager(activity, 2)
            root.albumgroup_recycler.adapter = if(artist is Artist) AlbumAdapterJ(
                    extractAllAlbums(artist).take(4) as List<Album>,
                    activity as MainActivity, requestMgr) else TopChartsAlbumAdapter(
                    extractAllAlbums(artist).take(4) as List<SkyjamAlbum>,
                    activity as MainActivity,
                    requestMgr)
            if(extractAllAlbums(artist).size < 4) {
                root.artistAllAlbums.visibility = View.INVISIBLE
            } else {
                root.artistAllAlbums.visibility = View.VISIBLE
                root.artistAllAlbums.setOnClickListener {
                    (activity as MainActivity).goto(AlbumListController(
                            extractAllAlbums(artist),
                            swatchPair ?: PaletteUtil.DEFAULT_SWATCH_PAIR
                    ))
                }
            }
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        root.artistgroup_recycler.requestLayout()
    }

    private fun extractTopTracks(artist: IArtist): List<ITrack> {
        return (artist as? SkyjamArtist)?.topTracks ?: (artist as Artist).topTracks
    }

    private fun extractRelatedArtists(artist: IArtist): List<IArtist> {
        return (artist as? SkyjamArtist)?.related_artists ?: (artist as Artist).related_artists
    }

    private fun extractAllAlbums(artist: IArtist): List<IAlbum> {
        return (artist as? SkyjamArtist)?.albums ?: (artist as Artist).albums
    }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        root.play_fab.y = (root.main_backdrop.height - fabOffset).toFloat()
    }

    override fun onDestroyView(view: View) {
        requestMgr.clear(view.main_backdrop)
        super.onDestroyView(view)
    }
}