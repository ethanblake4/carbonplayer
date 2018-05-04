package com.carbonplayer.audio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.net.wifi.WifiManager
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.carbonplayer.R
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.utils.*
import com.google.common.collect.Queues
import org.parceler.Parcels
import timber.log.Timber
import java.util.*

/**
 * Music player foreground service
 */
@SuppressLint("WakelockTimeout")
class MusicPlayerService : Service(), MusicFocusable {

    internal lateinit var audioFocusHelper: AudioFocusHelper
    internal var audioFocus = AudioFocus.NoFocusNoDuck

    internal enum class AudioFocus {
        NoFocusNoDuck,
        NoFocusCanDuck,
        Focused
    }

    private var initialized = false

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationMgr: NotificationManagerCompat

    internal lateinit var wifiLock: WifiManager.WifiLock
    internal lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var notificationIntent: Intent
    private lateinit var previousIntent: Intent
    private lateinit var playPauseIntent: Intent
    private lateinit var nextIntent: Intent

    private lateinit var playback: MusicPlayback

    private var lastBitmap: Bitmap? = null
    private var lastNotifiedTrack: ITrack? = null
    private var bufferedPosition: Long = 0

    var clients = ArrayList<Messenger>()
    private var messageQueue = Queues.newLinkedBlockingQueue<Pair<Int, Any?>>()
    private var sendStateOnRegistered = false

    internal val messenger = Messenger(IncomingHandler())


