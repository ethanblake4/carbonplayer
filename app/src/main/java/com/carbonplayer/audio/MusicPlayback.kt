package com.carbonplayer.audio

import android.app.Service
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.exception.PlaybackException

class MusicPlayback(
        service: Service,
        callback: (PlayState) -> Unit,
        trackCb: (Int, ITrack) -> Unit,
        bufferCb: (Float) -> Unit,
        error: (PlaybackException) -> Unit
) {

    enum class PlayState {
        NOT_PLAYING,
        STARTING,
        BUFFERING,
        PLAYING,
        CONTINUE
    }

    var wasPausedFromFocusLoss = false

    private val playbackImpl: MusicPlaybackImpl =
            MusicPlaybackImpl(service, this, callback, trackCb, bufferCb, error)

    fun setup() = playbackImpl.setup()

    fun newQueue(queue: List<ParcelableTrack>, track: Int = 0) = playbackImpl.newQueue(queue, track)

    fun getQueue() = playbackImpl.mirroredQueue

    fun addTracks(tracks: List<ParcelableTrack>) = playbackImpl.add(tracks)

    fun addNext(tracks: List<ParcelableTrack>) = playbackImpl.addNext(tracks)

    fun skipToTrack(index: Int) = playbackImpl.skipToTrack(index)

    fun nextTrack() = playbackImpl.nextTrack()

    fun prevTrack() = playbackImpl.prevTrack()

    fun seekTo(pos: Long) = playbackImpl.seekTo(pos)

    fun reorder(from: Int, to: Int) = playbackImpl.reorder(from, to)

    fun remove(pos: Int) = playbackImpl.remove(pos)

    fun getCurrentPosition() = playbackImpl.getCurrentPosition()

    fun pause() = playbackImpl.pause()

    fun play() = playbackImpl.play()

    fun isUnpaused(): Boolean = playbackImpl.isUnpaused()

    fun setDucked() = playbackImpl.setDucked()

    fun unsetDucked() = playbackImpl.unsetDucked()

    fun isDucked(): Boolean = playbackImpl.isDucked()

}