package com.carbonplayer.model.entity

data class SongID(val id: String, val clientID: String?, val nautilusID: String?){
    constructor(track: MusicTrack) : this(track.trackId, track.clientId, track.nid)
    constructor(track: ParcelableMusicTrack) : this(track.id, track.clientId, track.nid)
}
