package com.carbonplayer.model.entity.base



interface IPlaylist {

    val id: String?
    val clientId: String?
    val kind: String
    val name: String
    val deleted: Boolean?
    val type: String? /* MAGIC, SHARED, or USER_GENERATED */
    val lastModifiedTimestamp: Long?
    val recentTimestamp: Long?
    val shareToken: String
    val ownerProfilePhotoUrl: String?
    val ownerName: String?
    val accessControlled: Boolean
    var shareState: String? /* PRIVATE or PUBLIC*/
    var creationTimestamp: Long?
    var description: String?
    var explicitType: String?
    var contentType: String?
}