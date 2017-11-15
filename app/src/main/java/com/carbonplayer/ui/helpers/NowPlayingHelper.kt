package com.carbonplayer.ui.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.*
import android.support.v4.content.ContextCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.LinearLayoutManager
import android.view.KeyEvent
import android.view.View
import android.widget.Scroller
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.carbonplayer.R
import com.carbonplayer.audio.MusicPlayerService
import com.carbonplayer.audio.TrackQueue
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.entity.ParcelableMusicTrack
import com.carbonplayer.ui.main.adapters.NowPlayingQueueAdapter
import com.carbonplayer.utils.Constants
import com.carbonplayer.utils.asParcel
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.ui.AnimUtils
import kotlinx.android.synthetic.main.controller_main.*
import kotlinx.android.synthetic.main.nowplaying.*
import kotlinx.android.synthetic.main.nowplaying.view.*
import timber.log.Timber


/**
 * Manages now playing UI and sends commands to [com.carbonplayer.audio.MusicPlayerService]
 */
class NowPlayingHelper(private val activity: Activity) {

    var bottomNavHeight: Int = 0
    lateinit var replyMessenger: Messenger

    private val dispW = IdentityUtils.displayWidth2(activity)
    private val dispH = IdentityUtils.displayHeight2(activity)
    private val controlsScalar = (dispH.toFloat() / dispW.toFloat()) * 2.4f
    private val dp56 = MathUtils.dpToPx2(activity.resources, 56).toInt()
    private val buttonHalfWidth = MathUtils.dpToPx2(activity.resources, 16)
    private val prevInitialX = dispW - MathUtils.dpToPx2(activity.resources, 132)
    private val playPauseInitialX = dispW - MathUtils.dpToPx2(activity.resources, 90)
    private val nextInitialX = dispW - MathUtils.dpToPx2(activity.resources, 48)
    private val audioManager = activity.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var lastVolumePercent = 0f
    var playing = false

    private var recyclerIsUp = false
    private val recyclerScroller = Scroller(activity, FastOutSlowInInterpolator())

    private val queueSwipeRunnable = Runnable {
        if(!recyclerIsUp && !recyclerScroller.isFinished) {
            recyclerScroller.computeScrollOffset()
            activity.npui_recycler.translationY = -recyclerScroller.currY.toFloat()
        }
        repostQueueSwipe()
    }

