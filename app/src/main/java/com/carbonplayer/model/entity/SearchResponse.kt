package com.carbonplayer.model.entity

import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamTrack

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

            val album: SkyjamAlbum?,
            val artist: SkyjamArtist?,
           /* val genre: MusicGenreJson? = null*/

            val playlist: SkyjamPlaylist?,
    /*var podcastSeries: PodcastSeries? = null*/
            val searchEntryContext: String?,
            /*var situation: SituationJson? = null*/
            val station: SkyjamStation? = null,

            val track: SkyjamTrack?

            /*var video: YoutubeVideoJson? = null*/
    ) {
        val title: String? get() {
            return album?.name ?:
                artist?.name ?:
                playlist?.name ?:
                track?.title
        }
    }

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