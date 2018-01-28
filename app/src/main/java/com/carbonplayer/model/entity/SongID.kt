package com.carbonplayer.model.entity

data class SongID(val localId: Long, val id: String?, val clientID: String?, val nautilusID: String?) {
    constructor(track: Track) : this(track.localId, track.id ?: track.clientId, track.clientId, track.nid)
    constructor(track: ParcelableTrack) : this(track.localId, track.id, track.clientId, track.nid)
}
