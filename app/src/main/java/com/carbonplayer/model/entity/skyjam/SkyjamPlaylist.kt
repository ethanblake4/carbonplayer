package com.carbonplayer.model.entity.skyjam

import com.carbonplayer.model.entity.Image

data class SkyjamPlaylist (

        var id: String? = "",
        var clientId: String? = "",
        var kind: String = "",
        var name: String = "",
        var deleted: Boolean?,
        var type: String?,
        var lastModifiedTimestamp: Long?,
        var recentTimestamp: Long?,
        var shareToken: String = "",
        var ownerProfilePhotoUrl: String?,
        var ownerName: String?,
        var accessControlled: Boolean = false,
        var shareState: String?,
        var creationTimestamp: Long?,
        var albumArtRef: List<Image>?,
        var description: String?,
        var explicitType: String?,
        var contentType: String?

)