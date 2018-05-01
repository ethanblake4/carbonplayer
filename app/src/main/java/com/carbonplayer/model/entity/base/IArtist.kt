package com.carbonplayer.model.entity.base

interface IArtist {

    var artistId: String
    var kind: String
    var name: String
    var artistArtRef: String?
    var artistBio: String?
    val bestArtistArtUrl: String?

}