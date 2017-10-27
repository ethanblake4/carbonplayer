package com.carbonplayer.model.entity

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.json.JSONException
import org.json.JSONObject

open class Artist(

        @PrimaryKey var artistId: String = "",
        var kind: String = "",
        var name: String = "",
        var artistArtRef: String? = null,
        var artistArtRefs: RealmList<Image>? = null,
        var artistBio: String? = null,
        var albums: RealmList<Album>? = null,
        var topTracks: RealmList<MusicTrack>? = null,
        var totalAlbums: Int = -1,
        var artistBioAttribution: Attribution? = null,
        var relatedArtists: RealmList<Artist> = RealmList()

) : RealmObject() {

    constructor(artistId: String, name: String) : this(artistId, "", name)

    constructor(artistIdIndexed: String, track: MusicTrack) : this(
            artistIdIndexed,
            "",
            track.artist,
            track.artistArtURL
    )

    @Throws(JSONException::class)
    constructor(json: JSONObject) : this() {
        artistId = "artistId".let { if (json.has(it)) json.getString(it) else "" }
        kind = json.getString("kind")
        name = json.getString("name")
        artistArtRef = "artistArtRef".let { if (json.has(it)) json.getString(it) else null }
        artistArtRefs = "artistArtRefs".let {
            if (json.has(it)) {
                val refs = RealmList<Image>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(refs) { Image(array.getJSONObject(it)) }
                refs
            } else null
        }
        artistBio = "artistBio".let { if (json.has(it)) json.getString(it) else null }
        albums = "albums".let {
            if (json.has(it)) {
                val i_albums = RealmList<Album>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(i_albums) {
                    Album(array.getJSONObject(it))
                }
            } else null
        }
        topTracks = "topTracks".let {
            if (json.has(it)) {
                val i_topTracks = RealmList<MusicTrack>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(i_topTracks) {
                    MusicTrack(array.getJSONObject(it))
                }
            } else null
        }
        totalAlbums = "total_albums".let { if (json.has(it)) json.getInt(it) else -1 }
        artistBioAttribution = "artist_bio_attribution".let {
            if (json.has(it)) Attribution(json.getJSONObject(it))
            else null
        }
        relatedArtists = "related_artists".let {
            if (json.has(it)) {
                val i_relatedArtists = RealmList<Artist>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(i_relatedArtists) {
                    Artist(array.getJSONObject(it))
                }
            } else RealmList()
        }
    }

    @Throws(JSONException::class)
    constructor(json: JSONObject, realm: Realm) : this() {
        artistId = "artistId".let { if (json.has(it)) json.getString(it) else "" }
        kind = json.getString("kind")
        name = json.getString("name")
        artistArtRef = "artistArtRef".let { if (json.has(it)) json.getString(it) else null }
        artistArtRefs = "artistArtRefs".let {
            if (json.has(it)) {
                val refs = RealmList<Image>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(refs) { Image(array.getJSONObject(it)) }
                refs
            } else null
        }
        artistBio = "artistBio".let { if (json.has(it)) json.getString(it) else null }
        albums = "albums".let {
            if (json.has(it)) {
                val i_albums = RealmList<Album>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(i_albums) {
                    val newAlbum = Album(array.getJSONObject(it))
                    val old = realm.where(Album::class.java).equalTo(Album.ID, newAlbum.id)
                            .findAll()
                    if (old.size > 0) old.first()
                    else newAlbum
                }
            } else null
        }
        topTracks = "topTracks".let {
            if (json.has(it)) {
                val i_topTracks = RealmList<MusicTrack>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(i_topTracks) {
                    val newTrack = MusicTrack(array.getJSONObject(it))
                    val old = realm.where(MusicTrack::class.java)
                            .equalTo(MusicTrack.ID, newTrack.trackId)
                            .findAll()
                    if (old.size > 0) old.first()
                    else newTrack
                }
            } else null
        }
        totalAlbums = "total_albums".let { if (json.has(it)) json.getInt(it) else -1 }
        artistBioAttribution = "artist_bio_attribution".let {
            if (json.has(it)) Attribution(json.getJSONObject(it))
            else null
        }
        relatedArtists = "related_artists".let {
            if (json.has(it)) {
                val i_relatedArtists = RealmList<Artist>()
                val array = json.getJSONArray(it)
                (0..array.length()).mapTo(i_relatedArtists) {
                    val newId = array.getJSONObject(it).getString("artistId")
                    val old = realm.where(Artist::class.java)
                            .equalTo(MusicTrack.ID, newId)
                            .findAll()
                    if (old.size > 0) old.first()
                    else Artist(array.getJSONObject(it), realm)
                }
            } else RealmList()
        }
    }

    companion object {
        val NAME = "name"
    }

}