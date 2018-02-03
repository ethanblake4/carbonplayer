package com.carbonplayer.model.entity


data class SearchResponse (
        val kind: String,
        val numResults: Int?,
        val clusterOrder: List<Int>?,
        val continuation: String?,
        val suggestedQuery: String?,
        val entries: List<SearchResult>?,
        val clusterDetail: List<ClusterDetail>?
) {

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
            /*val station: SkyjamStation? = null*/

            val track: Track?

            /*var video: YoutubeVideoJson? = null*/
    )

    data class Cluster (
            val category: Int?,
            val id: String?,
            val type: Int?
    )

    data class ClusterDetail (
            val cluster: Cluster?,
            val displayName: String?,
            val entries: List<SearchResult>?,
            val resultToken: String?
    )
}