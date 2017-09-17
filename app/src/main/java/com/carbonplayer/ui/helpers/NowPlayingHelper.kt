package com.carbonplayer.ui.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import android.widget.LinearLayout

import com.bumptech.glide.Glide
import com.carbonplayer.audio.MusicPlayerService
import com.carbonplayer.audio.TrackQueue
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.entity.ParcelableMusicTrack
import com.carbonplayer.utils.Constants
import com.carbonplayer.utils.asParcel

import org.parceler.Parcels

import timber.log.Timber

/**
 * Manages now playing UI and sends commands to [com.carbonplayer.audio.MusicPlayerService]
 */
class NowPlayingHelper(private val activity: Activity) {

    private var messenger: Messenger? = null
    private var serviceStarted = false

    private val mainFrame: LinearLayout? = null
    private val thumb: ImageView? = null
    val detailsView: ConstraintLayout? = null
    private val playPause: ImageView? = null

    fun newQueue(tracks: List<MusicTrack>) {
        trackQueue.replace(tracks)
        /*Glide.with(activity)
                .load(trackQueue.currentTrack().getAlbumArtURL())
                .into(thumb);*/
    }
/*
    fun makePlayingScreen() {
        mainFrame?.visibility = View.VISIBLE
        val anim = AlphaAnimation(0.0f, 1.0f)
        //TranslateAnimation anim = new TranslateAnimation(0,0, IdentityUtils.displayHeight(activity), 0);
        anim.duration = 500
        anim.fillAfter = true
        mainFrame?.startAnimation(anim)
        activity.startService(buildServiceIntent())
    }

    fun makePlayingScreen(drawable: Drawable) {
        mainFrame?.visibility = View.VISIBLE
        val anim = AlphaAnimation(0.0f, 1.0f)
        //TranslateAnimation anim = new TranslateAnimation(0,0, IdentityUtils.displayHeight(activity), 0);
        anim.duration = 500
        anim.fillAfter = true
        mainFrame?.startAnimation(anim)
        thumb?.setImageDrawable(drawable)
    }*/

    fun updateDrawable() {
        val url = trackQueue.currentTrack().albumArtURL
        Glide.with(activity).load(url).into(thumb!!)
    }

    private fun maybeBind(intent: Intent) {
        if(!serviceStarted) {
            activity.bindService(intent, connection, Context.BIND_DEBUG_UNBIND)
            serviceStarted = true
        }
    }

    private fun newIntent(): Intent = Intent(activity, MusicPlayerService::class.java)

    private val trackQueueCallback = object: TrackQueue.TrackQueueCallback {
        override fun replace(tracks: MutableList<ParcelableMusicTrack>) {
            val intent = newIntent().apply {
                action = if (serviceStarted) Constants.ACTION.NEW_QUEUE
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
            when (msg.what) {
                Constants.EVENT.BufferProgress -> {
                    Timber.d("Received bufferProgress %f", msg.obj as Float)
                }
                Constants.EVENT.NextSong -> {
                    trackQueue.nexttrack()
//                    updateDrawable()
                }
                Constants.EVENT.PrevSong -> {
                    trackQueue.prevtrack()
                    //updateDrawable()
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            messenger = Messenger(service)
            try {
                val msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT)
                msg.replyTo = Messenger(IncomingHandler())
                messenger?.send(msg)
            } catch (e: RemoteException) {
                // In this case the service has crashed before we could even do anything with it
                messenger = null
            }

        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            messenger = null
        }
    }


}
