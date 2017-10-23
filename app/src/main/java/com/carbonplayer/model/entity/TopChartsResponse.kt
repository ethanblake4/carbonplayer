package com.carbonplayer.model.entity

import org.json.JSONObject

data class TopChartsResponse (
        val image: Image,
        val albums: List<Album>,
        val tracks: List<MusicTrack>
){
    constructor(json: JSONObject) : this (
            Image(json.getJSONObject("header").getJSONObject("header_image")),
            json.getJSONObject("chart").getJSONArray("albums").let {
                (0..it.length()-1).map {i -> Album(it.getJSONObject(i))}
            },
            json.getJSONObject("chart").getJSONArray("albums").let {
                (0..it.length()-1).map {i -> MusicTrack(it.getJSONObject(i))}
            }
    )

}