package com.carbonplayer.audio


import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.utils.parcelable

/**
 * Track queue which mirrors data in [MusicPlayerService]
 */
class TrackQueue(val callback: TrackQueueCallback) {

    private var queue = mutableListOf<ITrack>()
    var position: Int = 0

    fun replace(tracks: List<ITrack>, pos: Int, noCallback: Boolean = false) {
        queue.clear()
        queue.addAll(tracks)
        position = pos
        if (!noCallback) callback.replace(parcelable, pos)
    }

    val parcelable: MutableList<ParcelableTrack>
        get() = MutableList(queue.size, { i -> queue[i].parcelable() })

    val size: Int get() = queue.size

    fun insertAtEnd(tracks: List<ITrack>, noCallback: Boolean = false) {
        queue.addAll(tracks)
        if (!noCallback) callback.insertAtEnd(tracks.parcelable())
    }

    fun insertNext(tracks: List<ITrack>, noCallback: Boolean = false) {
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

    fun currentTrack(): ITrack {
        return queue[position]
    }

    interface TrackQueueCallback {
        fun replace(tracks: MutableList<ParcelableTrack>, pos: Int)
        fun insertAtEnd(tracks: MutableList<ParcelableTrack>)
        fun insertNext(tracks: MutableList<ParcelableTrack>)
        fun reorder(pos: Int, pnew: Int)
    }
}
