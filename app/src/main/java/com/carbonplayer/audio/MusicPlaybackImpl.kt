package com.carbonplayer.audio

import android.app.Service
import android.net.Uri
import android.os.Handler
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.exception.PlaybackException
import com.carbonplayer.model.network.StreamManager
import com.carbonplayer.model.network.entity.ExoPlayerDataSource
import com.carbonplayer.model.network.entity.Stream
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import io.reactivex.disposables.Disposable
import io.realm.Realm
import timber.log.Timber
import java.util.concurrent.TimeUnit

/***
 * This class is what directly handles music playback through ExoPlayer.
 */
class MusicPlaybackImpl(
        val service: Service,
        val playback: MusicPlayback,
        val callback: (MusicPlayback.PlayState) -> Unit,
        val ontrackchanged: (Int, ITrack) -> Unit,
        val onbuffer: (Float) -> Unit,
        val onerror: (PlaybackException) -> Unit
) : Player.EventListener {

    private val mainHandler: Handler = Handler(service.mainLooper)

    private var subscription: Disposable? = null

    private var renderersFactory = DefaultRenderersFactory(service)
    private val allocator = DefaultAllocator(true, 64 * 1024)
    private val loadControl = DefaultLoadControl(allocator, 2000, 4000,
            1000, 1500, C.LENGTH_UNSET, false)

    private val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
    private val trackSelector = DefaultTrackSelector(trackSelectionFactory)
    private val dynamicSource = DynamicConcatenatingMediaSource()

    val mirroredQueue = mutableListOf<ParcelableTrack>()
    private val mirroredContentQueue = mutableListOf<Stream>()
    private var trackNum = 0
    private var lastKnownWindowIndex = 0

    private var disallowNextAutoInc = false

    var inc = 0

    private var playerIsPrepared = false
    private var mirroredPlayState = MusicPlayback.PlayState.NOT_PLAYING

    private lateinit var exoPlayer: SimpleExoPlayer

    private lateinit var loop: Thread

    fun setup() {
        exoPlayer = ExoPlayerFactory
                .newSimpleInstance(renderersFactory, trackSelector, loadControl)

        exoPlayer.addListener(this)

        // This is probably not a good idea
        loop = Thread(Runnable {
            Timber.i("Thread created")

            while (true) {
                execLoop()
                Thread.sleep(83)
            }

        })

        loop.start()
    }

    fun isUnpaused(): Boolean {
        return exoPlayer.playWhenReady
    }

    fun pause() {
        Timber.d("Pausing playback")
        if (exoPlayer.playWhenReady) exoPlayer.playWhenReady = false
    }

    fun play() {
        Timber.d("Playing")
        if (!playerIsPrepared) {
            exoPlayer.prepare(dynamicSource)
            playerIsPrepared = true
            exoPlayer.seekTo(trackNum, 0L)
        }
        if (!loop.isAlive) loop.start()
        if (!exoPlayer.playWhenReady) exoPlayer.playWhenReady = true
    }


    @Synchronized
    fun newQueue(queue: List<ParcelableTrack>, track: Int = 0, initFirst: Boolean = true) {
        Timber.d("newQueue, initFirst: $initFirst")
        trackNum = track
        while (0 < dynamicSource.size) {
            dynamicSource.removeMediaSource(0)
        }
        mirroredQueue.clear()
        mirroredContentQueue.clear()
        add(queue, track, initFirst)

        ontrackchanged(trackNum, mirroredQueue[trackNum])
        playerIsPrepared = false
    }

    fun skipToTrack(index: Int) {
        Timber.d("skipToTrack $index")
        if (mirroredQueue.size > index) {
            var todo = { onbuffer(1f) }

            if (!mirroredContentQueue[index].downloadInitialized) {
                mirroredContentQueue[index].initDownload()
                todo = {
                    subscription?.dispose()
                    subscription = mirroredContentQueue[index].progressMonitor()
                            .subscribe({ b -> onbuffer(b) })
                }
            }

            disallowNextAutoInc = true
            exoPlayer.seekTo(index, 0L)
            trackNum = index

            ontrackchanged(index, mirroredQueue[index])
            todo()
        }
    }

    fun nextTrack() {
        if (mirroredQueue.size > trackNum + 1) {

            // Initialize download for the next track
            if (!mirroredContentQueue[trackNum + 1].downloadInitialized) {
                mirroredContentQueue[trackNum + 1].initDownload()
            }

            // This is not an automatic next track event
            disallowNextAutoInc = true

            exoPlayer.seekTo(trackNum + 1, 0L)
            trackNum++

            ontrackchanged(trackNum, mirroredQueue[trackNum])
        }
    }

    fun prevTrack(alwaysSkip: Boolean = false) {
        // If we are greater than SKIP_ON_PREVIOUS milliseconds into the current track,
        // we want to restart from the beginning of the current track
        if (!alwaysSkip && exoPlayer.currentPosition > SKIP_ON_PREVIOUS) {
            exoPlayer.seekTo(0L)
        } else if (trackNum > 0) {
            // Otherwise we want to move to the previous track
            if (!mirroredContentQueue[trackNum - 1].downloadInitialized) {
                mirroredContentQueue[trackNum - 1].initDownload()
            }

            exoPlayer.seekTo(trackNum - 1, 0L)
            trackNum--
            ontrackchanged(trackNum, mirroredQueue[trackNum])

            // This is not an automatic track change
            disallowNextAutoInc = true
        }

    }

    fun seekTo(pos: Long) {
        if (mirroredQueue[trackNum].durationMillis >= pos)
            exoPlayer.seekTo(pos)
    }


    @Synchronized
    fun add(tracks: List<ParcelableTrack>, track: Int = 0, downloadFirst: Boolean = false) {
        Timber.d("add tracks, downloadFirst: $downloadFirst")

        mirroredQueue.addAll(tracks)

        val sources = MutableList(tracks.size, { z ->
            val sourcePair = sourceFromTrack(tracks[z])
            if (z == track && downloadFirst && !sourcePair.first.isDownloaded) {
                sourcePair.first.initDownload()
            }
            mirroredContentQueue.add(sourcePair.first)
            sourcePair.second
        })
        subscription?.dispose()
        subscription = mirroredContentQueue[trackNum].progressMonitor()
                .subscribe({ b -> onbuffer(b) })

        dynamicSource.addMediaSources(sources)

        subscription?.dispose()
        subscription = mirroredContentQueue[trackNum].progressMonitor()
                .subscribe({ b -> onbuffer(b) })

        /*(maxOf(0, trackNum-1)..minOf(trackNum+2, mirroredQueue.size-1)).forEach {
            directAdd(mirroredQueue[it])
        }*/

        if (!loop.isAlive) loop.start()
    }

    /*private fun directAdd(track: ParcelableTrack) {
        var xtrack: Track? = null

        if(track.localId == null)
            Realm.getDefaultInstance().executeTransaction{ rlm ->
                xtrack = MusicLibrary.addOneToDatabase(rlm, track, true, track.inLibrary)
            }
        else {
            xtrack = Realm.getDefaultInstance().where(Track::class.java)
                    .equalTo(Track.LOCAL_ID, track.localId)
                    .findFirst()
        }

        if(xtrack != null) { // Should always be true
            val sourcePair = sourceFromTrack(xtrack!!)
            mirroredContentQueue.add(sourcePair.first)
            dynamicSource.addMediaSource(sourcePair.second)
        } else {
            Timber.e("directAdd(): Local track ${track.localId} MUST exist")
        }
    }*/

    fun addNext(tracks: List<ParcelableTrack>) {
        mirroredQueue.addAll(trackNum + 1, tracks)
        val sources = MutableList(tracks.size, { z ->
            sourceFromTrack(tracks[z])
        })
        mirroredContentQueue.addAll(trackNum + 1, sources.map { it.first })
        dynamicSource.addMediaSources(trackNum + 1, sources.map { it.second })
    }

    fun remove(index: Int) {
        mirroredQueue.removeAt(index)
        mirroredContentQueue.removeAt(index)
        dynamicSource.removeMediaSource(index)
    }

    fun reorder(from: Int, to: Int) {
        val ogTrack = mirroredQueue[from]
        val ogContent = mirroredContentQueue[from]
        mirroredQueue.removeAt(from)
        mirroredQueue.add(to, ogTrack)
        mirroredContentQueue.removeAt(from)
        mirroredContentQueue.add(to, ogContent)
        dynamicSource.moveMediaSource(from, to)
    }

    fun setDucked() {
        exoPlayer.volume = 0.3f
    }

    fun unsetDucked() {
        exoPlayer.volume = 1.0f
    }

    fun isDucked(): Boolean {
        return exoPlayer.volume > 0.5f
    }

    fun getCurrentPosition() = exoPlayer.currentPosition

    @Synchronized
    private fun execLoop() {
        if (exoPlayer.playWhenReady) {
            // Playing
            if (exoPlayer.playbackState == Player.STATE_READY) {

                lastKnownWindowIndex = exoPlayer.currentWindowIndex

                if (inc++ % 100 == 0) {
                    Timber.d("Position: %s", exoPlayer.currentPosition)
                }

                if(inc % 5 == 0) callback(MusicPlayback.PlayState.CONTINUE)

                if (exoPlayer.currentPosition > DELAY_ADD_ITEM &&
                        mirroredQueue.size > trackNum + 1
                        && !mirroredContentQueue[trackNum + 1].downloadInitialized) {
                    Timber.i("Init download for next track")
                    mirroredContentQueue[trackNum + 1].initDownload()
                }
            }
        }
    }

    private fun sourceFromTrack(track: ITrack): Pair<Stream, MediaSource> {
        Timber.d("sourceFromTrack $track")
        val stream = StreamManager.getStream(service, track, false)

        return Pair(stream, sourceFromStream(stream))
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
        Timber.d("Timeline changed")
    }

    private fun sourceFromStream(stream: Stream): MediaSource {
        Timber.d("sourceFromStream $stream")
        return ExtractorMediaSource.Factory( { ExoPlayerDataSource(stream) })
                .createMediaSource(Uri.parse("DefaultUri"))
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        Timber.e(error, "Player error")
        onerror(PlaybackException())
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        var newState = MusicPlayback.PlayState.NOT_PLAYING

        when (playbackState) {
            Player.STATE_IDLE -> {
                Timber.d("Playback state: IDLE")
                if (playWhenReady) newState = MusicPlayback.PlayState.STARTING
            }
            Player.STATE_BUFFERING -> {
                Timber.d("Playback state: BUFFERING")
                newState = MusicPlayback.PlayState.BUFFERING
            }
            Player.STATE_READY -> {
                Timber.d("Playback state: READY")
                if (playWhenReady) newState = MusicPlayback.PlayState.PLAYING
            }
            Player.STATE_ENDED -> {
                Timber.d("Playback state: ENDED")
            }
        }

        if (newState != mirroredPlayState) {
            mirroredPlayState = newState
        }

        callback(newState)
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Timber.d("Loading: " + if (isLoading) "true" else "false")
    }

    override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
        Timber.d("Position Discontinuity: $reason")

        if (exoPlayer.currentWindowIndex == lastKnownWindowIndex + 1 && !disallowNextAutoInc) {
            trackNum++
            Timber.i("Discontinuity -> Next Track")
            ontrackchanged(trackNum, mirroredQueue[trackNum])
            if (mirroredContentQueue[trackNum].isDownloaded) {
                onbuffer(1.0f)
            } else {
                subscription?.dispose()
                subscription = mirroredContentQueue[trackNum].progressMonitor()
                        .subscribe({ b -> onbuffer(b) })
            }
            callback(MusicPlayback.PlayState.CONTINUE)
        }

        if (disallowNextAutoInc) disallowNextAutoInc = false
    }


    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        Timber.d("Playback parameters changed")
    }

    override fun onTracksChanged(groups: TrackGroupArray?, selections: TrackSelectionArray?) {
        Timber.d("Tracks changed")
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        Timber.d("Repeat mode changed")
    }

    override fun onSeekProcessed() {
        Timber.d("Seek processed")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        Timber.d("Shuffle mode: $shuffleModeEnabled")
    }

    companion object {
        val bandwidthMeter = DefaultBandwidthMeter()

        //Start downloading the next track 15 seconds into the current one
        val DELAY_ADD_ITEM = TimeUnit.SECONDS.toMillis(15)

        val SKIP_ON_PREVIOUS = TimeUnit.SECONDS.toMillis(3) // 4 or 3?
    }

}