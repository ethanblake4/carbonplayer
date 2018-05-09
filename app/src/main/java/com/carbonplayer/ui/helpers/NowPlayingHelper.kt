package com.carbonplayer.ui.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.*
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.TextUtils
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.audio.MusicPlayerService
import com.carbonplayer.audio.TrackQueue
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.RadioFeedReason
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.main.adapters.NowPlayingQueueAdapter
import com.carbonplayer.ui.widget.helpers.QueueItemTouchCallback
import com.carbonplayer.utils.Constants
import com.carbonplayer.utils.asParcel
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.ui.AnimUtils
import com.carbonplayer.utils.ui.ColorUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.github.florent37.glidepalette.GlidePalette
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.CompletableSubject
import kotlinx.android.synthetic.main.controller_main.*
import kotlinx.android.synthetic.main.nowplaying.*
import kotlinx.android.synthetic.main.nowplaying.view.*
import timber.log.Timber


/**
 * Manages now playing UI and sends commands to [MusicPlayerService]
 * Also handles playing from radio stations
 *
 * The structure of this class is a bit confusing. When the queue is modified locally, this class
 * typically will notify its [TrackQueue] of the change, which will then callback to this class's
 * [TrackQueue.TrackQueueCallback], which will finally alert the [MusicPlayerService] of the change.
 *
 */

class NowPlayingHelper(private val activity: Activity) {

    var bottomNavHeight: Int = 0
    lateinit var replyMessenger: Messenger

    private val dispW = IdentityUtils.displayWidth2(activity)
    private val dispH = IdentityUtils.displayHeight2(activity)
    private val controlsScalar = (dispH.toFloat() / dispW.toFloat()) * 2f
    private val heightPx = MathUtils.dpToPx2(activity.resources, HEIGHT_DP)
    private val buttonHalfWidth = MathUtils.dpToPx2(activity.resources, 16)
    private val prevInitialX = dispW - MathUtils.dpToPx2(activity.resources, 132)
    private val playPauseInitialX = dispW - MathUtils.dpToPx2(activity.resources, 90)
    private val nextInitialX = dispW - MathUtils.dpToPx2(activity.resources, 48)
    private lateinit var touchHelper: ItemTouchHelper
    private var curTracK: ITrack? = null

    var recentlyPlayedTrackIds = mutableListOf<String>()

    private var queueAdapter: NowPlayingQueueAdapter? = null
    private var currentSeedType: Int? = null
    private var currentRadioSeed: String? = null
    private var currentRadioReason: RadioFeedReason? = null

    private var connection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            messenger = Messenger(service)

