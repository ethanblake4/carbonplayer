package com.carbonplayer.model.entity

import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.base.IArtist
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Artist(

        @PrimaryKey override var artistId: String = "",
        override var kind: String = "",
        override var name: String = "",
        override var artistArtRef: String? = null,
        var artistArtRefs: RealmList<Image> = RealmList(),
        var artistTracks: RealmList<Track> = RealmList(),
        override var artistBio: String? = null,
        var albums: RealmList<Album> = RealmList(),
        var topTracks: RealmList<Track> = RealmList(),
        var totalAlbums: Int = -1,
        var artistBioAttribution: Attribution? = null,
        var related_artists: RealmList<Artist> = RealmList()

) : RealmObject(), IArtist {

    constructor(artistId: String, name: String) : this(artistId, "sj#artist", name)

    constructor(artistId: String, name: String, artRefs: List<Image>?) :
            this(artistId, "", name, null, RealmList<Image>().apply {
                artRefs?.let { addAll(it) }
            })

    constructor(src: SkyjamArtist, realm: Realm) : this (
            src.artistId,
            src.kind,
            src.name,
            src.artistArtRef,
            RealmList<Image>().apply { addAll(src.artistArtRefs) },
            RealmList(),
            src.artistBio,
            MusicLibrary.processAlbums(src.albums.map {Album(it)}, realm),
            RealmList<Track>().apply { addAll(MusicLibrary.processTracks(src.topTracks, realm)) },
            src.total_albums,
            src.artist_bio_attribution,
            MusicLibrary.processArtists(src.related_artists, realm)
    )

    fun updateFrom(src: SkyjamArtist, realm: Realm) {

        this.topTracks.clear()
        this.topTracks.addAll(MusicLibrary.processTracks(src.topTracks, realm))
        this.related_artists.clear()
        this.related_artists.addAll(MusicLibrary.processArtists(src.related_artists, realm))

        this.artistBio = src.artistBio
        this.artistBioAttribution = src.artist_bio_attribution?.let { realm.copyToRealm(it) }

        this.albums = MusicLibrary.processAlbums(src.albums.map { a-> Album(a) }, realm)
    }

    companion object {
        const val ID = "artistId"
        const val NAME = "name"
        const val TRACKS = "artistTracks"
    }

}