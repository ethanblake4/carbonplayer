package com.carbonplayer.audio

import android.app.Service
import android.net.Uri
import android.os.Handler
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.ParcelableMusicTrack
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.entity.exception.PlaybackException
import com.carbonplayer.model.network.StreamManager
import com.carbonplayer.model.network.entity.ExoPlayerDataSource
import com.carbonplayer.model.network.entity.StreamingContent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import rx.Subscription
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MusicPlaybackImpl(
        val service: Service,
        val playback: MusicPlayback,
        val callback: (MusicPlayback.PlayState) -> Unit,
        val ontrackchanged: (Int, ParcelableMusicTrack) -> Unit,
        val onbuffer: (Float) -> Unit,
        val onerror: (PlaybackException) -> Unit
) : Player.EventListener {

    val mainHandler: Handler = Handler(service.mainLooper)
    val streamManager: StreamManager = StreamManager.getInstance()

    var subscription: Subscription? = null

    var renderersFactory = DefaultRenderersFactory(service)
    val allocator = DefaultAllocator(true, 64 * 1024)
    val loadControl = DefaultLoadControl(allocator, 6000, 10000, 2000, 5000)

    val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
    val trackSelector = DefaultTrackSelector(trackSelectionFactory)
    val dynamicSource = DynamicConcatenatingMediaSource()

    val mirroredQueue = mutableListOf<ParcelableMusicTrack>()
    val mirroredContentQueue = mutableListOf<StreamingContent>()
    var trackNum = 0
    var lastKnownWindowIndex = 0

    var disallowNextAutoInc = false

    var inc = 0

    var playerIsPrepared = false
    var mirroredPlayState = MusicPlayback.PlayState.NOT_PLAYING

    lateinit var exoPlayer: SimpleExoPlayer

    lateinit var loop: Thread

    fun setup() {
        exoPlayer = ExoPlayerFactory
                .newSimpleInstance(renderersFactory, trackSelector, loadControl)

        exoPlayer.addListener(this)
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
        }
        if (!exoPlayer.playWhenReady) exoPlayer.playWhenReady = true
    }

    fun newQueue(queue: List<ParcelableMusicTrack>) {
        trackNum = 0
        val i = 0; while (i < dynamicSource.size) {
            dynamicSource.removeMediaSource(i)
        }
        mirroredQueue.clear()
        mirroredContentQueue.clear()
        add(queue, true)
        ontrackchanged(trackNum, mirroredQueue[trackNum])
        playerIsPrepared = false
    }

    fun skipToTrack(index: Int) {
        if (mirroredQueue.size > index) {
            var todo = { onbuffer(1f) }

            if (!mirroredContentQueue[index].downloadInitialized) {
                mirroredContentQueue[index].initDownload()
                todo = {
                    subscription?.unsubscribe()
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
            if (!mirroredContentQueue[trackNum + 1].downloadInitialized) {
                mirroredContentQueue[trackNum + 1].initDownload()
            }
            disallowNextAutoInc = true
            exoPlayer.seekTo(trackNum + 1, 0L)
            trackNum++

            ontrackchanged(trackNum, mirroredQueue[trackNum])
        }
    }

    fun prevTrack(alwaysSkip: Boolean = false) {
        if (!alwaysSkip && exoPlayer.currentPosition > SKIP_ON_PREVIOUS) {
            exoPlayer.seekTo(0L)
        } else if (trackNum > 0) {
            if (!mirroredContentQueue[trackNum - 1].downloadInitialized) {
                mirroredContentQueue[trackNum - 1].initDownload()
            }

            exoPlayer.seekTo(trackNum - 1, 0L)
            trackNum--
            ontrackchanged(trackNum, mirroredQueue[trackNum])

            disallowNextAutoInc = true
        }

    }

    fun seekTo(pos: Long) {
        if (mirroredQueue[trackNum].durationMillis >= pos)
            exoPlayer.seekTo(pos)
    }


    fun add(tracks: List<ParcelableMusicTrack>, downloadFirst: Boolean = false) {
        mirroredQueue.addAll(tracks)
        val sources = MutableList(tracks.size, { z ->
            val sourcePair = sourceFromTrack(tracks[z])
            if (z == 0 && downloadFirst && !sourcePair.first.isDownloaded) {
                sourcePair.first.initDownload()
            }
            mirroredContentQueue.add(sourcePair.first)
            sourcePair.second
        })
        subscription?.unsubscribe()
        subscription = mirroredContentQueue[trackNum].progressMonitor()
                .subscribe({ b -> onbuffer(b) })

        dynamicSource.addMediaSources(sources)
    }

    fun addNext(tracks: List<ParcelableMusicTrack>) {
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

    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        return (service.application as CarbonPlayerApplication)
                .buildDataSourceFactory(if (useBandwidthMeter) bandwidthMeter else null)
    }

    private fun execLoop() {
        if (exoPlayer.playWhenReady) {
            // Playing
            if (exoPlayer.playbackState == Player.STATE_READY) {

                lastKnownWindowIndex = exoPlayer.currentWindowIndex

                if (inc++ % 100 == 0) {
                    Timber.d("Position: %s", exoPlayer.currentPosition)
                }

                if (exoPlayer.currentPosition > DELAY_ADD_ITEM &&
                        mirroredQueue.size > trackNum + 1
                        && !mirroredContentQueue[trackNum + 1].downloadInitialized) {
                    Timber.i("Init download for next track")
                    mirroredContentQueue[trackNum + 1].initDownload()
                }
            }
        }
    }

    private fun sourceFromTrack(track: ParcelableMusicTrack): Pair<StreamingContent, MediaSource> {
        val stream = streamManager.getStream(service, SongID(track), track.title, false)
        return Pair(stream, sourceFromStream(stream))
    }

    /*private fun prepareTrack(track: ParcelableMusicTrack) {
        preparingTrack = false
        dynamicSource.addMediaSource(sourceFromTrack(track))
        if(!playerIsPrepared) exoPlayer.prepare(dynamicSource, true, false)
    }*/

    private fun sourceFromStream(stream: StreamingContent): MediaSource {
        return ExtractorMediaSource(Uri.parse("DefaultUri"),
                DataSource.Factory { ExoPlayerDataSource(stream) },
                DefaultExtractorsFactory(), mainHandler, ExtractorMediaSource.EventListener { e ->
            Timber.e(e, "Error in buildMediaSource() -> extractorMediaSource")
            onerror(PlaybackException())
        })
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

    override fun onPositionDiscontinuity() {
        Timber.d("Position Discontinuity")
        if (exoPlayer.currentWindowIndex == lastKnownWindowIndex + 1 && !disallowNextAutoInc) {
            trackNum++
            Timber.i("Discontinuity -> Next Track")
            ontrackchanged(trackNum, mirroredQueue[trackNum])
            if (mirroredContentQueue[trackNum].isDownloaded) {
                onbuffer(1.0f)
            } else {
                subscription?.unsubscribe()
                subscription = mirroredContentQueue[trackNum].progressMonitor()
                        .subscribe({ b -> onbuffer(b) })
            }
        }

        if (disallowNextAutoInc) disallowNextAutoInc = false
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
        Timber.d("Timeline changed")
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

    companion object {
        val bandwidthMeter = DefaultBandwidthMeter()
        val DELAY_ADD_ITEM = TimeUnit.SECONDS.toMillis(15)
        val SKIP_ON_PREVIOUS = TimeUnit.SECONDS.toMillis(3)
    }

}