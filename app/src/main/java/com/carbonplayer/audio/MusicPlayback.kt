package com.carbonplayer.audio

import android.app.Service
import com.carbonplayer.model.entity.ParcelableMusicTrack

class MusicPlayback (val service: Service){

    val playbackImpl: MusicPlaybackImpl = MusicPlaybackImpl(service, this)

    fun setup() {
        playbackImpl.setup()
    }

    fun newQueue(queue: List<ParcelableMusicTrack>) {
        playbackImpl.newQueue(queue)
    }

    fun addTrack(track: ParcelableMusicTrack) {
        playbackImpl.add(track)
    }

    fun pause() {
        playbackImpl.pause()
    }

    fun play() {
        playbackImpl.play()
    }

}