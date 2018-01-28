package com.carbonplayer.model.entity.base

import com.carbonplayer.model.entity.Attribution

interface IAlbum {
    val kind: String
    val inLibrary: Boolean
    val albumId: String
    val recentTimestamp: Long?
    val title: String
    val albumArtist: String
    val albumArtRef: String
    val artistId: List<String>
    val composer: String?
    val year: Int
    val genre: String
    val description: String?
    val description_attribution: Attribution?
    val explicitType: String?
    val contentType: String?
}