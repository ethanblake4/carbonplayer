package com.carbonplayer.model.entity.skyjam

import android.annotation.SuppressLint
import android.os.Parcelable
import com.carbonplayer.model.entity.Image
import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.model.entity.proto.identifiers.PlayableItemIdV1Proto
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class SkyjamPlaylist (

        override var id: String? = "",
        override var clientId: String? = "",
        override var kind: String = "",
        override var name: String = "",
        override var deleted: Boolean?,
        override var type: String?,
        override var lastModifiedTimestamp: Long?,
        override var recentTimestamp: Long?,
        override var shareToken: String = "",
        override var ownerProfilePhotoUrl: String?,
        override var ownerName: String?,
        override var accessControlled: Boolean = false,
        override var shareState: String?,
        override var creationTimestamp: Long?,
        var albumArtRef: List<Image>?,
        override var description: String?,
        override var explicitType: String?,
        override var contentType: String?

) : IPlaylist, Parcelable {
    constructor(protoItem: PlayableItemIdV1Proto.PlayableItemId, name: String) : this (
            "", "", "", name, null, null, null, null,
            protoItem.audioList.sharedPlaylist.playlistToken,
            null, null, false, null, null, null, null, null, null
    )
}