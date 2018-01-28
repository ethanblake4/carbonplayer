package com.carbonplayer.model.entity

import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Playlist data class
 */

open class Playlist(

        @PrimaryKey var localId: Long = 0,
        var id: String? = "",
        var clientId: String? = "",
        var kind: String = "",
        var name: String = "",
        var deleted: Boolean? = null,
        var type: String? = null, /* MAGIC, SHARED, or USER_GENERATED */
        var lastModifiedTimestamp: Long? = null,
        var recentTimestamp: Long? = null,
        var shareToken: String = "",
        var ownerProfilePhotoUrl: String? = null,
        var ownerName: String? = null,
        var accessControlled: Boolean = false,
        var shareState: String? = null, /* PRIVATE or PUBLIC*/
        var creationTimestamp: Long? = null,
        var albumArtRef: RealmList<Image>? = null,
        var entries: RealmList<PlaylistEntry> = RealmList(),
        var description: String = "",
        var explicitType: String? = null,
        var contentType: String? = null

) : RealmObject() {

    constructor(source: SkyjamPlaylist, localId: Long) : this (
            localId,
            source.id,
            source.clientId,
            source.kind,
            source.name,
            source.deleted,
            source.type,
            source.lastModifiedTimestamp,
            source.recentTimestamp,
            source.shareToken,
            source.ownerProfilePhotoUrl,
            source.ownerName,
            source.accessControlled,
            source.shareState,
            source.creationTimestamp,
            source.albumArtRef?.let { RealmList<Image>().apply { addAll(it) } },
            RealmList<PlaylistEntry>(),
            source.description ?: "",
            source.explicitType,
            source.contentType
    )

    companion object {
        const val LOCAL_ID = "localId"
        const val REMOTE_ID = "id"
        const val CLIENT_ID = "clientId"
    }
}