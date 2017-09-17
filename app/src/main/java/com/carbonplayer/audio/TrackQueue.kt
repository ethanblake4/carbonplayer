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

    fun replace(tracks: List<MusicTrack>, noCallback: Boolean = false) {
        queue.clear()
        queue.addAll(tracks)
        if (!noCallback) callback.replace(parcelable)
    }

    val parcelable: MutableList<ParcelableMusicTrack>
        get() = MutableList(queue.size, { i -> queue[i].parcelable() })

    fun insertAtEnd(tracks: List<MusicTrack>, noCallback: Boolean = false) {
        queue.addAll(tracks)
        if (!noCallback) callback.insertAtEnd(tracks.parcelable())
    }

    fun insertNext(track: MusicTrack, noCallback: Boolean = false) {
        queue.add(position + 1, track)
        if (!noCallback) callback.insertNext(mutableListOf(track.parcelable()))
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
        fun replace(tracks: MutableList<ParcelableMusicTrack>)
        fun insertAtEnd(tracks: MutableList<ParcelableMusicTrack>)
        fun insertNext(tracks: MutableList<ParcelableMusicTrack>)
        fun reorder(pos: Int, pnew: Int)
    }
}
