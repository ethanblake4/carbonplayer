package com.carbonplayer.model.entity

import com.carbonplayer.model.entity.skyjam.SkyjamPlentry
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class PlaylistEntry(
        var kind: String = "",
        var id: String = "",
        @PrimaryKey var clientId: String = "",
        @LinkingObjects("entries") val playlist: RealmResults<Playlist>? = null,
        var absolutePosition: String = "",
        var trackId: String = "",
        var creationTimestamp: Long = 0,
        var lastModifiedTimestamp: Long = 0,
        var deleted: Boolean = false,
        var source: String = "",
        var track: Track? = null
) : RealmObject() {

    constructor(source: SkyjamPlentry, track: Track?) : this (
            source.kind,
            source.id,
            source.clientId!!,
            null,
            source.absolutePosition,
            source.trackId,
            source.creationTimestamp,
            source.lastModifiedTimestamp,
            source.deleted,
            source.source,
            track
    )
}