    private var connection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            messenger = Messenger(service)
            try {
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

        if (isServiceRunning()) {
            val intent = newIntent().apply {
                action = Constants.ACTION.SEND_STATE
                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        } else {
            activity.nowplaying_frame.visibility = View.GONE
        }

        activity.nowplaying_frame.npui_volumebar_background
                .layoutParams.width = (dispW / 2f).toInt() - dp56

        activity.npui_recycler.translationY = dispW * 1.5f
        activity.npui_recycler.isNestedScrollingEnabled = false

        activity.nowplaying_frame.npui_volumebar_background.translationY =
                (dispW * 1.3f) + (dp56 /4)


        activity.nowplaying_frame.npui_volumeLow.translationY = dispW * 1.3f
        activity.nowplaying_frame.npui_volumeHi.translationY = dispW * 1.3f
        activity.nowplaying_frame.volume_fab.translationY = (dispW * 1.3f ) + (dp56 / 6f)
        activity.nowplaying_frame.npui_volumeLow.x = dispW / 4f - buttonHalfWidth
        activity.nowplaying_frame.npui_volumeHi.x = dispW - (dispW / 4f) - buttonHalfWidth
        activity.nowplaying_frame.volume_fab.x = ((1f - volumePercent()) * dispW / 4f) +
                (volumePercent() * (dispW - dispW / 4f - 2 * buttonHalfWidth))

        activity.nowplaying_frame.callback = { up ->

            activity.nowplaying_frame.npui_thumb.run {
                postOnAnimation {
                    layoutParams.width =
                            (up.times(dispW - dp56)).toInt() + dp56
                    invalidate()
                }
            }
            activity.nowplaying_frame.npui_song.run {
                postOnAnimation {
                    alpha = 1f - up
                    invalidate()
                }
            }
            activity.nowplaying_frame.npui_artist.run {
                postOnAnimation {
                    alpha = 1f - up
                    invalidate()
                }
            }

            try {
                activity.bottom_nav.run {
                    postOnAnimation {
                        layoutParams.height = (dp56 * (1f - up)).toInt()
                    }
                }
                activity.nowplaying_frame.npui_fastrewind.run {
                    postOnAnimation {
                        translationY = (up * dispW / controlsScalar)
                        x = ((dispW / 4f - buttonHalfWidth) * up) + (prevInitialX * (1f - up))
                    }
                }
                activity.nowplaying_frame.npui_playpause.run {
                    postOnAnimation {
                        translationY = (up * dispW / controlsScalar)
                        x = (((dispW / 2f) - buttonHalfWidth) * up) + (playPauseInitialX * (1f - up))
                    }
                }
                activity.nowplaying_frame.npui_fastforward.run {
                    postOnAnimation {
                        translationY = (up * dispW / controlsScalar)
                        x = ((dispW - (dispW / 4f) - buttonHalfWidth)) * up + (nextInitialX * (1f - up))
                    }
                }
            } catch (e: NullPointerException) {
                Timber.e("NPE in nowplayinghelper -> why does this happen? ", e)
            }

        }
    }

    private var messenger: Messenger? = null
    private var serviceStarted = false
    private var requestMgr = Glide.with(activity)

    fun repostQueueSwipe() {
        activity.npui_recycler.postOnAnimation(queueSwipeRunnable)
    }

    fun newQueue(tracks: List<MusicTrack>, pos: Int) {
        trackQueue.replace(tracks, pos)
    }

    fun newQueue(tracks: List<MusicTrack>) {
        newQueue(tracks, 0)
    }

    fun insertNext(tracks: List<MusicTrack>) {
        if(trackQueue.size > 0) {
            trackQueue.insertNext(tracks)
        } else trackQueue.replace(tracks, 0)
    }

    fun insertAtEnd(tracks: List<MusicTrack>) {
        if(trackQueue.size > 0) {
            trackQueue.insertAtEnd(tracks)
        } else trackQueue.replace(tracks, 0)
    }

    private fun maybeBind(intent: Intent) {
        Timber.d("Should bind to service?")
        if (!serviceStarted) {
            Timber.d("Binding to service")
            activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            serviceStarted = true
        } else Timber.d("Not binding to service, already started")
    }

    fun maybeHandleVolumeEvent() {
        if (volumePercent() != lastVolumePercent) {
            activity.nowplaying_frame.volume_fab.animate()
                    .x(((1f - volumePercent()) * dispW / 4f) +
                            (volumePercent() * (dispW - dispW / 4f - 2 * buttonHalfWidth)))
                    .setDuration(250).setInterpolator(FastOutSlowInInterpolator()).start()
        }
    }

    fun handleVolumeEvent(event: Int): Boolean {
        if (event == KeyEvent.KEYCODE_VOLUME_DOWN || event == KeyEvent.KEYCODE_VOLUME_UP) {
            if (activity.nowplaying_frame.isUp) {
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        if (event == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE
                        else AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                return true
            }
        }
        return false
    }

    fun volumePercent(): Float {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    }

    private fun revealPlayerUI() {
        AnimUtils.expand(activity.bottomNavContainer.nowplaying_frame)
        activity.nowplaying_frame.initialHeight = MathUtils.dpToPx2(activity.resources, 56)
                .toInt()
    }

    private fun newIntent(): Intent = Intent(activity, MusicPlayerService::class.java)

    private val trackQueueCallback = object : TrackQueue.TrackQueueCallback {
        override fun replace(tracks: MutableList<ParcelableMusicTrack>, pos: Int) {
            val intent = newIntent().apply {

                action = if (serviceStarted || isServiceRunning()) Constants.ACTION.NEW_QUEUE
                else Constants.ACTION.START_SERVICE

                putExtra(Constants.KEY.TRACKS, tracks.asParcel())
                putExtra(Constants.KEY.POSITION, pos)

                maybeBind(this)
            }

            activity.npui_recycler.layoutManager = LinearLayoutManager(activity)

            activity.npui_recycler.adapter = NowPlayingQueueAdapter(tracks, { i ->

            })

            ContextCompat.startForegroundService(activity, intent)
        }

        override fun insertAtEnd(tracks: MutableList<ParcelableMusicTrack>) {
            val intent = newIntent().apply {
                action = Constants.ACTION.INSERT_AT_END

                putExtra(Constants.KEY.TRACKS, tracks.asParcel())

                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        }

        override fun insertNext(tracks: MutableList<ParcelableMusicTrack>) {
            val intent = newIntent().apply {
                action = Constants.ACTION.INSERT_NEXT

                putExtra(Constants.KEY.TRACKS, tracks.asParcel())

                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
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
    }

    val trackQueue: TrackQueue = TrackQueue(trackQueueCallback)

    @SuppressLint("HandlerLeak")
    private inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            //Timber.i("Client received message %d", msg.what)
            when (msg.what) {
                Constants.EVENT.BufferProgress -> {
                    //Timber.d("Received bufferProgress %f", msg.obj as Float)
                }
                Constants.EVENT.TrackPlaying -> {
                    if (activity.nowplaying_frame.visibility != View.VISIBLE) {
                        revealPlayerUI()
                    }
                    val track = msg.obj as ParcelableMusicTrack
                    requestMgr.load(track.albumArtURL)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(activity.npui_thumb)

                    activity.npui_song.text = track.title
                    activity.npui_artist.text = track.artist
                }
                Constants.EVENT.Playing -> {
                    activity.npui_playpause
                            .setImageDrawable(activity.getDrawable(R.drawable.ic_pause))
                }
                Constants.EVENT.Paused -> {
                    activity.npui_playpause
                            .setImageDrawable(activity.getDrawable(R.drawable.ic_play))
                }

                else -> super.handleMessage(msg)
            }
        }
    }

    fun onDestroy() {
        messenger?.send(Message.obtain(null, Constants.MESSAGE.UNREGISTER_CLIENT).apply {
            replyTo = replyMessenger
        })
        if(isServiceRunning()) activity.unbindService(connection)
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            MusicPlayerService::class.java.name == it.service.className
        }
    }
}
