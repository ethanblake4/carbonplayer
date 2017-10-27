package com.carbonplayer.model.entity

import com.carbonplayer.utils.*
import org.json.JSONObject


data class SearchResponse (
    val kind: String,
    val numResults: Int?,
    val clusterOrder: List<Int>?,
    val continuation: String?,
    val suggestedQuery: String?,
    val entries: List<SearchResult>?,
    val clusterDetail: List<ClusterDetail>?
) {

    constructor(json: JSONObject): this (
            json.getString("kind"),
            json.maybeGetInt("num_results"),
            json.maybeGetArray("cluster_order")?.let { it.mapArray { i -> getInt(i) } },
            json.maybeGetString("continuation_token"),
            json.maybeGetString("suggestedQuery"),
            json.maybeGetArray("entries")?.let {
                it.mapArray { i -> SearchResult(getJSONObject(i)) }
            },
            json.maybeGetArray("clusterDetail")?.let {
                it.mapArray { i -> ClusterDetail(getJSONObject(i)) }
            }
    )

    data class SearchResult (
            val type: Int,
            val score: Int?,
            val navigationalConfidence: Float?,
            val navigationalResult: Boolean?,
            val playable: Boolean?,
            val subtitle: String?,

            val album: Album?,
            val artist: Artist?,
           /* val genre: MusicGenreJson? = null*/

            val playlist: Playlist?,
    /*var podcastSeries: PodcastSeries? = null*/
            val searchEntryContext: String?,
            /*var situation: SituationJson? = null*/
            /*val station: SyncableRadioStation? = null*/

            val track: MusicTrack?

            /*var video: YoutubeVideoJson? = null*/
    ) {
        constructor(json: JSONObject): this (
                json.getInt("type"),
                json.maybeGetInt("score"),
                json.maybeGetDouble("navigational_confidence")?.toFloat(),
                json.maybeGetBool("navigational_result"),
                json.maybeGetBool("playable"),
                json.maybeGetString("subtitle"),
                json.maybeGetObj("album")?.let { Album(it) },
                json.maybeGetObj("artist")?.let { Artist(it) },
                json.maybeGetObj("playlist")?.let { Playlist(it) },
                json.maybeGetString("search_entry_context"),
                json.maybeGetObj("track")?.let { MusicTrack(it) }
        )
    }

    data class Cluster (
            val category: Int?,
            val id: String?,
            val type: Int?
    ) {
        constructor(json: JSONObject): this (
                json.maybeGetInt("category"),
                json.maybeGetString("id"),
                json.maybeGetInt("type")
        )
    }

    data class ClusterDetail (
            val cluster: Cluster?,
            val displayName: String?,
            val entries: List<SearchResult>?,
            val resultToken: String?
    ) {
        constructor(json: JSONObject): this (
                json.maybeGetObj("cluster")?.let { Cluster(it) },
                json.maybeGetString("displayName"),
                json.maybeGetArray("entries")?.let {
                    it.mapArray { i -> SearchResult(getJSONObject(i)) }
                },
                json.maybeGetString("resultToken")
        )
    }
}