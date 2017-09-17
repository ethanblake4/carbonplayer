package com.carbonplayer.audio

import android.app.Service
import com.carbonplayer.model.entity.ParcelableMusicTrack
import com.carbonplayer.model.entity.exception.PlaybackException

class MusicPlayback (
        service: Service,
        callback: (PlayState) -> Unit,
        trackCb: (Int, ParcelableMusicTrack) -> Unit,
        bufferCb: (Float) -> Unit,
        error: (PlaybackException) -> Unit
) {

    enum class PlayState {
        NOT_PLAYING,
        STARTING,
        BUFFERING,
        PLAYING
    }

    var wasPausedFromFocusLoss = false

    private val playbackImpl: MusicPlaybackImpl =
            MusicPlaybackImpl(service, this, callback, trackCb, bufferCb, error)

    fun setup() = playbackImpl.setup()

    fun newQueue(queue: List<ParcelableMusicTrack>) = playbackImpl.newQueue(queue)

    fun getQueue() = playbackImpl.mirroredQueue

    fun addTracks(tracks: List<ParcelableMusicTrack>) = playbackImpl.add(tracks)

    fun addNext(tracks: List<ParcelableMusicTrack>) = playbackImpl.addNext(tracks)

    fun skipToTrack(index: Int) = playbackImpl.skipToTrack(index)

    fun nextTrack() = playbackImpl.nextTrack()

    fun prevTrack() = playbackImpl.prevTrack()

    fun seekTo(pos: Long) = playbackImpl.seekTo(pos)

    fun reorder(from: Int, to: Int) = playbackImpl.reorder(from, to)

    fun getCurrentPosition() = playbackImpl.getCurrentPosition()

    fun pause() = playbackImpl.pause()

    fun play() = playbackImpl.play()

    fun isUnpaused(): Boolean = playbackImpl.isUnpaused()

    fun setDucked() = playbackImpl.setDucked()

    fun unsetDucked() = playbackImpl.unsetDucked()

    fun isDucked(): Boolean = playbackImpl.isDucked()

}