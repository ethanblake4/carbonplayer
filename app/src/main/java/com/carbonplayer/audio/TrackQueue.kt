package com.carbonplayer.audio


import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.entity.ParcelableMusicTrack
import com.carbonplayer.utils.parcelable

/**
 * Track queue which mirrors data in [MusicPlayerService]
 */
class TrackQueue(val callback: TrackQueueCallback) {

    private var queue = mutableListOf<MusicTrack>()
    var position: Int = 0

    fun replace(tracks: List<MusicTrack>, pos: Int, noCallback: Boolean = false) {
        queue.clear()
        queue.addAll(tracks)
        position = pos
        if (!noCallback) callback.replace(parcelable, pos)
    }

    val parcelable: MutableList<ParcelableMusicTrack>
        get() = MutableList(queue.size, { i -> queue[i].parcelable() })

    val size: Int get() = queue.size

    fun insertAtEnd(tracks: List<MusicTrack>, noCallback: Boolean = false) {
        queue.addAll(tracks)
        if (!noCallback) callback.insertAtEnd(tracks.parcelable())
    }

    fun insertNext(tracks: List<MusicTrack>, noCallback: Boolean = false) {
        queue.addAll(position, tracks)
        if (!noCallback) callback.insertNext(tracks.parcelable())
    }

    fun reorder(pos: Int, pnew: Int, noCallback: Boolean = false) {
        val t = queue[pos]
        queue.removeAt(pos)
        queue.add(pnew, t)
        if (!noCallback) callback.reorder(pos, pnew)
    }


    fun prevtrack() {
        position--
    }

    fun nexttrack() {
        position++
    }

    fun currentTrack(): MusicTrack {
        return queue[position]
    }

    interface TrackQueueCallback {
        fun replace(tracks: MutableList<ParcelableMusicTrack>, pos: Int)
        fun insertAtEnd(tracks: MutableList<ParcelableMusicTrack>)
        fun insertNext(tracks: MutableList<ParcelableMusicTrack>)
        fun reorder(pos: Int, pnew: Int)
    }
}
