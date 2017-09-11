package com.carbonplayer.ui.main

import android.app.Fragment
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.ScaleAnimation
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.helpers.NowPlayingUIHelper
import com.carbonplayer.ui.transition.DetailSharedElementEnterCallback
import com.carbonplayer.utils.ColorUtils
import com.carbonplayer.utils.IdentityUtils
import com.carbonplayer.utils.MathUtils
import com.github.florent37.glidepalette.GlidePalette
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_songgroup.view.*
import kotlinx.android.synthetic.main.songgroup_details.view.*
import timber.log.Timber

/**
 * Displays an album
 */
class AlbumFragment : Fragment() {

    //onstructor(album: Album) : this()

    internal lateinit var root: View


    private var fabOffset: Int = 0
    private var squareHeight: Int = 0

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private lateinit var album: Album
    private lateinit var tracks: List<MusicTrack>

    private lateinit var manager: MusicManager

    private var nowPlayingHelper: NowPlayingUIHelper? = null

    override fun onCreateView(inflater: LayoutInflater, containerView: ViewGroup?, savedInstanceState: Bundle?): View {

        album = Realm.getDefaultInstance().where(Album::class.java)
                .equalTo("id", arguments.getString("album_id")).findFirst()

        root = inflater.inflate(R.layout.activity_songgroup, containerView, false)

        manager = MusicManager(activity as MainActivity)

        //nowPlayingHelper = NowPlayingUIHelper(activity)

        Timber.d("album %s", album.id)

        root.primaryText.setTextColor(arguments.getInt("text_color"))
        root.secondaryText.setTextColor(arguments.getInt("text_color"))
        root.constraintLayout6.background = ColorDrawable(arguments.getInt("main_color"))

        root.main_backdrop.transitionName = album.id + "i"
        root.constraintLayout6.transitionName = album.id + "cr"
        root.primaryText.transitionName = album.id + "t"
        root.secondaryText.transitionName = album.id + "d"

        root.songgroup_scrollview.setScrollViewCallbacks(object: ObservableScrollViewCallbacks {
            override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {}

            override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
                root.main_backdrop.offset = (-scrollY).toFloat()
            }

            override fun onDownMotionEvent() {}
            //root.main_backdrop.offset = (-scrollY).toFloat()
        })

        root.albumLayoutRoot.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        squareHeight = root.main_backdrop.height
        fabOffset = MathUtils.dpToPx(activity, 28)

        val preImageWidth = IdentityUtils.displayWidthDp(activity) - 4


        val handler = DetailSharedElementEnterCallback(this)
        handler.addTextViewSizeResource(root.primaryText,
                R.dimen.small_text_size, R.dimen.large_text_size)
        handler.addTextViewSizeResource(root.secondaryText,
                R.dimen.small_text_2, R.dimen.large_text_2)

        Glide.with(this).load(album.albumArtURL)
                .apply(RequestOptions.overrideOf(preImageWidth, preImageWidth).diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate())
                .listener(
                        GlidePalette.with(album.albumArtURL)
                                .use(0)
                                /*.intoTextColor(root.primaryText, BitmapPalette.Swatch.BODY_TEXT_COLOR)*/
                                /*.intoTextColor(root.secondaryText, BitmapPalette.Swatch.BODY_TEXT_COLOR)*/
                                .intoCallBack { palette -> palette?.let {
                                    if (Color.red(ColorUtils.contrastColor(it.getVibrantColor(Color.DKGRAY))) > 200) {
                                        Timber.d("red>200")
                                        root.play_fab.backgroundTintList = ColorStateList.valueOf(it.getLightVibrantColor(Color.WHITE))
                                        nowPlayingHelper?.detailsView?.setBackgroundColor(it.getLightVibrantColor(Color.WHITE))
                                    } else {
                                        Timber.d("not")
                                        val s = ColorStateList.valueOf(it.getDarkVibrantColor(Color.DKGRAY))
                                        val t = ColorStateList.valueOf(it.getVibrantColor(Color.WHITE))
                                        Timber.d(s.toString())
                                        Timber.d(t.toString())
                                        root.play_fab.backgroundTintList = s
                                        root.play_fab.imageTintList = t
                                        nowPlayingHelper?.detailsView?.setBackgroundColor(it.getDarkVibrantColor(Color.DKGRAY))
                                    } }
                                }
                )
                .into(root.main_backdrop)

        root.play_fab.visibility = View.INVISIBLE
        Handler().postDelayed({
            root.play_fab.visibility = View.VISIBLE
            val anim = ScaleAnimation(0f, 1f, 0f, 1f, root.play_fab.pivotX, (root.main_backdrop.height - fabOffset).toFloat())
            anim.fillAfter = true
            anim.duration = 310
            anim.interpolator = FastOutSlowInInterpolator()
            root.play_fab.startAnimation(anim)
        }, 600)


        /*new Handler().postDelayed(() ->
                Glide.with(AlbumActivity.this).load(mAlbum.getAlbumArtURL())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .into(albumart), 1000);*/

        root.secondaryText.text = album.artist
        root.primaryText.text = album.title

        root.songgroup_recycler.isNestedScrollingEnabled = false

        // use a linear layout manager
        mLayoutManager = object : LinearLayoutManager(activity) {

            /*@Override
            public boolean canScrollVertically() { return false; }*/
        }
        root.songgroup_recycler.layoutManager = mLayoutManager

                tracks = MusicLibrary.getInstance().getAllAlbumTracks(album.id)


        val params = root.songgroup_recycler.layoutParams
        params.height = tracks.size * MathUtils.dpToPx(activity, 67)

        mAdapter = SongListAdapter(tracks) { (id, clientID, nautilusID) ->
            manager.fromAlbum(album.id)
        }
        root.songgroup_recycler.adapter = mAdapter

        return root
    }

    @OnClick(R.id.play_fab)
    internal fun playFABClicked() {
        manager.fromAlbum(album.id)
    }

    fun setTransformedTextPosition(transform: Int) {
        root.secondaryText.layout(root.secondaryText.left, root.secondaryText.top + transform,
                root.secondaryText.right, root.secondaryText.bottom + transform)
    }

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {

        val scrollY = root.songgroup_scrollview.scrollY
        root.main_backdrop.offset = (-scrollY).toFloat()
        //fab.setY(albumart.getHeight() - scrollY - fabOffset);
    }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { root.play_fab.y = (root.main_backdrop.height - fabOffset).toFloat() }

    /*@Override
    public void onResume(){
        super.onResume();
        Glide.with(AlbumActivity.this).load(mAlbum.getAlbumArtURL())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(albumart);
    }

    @Override
    public void onStop(){
        super.onStop();
        Glide.clear(albumart);
    }*/
}
