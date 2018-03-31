package com.carbonplayer.model.entity

import com.carbonplayer.model.entity.base.ITrack

data class SongID(var localId: Long, val id: String?, val clientID: String?, val nautilusID: String?, val storeId: String?) {
    constructor(track: Track) : this(track.localId, track.id ?: track.clientId, track.clientId, track.nid, track.storeId)
    constructor(track: ParcelableTrack) : this(track.localId ?: -1, track.id, track.clientId, track.nid, track.storeId)
    constructor(track: ITrack) : this(
            (track as? Track)?.localId ?: (track as? ParcelableTrack)?.localId ?: -1L,
            track.id, track.clientId, track.nid, track.storeId
    )

}