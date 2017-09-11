package com.carbonplayer.audio

import android.app.Service
import android.net.Uri
import android.os.Handler
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.ParcelableMusicTrack
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.network.StreamManager
import com.carbonplayer.model.network.entity.ExoPlayerDataSource
import com.carbonplayer.model.network.entity.StreamingContent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.*

class MusicPlaybackImpl (
        val service: Service,
        val playback: MusicPlayback
) : Player.EventListener {

    val mainHandler: Handler = Handler(service.mainLooper)
    val streamManager: StreamManager = StreamManager.getInstance(service)

    var renderersFactory = DefaultRenderersFactory(service)
    val allocator = DefaultAllocator(true, 64 * 1024)
    val loadControl = DefaultLoadControl(allocator, 3000, 4500, 1000, 5000)
    val dataSourceFactory = buildDataSourceFactory(true)
    val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
    val trackSelector = DefaultTrackSelector(trackSelectionFactory)
    val dynamicSource = DynamicConcatenatingMediaSource()

    val mirroredQueue = LinkedList<ParcelableMusicTrack>()
    var trackNum = 0
    val trackPlaying = 0

    val playerIsPrepared = false

    var preparingTrack = false

    lateinit var exoPlayer: SimpleExoPlayer

    lateinit var loop: Runnable

    fun setup() {
        exoPlayer = ExoPlayerFactory
                .newSimpleInstance(renderersFactory, trackSelector, loadControl)

        exoPlayer.addListener(this)
        loop = Runnable {
            execLoop()
            mainHandler.postDelayed(loop, 83)
        }
        mainHandler.postDelayed(loop, 83)
    }

    fun pause() {
        Timber.d("Pausing playback")
        if(exoPlayer.playWhenReady) exoPlayer.playWhenReady = false
    }

    fun play() {
        Timber.d("Playing")
        if(!exoPlayer.playWhenReady) exoPlayer.playWhenReady = true
    }

    fun newQueue(queue: List<ParcelableMusicTrack>) {
        trackNum = 0
        val i = 0; while (i < dynamicSource.size) {
            dynamicSource.removeMediaSource(i)
        }
        mirroredQueue.clear()
        queue.forEach { add(it) }
    }

    fun add(track: ParcelableMusicTrack) {
        mirroredQueue.add(track)
    }

    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        return (service.application as CarbonPlayerApplication)
                .buildDataSourceFactory(if (useBandwidthMeter) bandwidthMeter else null)
    }

    private fun execLoop() {
        if(exoPlayer.playWhenReady) {
            // Playing
            if(exoPlayer.playbackState == Player.STATE_IDLE
                    && mirroredQueue.size > 0 && !preparingTrack) {
                preparingTrack = true
                prepareTrack(mirroredQueue[trackNum])
            } else if (exoPlayer.playbackState == Player.STATE_READY
                    && !preparingTrack) {

            }
        }
    }

    private fun prepareTrack(track: ParcelableMusicTrack) {
        val stream = streamManager.getStream(service, SongID(track), track.title)
        preparingTrack = false
        val singleSource = sourceFromStream(stream)
        dynamicSource.addMediaSource(singleSource)
        if(!playerIsPrepared) exoPlayer.prepare(dynamicSource, true, false)

    }

    private fun sourceFromStream(stream: StreamingContent): MediaSource {
        return ExtractorMediaSource(Uri.parse("DefaultUri"),
                DataSource.Factory { ExoPlayerDataSource(stream) },
                DefaultExtractorsFactory(), mainHandler, ExtractorMediaSource.EventListener { e ->
            Timber.e(e, "Error in buildMediaSource() -> extractorMediaSource")
        })
    }

    private fun sourceFromUrl(url: String): MediaSource {
        return ExtractorMediaSource(Uri.parse(url), dataSourceFactory, DefaultExtractorsFactory(),
                mainHandler, ExtractorMediaSource.EventListener { e -> // Error listener

            Timber.e(e, "Error in buildMediaSource() -> extractorMediaSource")
        })
    }


    override fun onPlayerError(error: ExoPlaybackException?) {
        Timber.e(error, "Player error")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when(playbackState) {
            Player.STATE_IDLE -> {
                Timber.d("Playback state: IDLE")
            }
            Player.STATE_BUFFERING -> {
                Timber.d("Playback state: BUFFERING")
            }
            Player.STATE_READY -> {
                Timber.d("Playback state: READY")
            }
            Player.STATE_ENDED -> {
                Timber.d("Playback state: ENDED")
            }
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Timber.d("Loading: "+ if(isLoading) "true" else "false")
    }

    override fun onPositionDiscontinuity() {
        Timber.d("Position Discontinuity")
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
    }

}