            try {
                /**
                 * Register an [IncomingHandler] so we receive messages from the service
                 */
                Timber.d("Registering bound client")
                val msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT)
                replyMessenger = Messenger(IncomingHandler())
                msg.replyTo = replyMessenger
                messenger?.send(msg)
            } catch (e: RemoteException) {
                // In this case the service has crashed before we could even do anything with it
                messenger = null
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            messenger = null
        }
    }

    init {

        bottomNavHeight = activity.bottom_nav.height

        if(activity.npui_recycler.layoutManager == null)
            activity.npui_recycler.layoutManager = LinearLayoutManager(activity)

        activity.npui_playpause.setOnClickListener {
            val intent = newIntent().apply {
                action = Constants.ACTION.PLAYPAUSE
                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        }

        activity.npui_fastforward.setOnClickListener {
            val intent = newIntent().apply {
                action = Constants.ACTION.NEXT
                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        }

        activity.npui_fastrewind.setOnClickListener {
            val intent = newIntent().apply {
                action = Constants.ACTION.PREVIOUS
                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        }

        /**
         * If the service is already running, the app was killed due to memory pressure
         * but we probably have active state. Tell the service to send us its state.
         */
        if (isServiceRunning()) {
            val intent = newIntent().apply {
                action = Constants.ACTION.SEND_STATE
                maybeBind(this)
            }
            ContextCompat.startForegroundService(activity, intent)
        } else {
            activity.nowplaying_frame.visibility = View.GONE
        }

        activity.seekBar.callback = { p ->
            activity.nowplaying_frame.seekSlider.translationX = p * dispW * 0.85f
        }

        activity.seekBar.upCallback = { p ->
            activity.nowplaying_frame.seekSlider.translationX = p * dispW * 0.85f
            curTracK?.let {
                val intent = newIntent().apply {
                    action = Constants.ACTION.SEEK
                    putExtra(Constants.KEY.POSITION, (p.toDouble() * it.durationMillis).toLong())
                    maybeBind(this)
                }
                ContextCompat.startForegroundService(activity, intent)
            }

        }

        activity.npui_recycler.translationY = dispW * 1.5f

        activity.npui_recycler.isNestedScrollingEnabled = false

        activity.nowplaying_frame.npui_mixDescriptor.translationY =
                (dispW * 1.3f) + (heightPx /4)

        activity.nowplaying_frame.seekBar.translationY = dispW.toFloat()

        /* The NPUI's expanded percent has changed **/
        activity.nowplaying_frame.callback = { up ->

            activity.nowplaying_frame.npui_thumb.layoutParams.width =
                            (up.times(dispW - heightPx)).toInt() + heightPx

            activity.nowplaying_frame.npui_song.alpha = 1f - up

            activity.nowplaying_frame.npui_artist.alpha = 1f - up

            try {
                activity.bottom_nav.layoutParams.height = (heightPx * (1f - up)).toInt()

                activity.nowplaying_frame.npui_fastrewind.run {
                        translationY = (up * dispW / controlsScalar)
                        x = ((dispW / 4f - buttonHalfWidth) * up) + (prevInitialX * (1f - up))
                }
                activity.nowplaying_frame.npui_playpause.run {
                        translationY = (up * dispW / controlsScalar)
                        x = (((dispW / 2f) - buttonHalfWidth) * up) + (playPauseInitialX * (1f - up))
                }
                activity.nowplaying_frame.npui_fastforward.run {
                        translationY = (up * dispW / controlsScalar)
                        x = ((dispW - (dispW / 4f) - buttonHalfWidth)) * up + (nextInitialX * (1f - up))
                }
            } catch (e: NullPointerException) {
                Timber.e("NPE in nowplayinghelper -> why does this happen? ", e)
            }

        }
    }

    private var messenger: Messenger? = null
    private var serviceStarted = false
    private var requestMgr = Glide.with(activity)

    fun newQueue(tracks: List<ITrack>, pos: Int, local: Boolean = true,
                 descriptor: String = "") {
        activity.nowplaying_frame.npui_mixDescriptor.text = descriptor
        trackQueue.replace(tracks, pos, false, local)

        currentSeedType = null
        currentRadioReason = null
        currentRadioSeed = null
    }

    fun startRadio(seedType: Int, seed: String,
                   reason: RadioFeedReason = RadioFeedReason.INSTANT_MIX ): Completable {

        val completer = CompletableSubject.create()

        Protocol.radioFeed(activity, seed, 25, reason,
                seedType, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ r ->
                    completer.onComplete()
                    newQueue(r.data.stations[0].tracks, 0, false,
                            "Playing from " +
                                    (r.data.stations[0].seed?.metadataSeed?.artist?.name ?:
                                    r.data.stations[0].seed?.metadataSeed?.album?.name ?:
                                    r.data.stations[0].seed?.metadataSeed?.playlist?.name
                                    ?: r.data.stations[0].name) + " radio")

                    currentSeedType = seedType
                    currentRadioReason = reason
                    currentRadioSeed = seed

                }, { err ->
                    Timber.d(err)
                })

        return completer
    }

    fun newQueue(tracks: List<ITrack>) {
        newQueue(tracks, 0)
    }

    fun insertNext(tracks: List<ITrack>) {
        if(trackQueue.size > 0) {
            trackQueue.insertNext(tracks)
        } else trackQueue.replace(tracks, 0)
    }

    fun insertAtEnd(tracks: List<ITrack>) {
        if(trackQueue.size > 0) {
            trackQueue.insertAtEnd(tracks)
        } else trackQueue.replace(tracks, 0)
    }

    /**
     * Bind to the service if the service is not started
     */
    private fun maybeBind(intent: Intent) {
        Timber.d("Should bind to service?")
        if (!serviceStarted) {
            Timber.d("Binding to service")
            activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            serviceStarted = true
        } else Timber.d("Not binding to service, already started")
    }

    fun maybeHandleVolumeEvent() {}

    fun handleVolumeEvent(event: Int) = false


    private fun revealPlayerUI() {
        // In the future, we should do a cool transition here
        AnimUtils.expand(activity.bottomNavContainer.nowplaying_frame, heightPx)
        activity.nowplaying_frame.initialHeight= heightPx
        activity.npui_recycler.initialY = dispW * 1.5f
    }

    private fun newIntent(): Intent = Intent(activity, MusicPlayerService::class.java)

    /**
     * When something in the queue changes, alert the service of the change
     */
    private val trackQueueCallback = object : TrackQueue.TrackQueueCallback {
        override fun replace(tracks: MutableList<ParcelableTrack>, pos: Int) {
            val intent = newIntent().apply {

                action = if (serviceStarted || isServiceRunning()) Constants.ACTION.NEW_QUEUE
                else Constants.ACTION.START_SERVICE

                putExtra(Constants.KEY.TRACKS, tracks.asParcel())
                putExtra(Constants.KEY.POSITION, pos)

                maybeBind(this)
            }

            updateRecycler(tracks)

            ContextCompat.startForegroundService(activity, intent)
        }

        override fun insertAtEnd(tracks: MutableList<ParcelableTrack>) {
            val intent = newIntent().apply {
                action = Constants.ACTION.INSERT_AT_END

                putExtra(Constants.KEY.TRACKS, tracks.asParcel())

                maybeBind(this)
            }

            updateRecycler(trackQueue.parcelable())

            ContextCompat.startForegroundService(activity, intent)
        }

        override fun insertNext(tracks: MutableList<ParcelableTrack>) {
            val intent = newIntent().apply {
                action = Constants.ACTION.INSERT_NEXT

                putExtra(Constants.KEY.TRACKS, tracks.asParcel())

                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)

            updateRecycler(trackQueue.parcelable())
        }

        override fun reorder(pos: Int, pnew: Int) {
            val intent = newIntent().apply {
                action = Constants.ACTION.REORDER

                putExtra(Constants.KEY.REORDER_FROM, pos)
                putExtra(Constants.KEY.REORDER_TO, pnew)

                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        }

        override fun remove(pos: Int) {
            val intent = newIntent().apply {
                action = Constants.ACTION.REMOVE

                putExtra(Constants.KEY.POSITION, pos)

                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        }
    }

    val trackQueue: TrackQueue = TrackQueue(trackQueueCallback)

    /**
     * Receives messages from the [MusicPlayerService]
     */
    @SuppressLint("HandlerLeak")
    private inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            //Timber.i("Client received message %d", msg.what)
            when (msg.what) {
                Constants.EVENT.BufferProgress -> {
                    //Timber.d("Received bufferProgress %f", msg.obj as Float)
                }
                Constants.EVENT.TrackPlaying -> {
                    // TODO This should probably be changed to act immediately upon UI signal
                    if (activity.nowplaying_frame.visibility != View.VISIBLE) {
                        revealPlayerUI()
                    }
                    curTracK = msg.obj as ParcelableTrack
                    val second = (curTracK!!.durationMillis / 1000) % 60
                    val minute = (curTracK!!.durationMillis / (1000 * 60)) % 60

                    activity.nowplaying_frame.songDuration.text =
                            String.format("%02d:%02d", minute, second)
                    requestMgr.load(curTracK!!.albumArtURL)
                            .listener(GlidePalette.with(curTracK!!.albumArtURL)
                            .use(0)
                            .intoCallBack{ palette -> if (palette != null) {
                                activity.nowplaying_frame.post {
                                    val pair = PaletteUtil.getSwatches(activity, palette)
                                    PaletteUtil.crossfadeBackground(
                                            activity.bottomNavContainer, pair.primary)
                                    PaletteUtil.crossfadeTitle(
                                            activity.npui_song, pair.primary)
                                    PaletteUtil.crossfadeSubtitle(activity.npui_artist, pair.primary)
                                    PaletteUtil.crossfadeSubtitle(activity.npui_mixDescriptor, pair.primary)
                                    val tintList = ColorStateList(
                                            arrayOf(IntArray(1, { -android.R.attr.state_checked }),
                                                    IntArray(1, {android.R.attr.state_checked})),
                                            arrayOf(ColorUtils.scrimify(pair.primary.bodyTextColor,
                                                    CarbonPlayerApplication.instance.preferences.scrimifyAmount),
                                                    ColorUtils.scrimify(pair.primary.bodyTextColor,
                                                            0.7f)).toIntArray()
                                            )

                                    activity.npui_fastrewind.imageTintList = tintList
                                    activity.npui_fastforward.imageTintList = tintList
                                    activity.npui_playpause.imageTintList = tintList
                                    activity.bottom_nav.itemIconTintList = tintList
                                    activity.bottom_nav.itemTextColor = tintList
                                }
                            }})
                            .apply(RequestOptions.overrideOf(dispW, dispH)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(activity.npui_thumb)

                    queueAdapter?.setPlaying((curTracK as ParcelableTrack).queuePosition)
                    trackQueue.position = (curTracK as ParcelableTrack).queuePosition

                    if(trackQueue.position > trackQueue.size - 10 && currentRadioSeed != null) {
                        Protocol.radioFeed(activity, currentRadioSeed!!, 25,
                                currentRadioReason!!, currentSeedType!!, null)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe ({ r ->
                                    insertAtEnd(r.data.stations[0].tracks)
                                }, { err -> Timber.d(err) })
                    }

                    activity.npui_song.text = curTracK!!.title
                    activity.npui_artist.text = curTracK!!.artist

                    activity.npui_song.ellipsize = TextUtils.TruncateAt.MARQUEE
                    activity.npui_song.setSingleLine(true)
                    activity.npui_song.marqueeRepeatLimit = 5
                    activity.npui_song.isSelected = true
                }

                Constants.EVENT.TrackPosition -> {
                    val positionMillis = msg.obj as Long
                    val second = (positionMillis / 1000) % 60
                    val minute = (positionMillis / (1000 * 60)) % 60
                    activity.nowplaying_frame.playTime.text =
                            String.format("%02d:%02d", minute, second)
                    curTracK?.let {
                        activity.nowplaying_frame.seekSlider.translationX =
                                ((positionMillis.toDouble() / it.durationMillis.toDouble())
                                        * dispW * 0.85).toFloat()

                    }
                }

                Constants.EVENT.Playing -> {
                    activity.npui_playpause
                            .setImageDrawable(activity.getDrawable(R.drawable.ic_pause))
                }
                Constants.EVENT.Paused -> {
                    activity.npui_playpause
                            .setImageDrawable(activity.getDrawable(R.drawable.ic_play))
                }

                Constants.EVENT.SendQueue -> {

                    revealPlayerUI()

                    updateRecycler(msg.obj as List<ParcelableTrack>)
                }

                else -> super.handleMessage(msg)
            }
        }
    }

    private fun updateRecycler(tracks: List<ParcelableTrack>) {
        if(activity.npui_recycler.layoutManager == null)
            activity.npui_recycler.layoutManager = LinearLayoutManager(activity)
        if (activity.npui_recycler.adapter == null) {
            setupQueueAdapter(tracks)
        }
        else (activity.npui_recycler.adapter as NowPlayingQueueAdapter).apply {
            dataset = tracks
            notifyDataSetChanged()
        }
    }

    private fun setupQueueAdapter(tracks: List<ParcelableTrack>) {

        queueAdapter = NowPlayingQueueAdapter(activity, tracks, { pos ->
            /* When an item in the queue has been clicked **/
            val intent = newIntent().apply {

                action = Constants.ACTION.SKIP_TO_TRACK
                putExtra(Constants.KEY.POSITION, pos.second)
                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        }, { vh -> /* When the drag handle is touched **/
            touchHelper.startDrag(vh)
        }, { pos -> /* When an item is removed via swipe **/
            trackQueue.remove(pos)
        }, { from, to -> /* When an item has been drag-reordered **/
            trackQueue.reorder(from, to)
        })

        activity.npui_recycler.adapter = queueAdapter!!
        val callback = QueueItemTouchCallback(queueAdapter!!)
        touchHelper = ItemTouchHelper(callback) /** Manages drag and swipe for queue **/
        touchHelper.attachToRecyclerView(activity.npui_recycler)
    }

    private fun addNextToRecycler(tracks: List<ParcelableTrack>) {
        if(activity.npui_recycler.layoutManager == null)
            activity.npui_recycler.layoutManager = LinearLayoutManager(activity)
        if (activity.npui_recycler.adapter == null)
            setupQueueAdapter(tracks)
        else (activity.npui_recycler.adapter as NowPlayingQueueAdapter).apply {
            dataset = tracks
            notifyItemRangeInserted(trackQueue.position, tracks.size)
        }
    }

    private fun addAtEndToRecycler(tracks: List<ParcelableTrack>, added: List<ParcelableTrack>) {
        if(activity.npui_recycler.layoutManager == null)
            activity.npui_recycler.layoutManager = LinearLayoutManager(activity)
        if (activity.npui_recycler.adapter == null)
            setupQueueAdapter(tracks)
        else (activity.npui_recycler.adapter as NowPlayingQueueAdapter).apply {
            dataset = tracks
            notifyItemRangeInserted(dataset.size - added.size, tracks.size)
        }
    }

    fun onDestroy() {
        messenger?.send(Message.obtain(null, Constants.MESSAGE.UNREGISTER_CLIENT).apply {
            replyTo = replyMessenger
        })
        if(isServiceRunning()) activity.unbindService(connection)
    }


    /**
     * Although this method is deprecated, it is the only way to tell if an application's own
     * Service is running. It is very unlikely that it will be removed.
     */
    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            MusicPlayerService::class.java.name == it.service.className
        }
    }

    companion object {
        const val HEIGHT_DP = 56
    }
}