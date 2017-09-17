package com.carbonplayer.ui.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v4.content.ContextCompat
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.carbonplayer.R
import com.carbonplayer.audio.MusicPlayerService
import com.carbonplayer.audio.TrackQueue
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.entity.ParcelableMusicTrack
import com.carbonplayer.utils.AnimUtils
import com.carbonplayer.utils.Constants
import com.carbonplayer.utils.asParcel
import kotlinx.android.synthetic.main.controller_main.*
import kotlinx.android.synthetic.main.nowplaying.*
import kotlinx.android.synthetic.main.nowplaying.view.*
import timber.log.Timber


/**
 * Manages now playing UI and sends commands to [com.carbonplayer.audio.MusicPlayerService]
 */
class NowPlayingHelper(private val activity: Activity) {

    var bottomNavHeight: Int = 0

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            messenger = Messenger(service)
            try {
                Timber.d("Registering bound client")
                val msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT)
                msg.replyTo = Messenger(IncomingHandler())
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

        if(isServiceRunning()) {
            val intent = newIntent().apply {
                action = Constants.ACTION.SEND_STATE

                maybeBind(this)
            }

            ContextCompat.startForegroundService(activity, intent)
        } else {
            activity.nowplaying_frame.visibility = View.GONE
        }
    }

    private var messenger: Messenger? = null
    private var serviceStarted = false
    private var requestMgr = Glide.with(activity)

    fun newQueue(tracks: List<MusicTrack>) {
        trackQueue.replace(tracks)
    }

    private fun maybeBind(intent: Intent) {
        Timber.d("Should bind to service?")
        if(!serviceStarted) {
            Timber.d("Binding to service")
            activity.bindService(intent, connection, Context.BIND_DEBUG_UNBIND)
            serviceStarted = true
        } else Timber.d("Not binding to service, already started")
    }

    private fun revealPlayerUI() {
        AnimUtils.expand(activity.bottomNavContainer.nowplaying_frame)
    }

    private fun newIntent(): Intent = Intent(activity, MusicPlayerService::class.java)

    private val trackQueueCallback = object: TrackQueue.TrackQueueCallback {
        override fun replace(tracks: MutableList<ParcelableMusicTrack>) {
            val intent = newIntent().apply {

                action = if (serviceStarted || isServiceRunning()) Constants.ACTION.NEW_QUEUE
                else Constants.ACTION.START_SERVICE

                putExtra(Constants.KEY.TRACKS, tracks.asParcel())

                maybeBind(this)
            }

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
                    if(activity.nowplaying_frame.visibility != View.VISIBLE) {
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

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            MusicPlayerService::class.java.name == it.service.className
        }
    }


}
