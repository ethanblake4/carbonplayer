package com.carbonplayer.model.entity

import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

/**
 * Album data class
 */

open class Album (

        override var kind: String = "",
        override var inLibrary: Boolean = false,
        @PrimaryKey override var albumId: String = "",
        override var recentTimestamp: Long? = null,
        override var title: String = "",
        override var albumArtist: String = "",
        override var composer: String = "",
        override var year: Int = 0,
        override var genre: String = "",
        override var albumArtRef: String = "",
        @LinkingObjects("albums") val artists: RealmResults<Artist>? = null,
        var tracks: RealmList<Track> = RealmList(),
        override var description: String? = null,
        override var description_attribution: Attribution? = null,
        override var explicitType: String? = null,
        override var contentType: String? = null

) : RealmObject(), IAlbum {

    override val artistId: List<String>
        get() = artists?.map { it.artistId } ?: listOf()

    constructor(source: SkyjamTrack, track: Track, inLibrary: Boolean = true) : this(
            "sj#album", inLibrary,
            source.albumId, source.recentTimestamp,
            if(source.album.isBlank()) "Unknown Album" else source.album,
            source.albumArtist, source.composer ?: "", source.year, source.genre ?: "",
            source.albumArtRef?.first()?.url ?: "",
            null, RealmList(track))

    constructor(source: SkyjamAlbum) : this (
            source.kind,
            source.inLibrary,
            source.albumId,
            source.recentTimestamp,
            source.title,
            source.albumArtist,
            source.composer?: "",
            source.year,
            source.genre,
            source.albumArtRef,
            null,
            RealmList<Track>(),
            source.description,
            source.description_attribution,
            source.explicitType,
            source.contentType

    )

    fun updateFrom(source: SkyjamAlbum, realm: Realm) : Album {
        kind = source.kind
        recentTimestamp = source.recentTimestamp
        albumArtRef = source.albumArtRef
        composer = source.composer ?: composer
        year = source.year
        description = source.description
        description_attribution = source.description_attribution?.let { realm.copyToRealm(it) }
        explicitType = source.explicitType
        contentType = source.contentType
        return this
    }



    companion object {
        const val ID = "albumId"
        const val TITLE = "title"
    }
}