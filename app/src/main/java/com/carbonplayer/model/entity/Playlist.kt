package com.carbonplayer.model.entity

import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Playlist data class
 */

open class Playlist(

        @PrimaryKey var localId: Long = 0,
        override var id: String? = "",
        override var clientId: String? = "",
        override var kind: String = "",
        override var name: String = "",
        override var deleted: Boolean? = null,
        override var type: String? = null, /* MAGIC, SHARED, or USER_GENERATED */
        override var lastModifiedTimestamp: Long? = null,
        override var recentTimestamp: Long? = null,
        override var shareToken: String = "",
        override var ownerProfilePhotoUrl: String? = null,
        override var ownerName: String? = null,
        override var accessControlled: Boolean = false,
        override var shareState: String? = null, /* PRIVATE or PUBLIC*/
        override var creationTimestamp: Long? = null,
        var albumArtRef: RealmList<Image>? = null,
        var entries: RealmList<PlaylistEntry> = RealmList(),
        override var description: String? = null,
        override var explicitType: String? = null,
        override var contentType: String? = null

) : RealmObject(), IPlaylist {

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
        const val SHARE_TOKEN = "shareToken"
    }
}