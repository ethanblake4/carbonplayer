package com.carbonplayer.ui.main.library

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.SongListAdapter
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.ui.ColorUtils
import com.github.florent37.glidepalette.GlidePalette
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_songgroup.view.*
import kotlinx.android.synthetic.main.songgroup_details.view.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber

/**
 * Displays an album
 */
class AlbumController(
        val albumId: String,
        val textColor: Int,
        val mainColor: Int
) : Controller() {

    internal lateinit var root: View

    private var fabOffset: Int = 0
    private var squareHeight: Int = 0

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private lateinit var album: Album
    private lateinit var tracks: List<MusicTrack>

    private lateinit var manager: MusicManager
    private lateinit var requestMgr: RequestManager

    @Suppress("unused") @Keep constructor(savedState: Bundle) : this (
            savedState.getString("albumId"),
            savedState.getInt("textColor"),
            savedState.getInt("mainColor")
    )

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("albumId", albumId)
        outState.putInt("textColor", textColor)
        outState.putInt("mainColor", mainColor)
        super.onSaveInstanceState(outState)
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)

        album = Realm.getDefaultInstance().where(Album::class.java)
                .equalTo("id", albumId).findFirst()

        if(album.description == null) {
            Protocol.getNautilusAlbum(activity!!, Realm.getDefaultInstance().copyFromRealm(album), album.id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ a ->

                        try {
                            Realm.getDefaultInstance().executeTransaction { rlm ->
                                rlm.insertOrUpdate(a)
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        album = a

                        if(view != null && album.description != null && album.description!!.isNotBlank()) {
                            val const = ConstraintSet().apply {
                                clone(view!!.constraintLayout6)
                                setMargin(R.id.primaryText, ConstraintSet.TOP,
                                        MathUtils.dpToPx2(context.resources, 32).toInt())
                            }

                            val t = AutoTransition().apply {
                                interpolator = FastOutSlowInInterpolator()
                                duration = 250
                            }

                            TransitionManager.beginDelayedTransition(view!!.constraintLayout6, t)
                            const.applyTo(view!!.constraintLayout6)

                            view!!.descriptionText.run {
                                text = album.description
                                visibility = View.VISIBLE
                                alpha = 0.0f
                                animate().alpha(1f).setDuration(250).start()
                            }
                        }
                    }, {
                        err -> Timber.e(err)
                    })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        root = inflater.inflate(R.layout.activity_songgroup, container, false)

        manager = MusicManager(activity as MainActivity)

        Timber.d("album %s", album.id)

        root.primaryText.setTextColor(textColor)
        root.secondaryText.setTextColor(textColor)
        root.constraintLayout6.background = ColorDrawable(mainColor)
        root.songgroup_grad.background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(mainColor, ColorUtils.modifyAlpha(mainColor, 0)))

        root.downloadButton.imageTintList = ColorStateList.valueOf(textColor)
        root.overflowButton.imageTintList = ColorStateList.valueOf(textColor)

        root.downloadButton.setOnClickListener {
            activity?.let { Toast.makeText(it, "Downloading is not supported yet", Toast.LENGTH_LONG).show() }
        }
        root.overflowButton.setOnClickListener { v ->
            (activity as? MainActivity)?.showAlbumPopup(v, album)
        }

        root.main_backdrop.transitionName = album.id + "i"
        root.constraintLayout6.transitionName = album.id + "cr"
        root.primaryText.transitionName = album.id + "t"
        root.secondaryText.transitionName = album.id + "d"

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

        requestMgr = Glide.with(activity as Activity)

        requestMgr.load(album.albumArtURL)
                .apply(
                        RequestOptions.overrideOf(preImageWidth, preImageWidth)
                                .diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate())
                .listener(
                        GlidePalette.with(album.albumArtURL)
                                .use(0)
                                .intoCallBack { palette ->
                                    palette?.let {
                                        if (Color.red(ColorUtils.contrastColor(
                                                it.getVibrantColor(Color.DKGRAY))) > 200) {
                                            Timber.d("red>200")
                                            root.play_fab.backgroundTintList =
                                                    ColorStateList.valueOf(
                                                            it.getLightVibrantColor(Color.WHITE))
                                        } else {
                                            Timber.d("not")
                                            val s = ColorStateList.valueOf(
                                                    it.getDarkVibrantColor(Color.DKGRAY))
                                            val t = ColorStateList.valueOf(
                                                    it.getVibrantColor(Color.WHITE))
                                            Timber.d(s.toString())
                                            Timber.d(t.toString())
                                            root.play_fab.backgroundTintList = s
                                            root.play_fab.imageTintList = t
                                        }
                                    }
                                }
                )
                .into(root.main_backdrop)

        if(album.description != null && album.description!!.isNotBlank()) {

            val const = ConstraintSet().apply {
                clone(root.constraintLayout6)
                setMargin(R.id.primaryText, ConstraintSet.TOP, MathUtils.dpToPx2(resources, 32).toInt())
            }

            const.applyTo(root.constraintLayout6)

            root.descriptionText.run {
                text = album.description
                setTextColor(textColor)
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


        root.secondaryText.text = album.artist
        root.primaryText.text = album.title

        root.songgroup_recycler.isNestedScrollingEnabled = false

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(activity)

        root.songgroup_recycler.layoutManager = mLayoutManager

        tracks = MusicLibrary.getInstance().getAllAlbumTracks(album.id)

        val params = root.songgroup_recycler.layoutParams
        params.height = tracks.size * MathUtils.dpToPx(activity, 67)

        mAdapter = SongListAdapter(tracks) { (id, pos) ->
            manager.fromAlbum(album.id, pos)
        }

        root.play_fab.setOnClickListener {
            manager.fromAlbum(album.id, 0)
        }
        root.songgroup_recycler.adapter = mAdapter

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
