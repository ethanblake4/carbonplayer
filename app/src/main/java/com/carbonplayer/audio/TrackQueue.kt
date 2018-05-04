package com.carbonplayer.audio


import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.utils.parcelable
import io.realm.Realm

/**
 * Track queue which mirrors data in [MusicPlayerService]
 */
class TrackQueue(val callback: TrackQueueCallback) {

    private var queue = mutableListOf<ITrack>()
    var position: Int = 0

    fun replace(tracks: List<ITrack>, pos: Int, noCallback: Boolean = false, local: Boolean = true) {
        queue.clear()
        queue.addAll(tracks)
        position = pos
        if (!noCallback) callback.replace(parcelable(local), pos)
    }

    fun parcelable(local: Boolean = true) = mutableListOf<ParcelableTrack>().apply {
        wrapRealm(local) { realm ->
            queue.forEach {
                this.add(it.parcelable(realm))
            }
        }
    }

    val size: Int get() = queue.size

    fun insertAtEnd(tracks: List<ITrack>, noCallback: Boolean = false, local: Boolean = true) {
        queue.addAll(tracks)
        wrapRealm(local) { realm ->
            if (!noCallback) callback.insertAtEnd(tracks.parcelable(realm))
        }
    }

    fun insertNext(tracks: List<ITrack>, noCallback: Boolean = false, local: Boolean = true) {
        queue.addAll(position, tracks)
        wrapRealm(local) { realm ->
            if (!noCallback) callback.insertNext(tracks.parcelable(realm))
        }
    }


    private inline fun wrapRealm(local: Boolean, crossinline block: (realm: Realm?) -> Unit) {

        if (!local) Realm.getDefaultInstance().executeTransaction({ block(it) })
        else return block(null)
    }

    fun reorder(pos: Int, pnew: Int, noCallback: Boolean = false) {
        val t = queue[pos]
        queue.removeAt(pos)
        queue.add(pnew, t)
        if (!noCallback) callback.reorder(pos, pnew)
    }

    fun remove(pos: Int, noCallback: Boolean = false) {
        queue.removeAt(pos)
        if(!noCallback) callback.remove(pos)
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
        fun remove(pos: Int)
    }
}