    /** This is called any time we receive a command from the app **/
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            Constants.ACTION.START_SERVICE -> initService(intent)
            Constants.ACTION.NEW_QUEUE -> {
                val bundle = intent.extras
                val tracks = Parcels.unwrap<List<ParcelableTrack>>(
                        bundle.getParcelable<Parcelable>(Constants.KEY.TRACKS))

                fromNewQueue(tracks, bundle.getInt(Constants.KEY.POSITION))
            }
            Constants.ACTION.PREVIOUS -> {
                Timber.i("Clicked Previous")
                playback.prevTrack()
                if (!playback.isUnpaused()) {
                    playback.play()
                }
                //emit(Constants.EVENT.PrevSong)
            }
            Constants.ACTION.PLAYPAUSE -> {
                Timber.i("Clicked Play/Pause")
                val isPlaying = playback.isUnpaused()
                //emit(if (isPlaying) Constants.EVENT.Paused else Constants.EVENT.Playing)
                if (isPlaying) {
                    playback.pause()
                    audioFocusHelper.abandonFocus()
                    wifiLock.release()
                    wakeLock.release()
                } else {
                    playback.play()
                    audioFocusHelper.requestFocus()
                    wifiLock.acquire()
                    wakeLock.acquire()
                }
            }
            Constants.ACTION.NEXT -> {
                Timber.i("Clicked Next")
                playback.nextTrack()
                if (!playback.isUnpaused()) {
                    playback.play()
                }
                //emit(Constants.EVENT.NextSong)
            }
            Constants.ACTION.INSERT_NEXT -> {
                Timber.i("Insert next")
                val bundle = intent.extras
                val tracks = Parcels.unwrap<List<ParcelableTrack>>(
                        bundle.getParcelable<Parcelable>(Constants.KEY.TRACKS))
                playback.addNext(tracks)
            }
            Constants.ACTION.INSERT_AT_END -> {
                Timber.i("Insert at end")
                val bundle = intent.extras
                val tracks = Parcels.unwrap<List<ParcelableTrack>>(
                        bundle.getParcelable<Parcelable>(Constants.KEY.TRACKS))
                playback.addTracks(tracks)
            }
            Constants.ACTION.REORDER -> {
                val bundle = intent.extras

                val from = bundle.getInt(Constants.KEY.REORDER_FROM)
                val to = bundle.getInt(Constants.KEY.REORDER_TO)

                Timber.i("Reorder %d %d", from, to)

                playback.reorder(from, to)
            }
            Constants.ACTION.REMOVE -> {
                val bundle = intent.extras

                val pos = bundle.getInt(Constants.KEY.POSITION)

                Timber.i("Remove %d", pos)

                playback.remove(pos)
            }
            Constants.ACTION.SEND_QUEUE -> {
                Timber.i("Sending Queue")
                emit(Constants.EVENT.SendQueue)
                Timber.i("Received Stop Foreground Intent")
                stopForeground(true)
                stopSelf()
            }
            Constants.ACTION.SEND_STATE -> {
                Timber.i("Will send service state")
                sendStateOnRegistered = true
            }
            Constants.ACTION.STOP_SERVICE -> {
                Timber.i("Received Stop Foreground Intent")
                stopForeground(true)
                stopSelf()
            }
        }
        return Service.START_NOT_STICKY
    }

    private fun initService(intent: Intent) {

        wifiLock = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .createWifiLock(WifiManager.WIFI_MODE_FULL, TAG)
        wifiLock.setReferenceCounted(false)

        wakeLock = (applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        wakeLock.setReferenceCounted(false)

        notificationMgr = NotificationManagerCompat.from(this)

        notificationIntent = newIntent<MainActivity> {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        previousIntent = newIntent<MusicPlayerService>(Constants.ACTION.PREVIOUS)
        playPauseIntent = newIntent<MusicPlayerService>(Constants.ACTION.PLAYPAUSE)
        nextIntent = newIntent<MusicPlayerService>(Constants.ACTION.NEXT)

        val bundle = intent.extras
        val tracks = Parcels.unwrap<List<ParcelableTrack>>(
                bundle.getParcelable<Parcelable>(Constants.KEY.TRACKS))

        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO)

        playback = MusicPlayback(this, { playState ->
            when (playState) {
                MusicPlayback.PlayState.NOT_PLAYING -> {
                    audioFocusHelper.abandonFocus()
                    if (wifiLock.isHeld) wifiLock.release()
                    if (wakeLock.isHeld) wakeLock.release()
                    mediaSession.setPlaybackState(
                            stateBuilder.setState(PlaybackStateCompat.STATE_NONE,
                                    playback.getCurrentPosition(), 1.0f).build())
                    emit(Constants.EVENT.Paused)
                }
                MusicPlayback.PlayState.STARTING -> {
                    if (!wifiLock.isHeld) wifiLock.acquire()
                    if (!wakeLock.isHeld) wakeLock.acquire()
                    mediaSession.setPlaybackState(
                            stateBuilder.setState(PlaybackStateCompat.STATE_CONNECTING,
                                    playback.getCurrentPosition(), 1.0f).build())
                }
                MusicPlayback.PlayState.BUFFERING -> {
                    if (!wifiLock.isHeld) wifiLock.acquire()
                    if (!wakeLock.isHeld) wakeLock.acquire()
                    mediaSession.setPlaybackState(
                            stateBuilder.setState(PlaybackStateCompat.STATE_BUFFERING,
                                    playback.getCurrentPosition(), 1.0f).build())

                    emit(Constants.EVENT.Buffering)
                }
                MusicPlayback.PlayState.PLAYING -> {
                    if (!wifiLock.isHeld) wifiLock.acquire()
                    if (!wakeLock.isHeld) wakeLock.acquire()
                    mediaSession.setPlaybackState(
                            stateBuilder.setState(if (playback.isUnpaused())
                                PlaybackStateCompat.STATE_PLAYING else
                                PlaybackStateCompat.STATE_PAUSED,
                                    playback.getCurrentPosition(), 1.0f).build())

                    emit(if (playback.isUnpaused()) Constants.EVENT.Playing else Constants.EVENT.Paused)
                }
                MusicPlayback.PlayState.CONTINUE -> { // From position change
                    mediaSession.setPlaybackState(stateBuilder.setState(if (playback.isUnpaused())
                        PlaybackStateCompat.STATE_PLAYING else
                        PlaybackStateCompat.STATE_PAUSED,
                            playback.getCurrentPosition(), 1.0f)
                            .setBufferedPosition(bufferedPosition).build())
                }
            }
            lastNotifiedTrack?.let {
                updateNotification(it, lastBitmap)
            }
        }, { num, track ->
            // When the track is changed
            emit(Constants.EVENT.TrackPlaying, track)
            loadImageAndDoUpdates(track)
        }, { bufferProgress ->
            bufferedPosition = (lastNotifiedTrack?.durationMillis?.toFloat())?.times(bufferProgress)?.toLong() ?: 0
            emit(Constants.EVENT.BufferProgress, bufferProgress)
        }, { error ->
            emit(Constants.EVENT.Error, error)
        })

        playback.setup()

        audioFocusHelper = AudioFocusHelper(this, object : MusicFocusable {
            override fun onGainedAudioFocus() {
                audioFocus = AudioFocus.Focused
                if (playback.wasPausedFromFocusLoss) {
                    if (!wifiLock.isHeld) wifiLock.acquire()
                    if (!wakeLock.isHeld) wakeLock.acquire()
                    playback.play()
                } else if (playback.isDucked()) {
                    playback.unsetDucked()
                }
            }

            override fun onLostAudioFocus(canDuck: Boolean) {
                audioFocus = if (canDuck) AudioFocus.NoFocusCanDuck else AudioFocus.NoFocusNoDuck
                if (canDuck) {
                    playback.setDucked()
                } else {
                    playback.wasPausedFromFocusLoss = true
                    playback.pause()
                    if (wifiLock.isHeld) wifiLock.release()
                    if (wakeLock.isHeld) wakeLock.release()
                }
            }
        })
        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.setCallback(mediaSessionCallback)

        Timber.d("Will call fromNewQueue with position ${intent.extras.getInt(Constants.KEY.POSITION)}")

        fromNewQueue(tracks, intent.extras.getInt(Constants.KEY.POSITION))
    }

    private fun fromNewQueue(tracks: List<ParcelableTrack>, pos: Int) {
        playback.newQueue(tracks, pos)
        audioFocusHelper.requestFocus()
        wifiLock.acquire()
        playback.seekTo(0)
        playback.play()
    }

    private fun loadImageAndDoUpdates(track: ITrack) {

        val metadata = (track as? ParcelableTrack)?.mediaMetadata ?:
            (track as? Track)?.mediaMetadata

        Timber.i("loadImageBitmap")

        loadImageBitmap(track.albumArtURL ?: "") { img ->
            if (img == null) {
                if(metadata != null) mediaSession.setMetadata(metadata)
                updateNotification(track)
            } else {
                updateNotification(track, bitmap = img)
                if(metadata !=  null) mediaSession.setMetadata(MediaMetadataCompat.Builder(metadata)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, img)
                        .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, img)
                        .build())
            }
        }

        mediaSession.isActive = true
    }

    private fun updateNotification(track: ITrack, bitmap: Bitmap? = null) {

        lastNotifiedTrack = track
        lastBitmap = bitmap

        val builder = NotificationCompat.Builder(this, "Default")

        val contentIntent = pendingActivityIntent(notificationIntent)
        val ppreviousIntent = pendingServiceIntent(previousIntent)
        val pplayPauseIntent = pendingServiceIntent(playPauseIntent)
        val pnextIntent = pendingServiceIntent(nextIntent)

        builder.addAction(R.drawable.ic_fast_rewind, "Previous Track", ppreviousIntent)

        val playLabel = if (playback.isUnpaused()) "Pause" else "Play"
        val playIcon = if (playback.isUnpaused()) R.drawable.ic_pause else R.drawable.ic_play

        builder.addAction(NotificationCompat.Action(playIcon, playLabel, pplayPauseIntent))

        builder.addAction(R.drawable.ic_fast_forward, "Next Track", pnextIntent)

        mediaSession.setSessionActivity(contentIntent)

        builder.setStyle(
                android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowCancelButton(true)
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.sessionToken))
                .setChannelId("default")
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_play)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(track.title)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(playback.isUnpaused())
                .setContentText(track.artist)
                .setColorized(true)
                .setShowWhen(false)
                .apply { bitmap?.let { setLargeIcon(it) } }

        if (!initialized && playback.isUnpaused()) {
            startForeground(Constants.ID.MUSIC_PLAYER_SERVICE,
                    builder.build())
            initialized = true
        } else {
            notificationMgr.notify(Constants.ID.MUSIC_PLAYER_SERVICE,
                    builder.build())
        }
    }

    private fun emit(e: Int) {
        when (e) {
            Constants.EVENT.SendQueue -> emit(e, playback.getQueue())
            else -> emit(e, null)
        }

    }

    private fun emit(e: Int, obj: Any?, recurse: Boolean = false) {
        //Timber.d("Emitting %d to %d clients", e, clients.size)
        if (clients.size == 0) {
            messageQueue.add(Pair(e, obj))
        } else {
            if (!recurse) {
                messageQueue.forEach { (first, second) -> emit(first, second, true) }
                messageQueue.clear()
            }
        }
        for (m in clients) {
            try {
                if (obj == null)
                    m.send(Message.obtain(null, e))
                else
                    m.send(Message.obtain(null, e, obj))
            } catch (exception: RemoteException) {
                /* The client must've died */
                clients.remove(m)
            }

        }
    }

    override fun onGainedAudioFocus() {
        if (playback.isDucked())
            playback.unsetDucked()
        else if (playback.wasPausedFromFocusLoss) {
            playback.wasPausedFromFocusLoss = false
            playback.play()
        }
    }

    override fun onLostAudioFocus(canDuck: Boolean) {
        if (canDuck)
            playback.setDucked()
        else {
            playback.wasPausedFromFocusLoss = true
            playback.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("In onDestroy")
    }

    override fun onBind(intent: Intent): IBinder? {
        return messenger.binder
    }

    @SuppressLint("HandlerLeak")
    internal inner class IncomingHandler : Handler() { // Handler of incoming messages from clients.
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MESSAGE.REGISTER_CLIENT -> {
                    Timber.d("Registering 1 client")
                    clients.add(msg.replyTo)
                    if (sendStateOnRegistered) {
                        sendStateOnRegistered = false
                        Timber.d("Sending service state")
                        emit(Constants.EVENT.SendQueue)
                        emit(Constants.EVENT.TrackPlaying, lastNotifiedTrack)
                        emit(if (playback.isUnpaused()) Constants.EVENT.Playing else
                            Constants.EVENT.Paused)
                    }
                }
                Constants.MESSAGE.UNREGISTER_CLIENT -> {
                    Timber.d("Unregistering 1 client")
                    clients.remove(msg.replyTo)
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private var mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() = playback.play()

        override fun onPause() = playback.pause()

        override fun onSkipToNext() = playback.nextTrack()

        override fun onSkipToPrevious() = playback.prevTrack()

        override fun onSeekTo(pos: Long) = playback.seekTo(pos)

    }

    companion object {
        const val TAG = "CarbonMplayService"
    }

}