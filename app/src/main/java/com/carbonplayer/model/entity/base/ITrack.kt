package com.carbonplayer.model.entity.base

import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import io.realm.Realm

interface ITrack {

    var inLibrary: Boolean

    val audioAd: Boolean?
    val id: String?
    val clientId: String?
    val nid: String?
    val storeId: String?

    val title: String

    /* Relic of a simpler time, when most tracks didn't have (ft.) at the end */
    val artist: String

    val album: String
    val albumId: String
    val albumArtist: String
    val albumArtURL: String?

    val trackNumber: Int
    val totalTrackCount: Int?
    val discNumber: Int
    val totalDiscCount: Int?
    val estimatedSize: Int?
    val trackType: Int?
    val durationMillis: Int

    val trackAvailableForPurchase: Boolean?
    val trackAvailableForSubscription: Boolean?

    val rating: String?
    val lastRatingChangeTimestamp: Long?

    /**
     * @see com.carbonplayer.model.entity.enums.ExplicitType
     */
    val explicitType: Int?

    val creationTimestamp: Long?
    val recentTimestamp: Long?
    val isDeleted: Boolean
    val comment: String?

    val composer: String?
    val genre: String?
    val year: Int
    val beatsPerMinute: Int?

    val playCount: Int?

    fun parcelable(realm: Realm? = null) : ParcelableTrack
    fun syncable() : SkyjamTrack
}