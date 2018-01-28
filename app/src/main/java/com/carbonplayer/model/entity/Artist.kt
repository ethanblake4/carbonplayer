package com.carbonplayer.model.entity

import com.squareup.moshi.Json
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Artist(

        @PrimaryKey var artistId: String = "",
        var kind: String = "",
        var name: String = "",
        var artistArtRef: String? = null,
        var artistArtRefs: RealmList<Image> = RealmList(),
        var artistBio: String? = null,
        var albums: RealmList<Album> = RealmList(),
        var topTracks: RealmList<Track>? = null,

        @Json(name = "total_albums")
        var totalAlbums: Int = -1,

        @Json(name = "artist_bio_attribution")
        var artistBioAttribution: Attribution? = null,

        var related_artists: RealmList<Artist> = RealmList()

) : RealmObject() {

    constructor(artistId: String, name: String) : this(artistId, "sj#artist", name)

    constructor(artistId: String, name: String, artRefs: List<Image>) :
            this(artistId, "", name, null, RealmList<Image>().apply {
                addAll(artRefs)
            })

    companion object {
        val ID = "artistId"
        val NAME = "name"
    }

}