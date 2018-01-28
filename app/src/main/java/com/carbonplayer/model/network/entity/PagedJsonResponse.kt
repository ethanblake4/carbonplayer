package com.carbonplayer.model.network.entity

import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamPlentry
import com.carbonplayer.model.entity.skyjam.SkyjamTrack

interface PagedJsonResponse<out T> {
    val nextPageToken: String?
    val data: T
}

data class PagedTrackResponse (
        override val nextPageToken: String?,
        override val data: PagedTrackResponseData
) : PagedJsonResponse<PagedTrackResponseData>

data class PagedTrackResponseData (
        val items: List<SkyjamTrack>
)

data class PagedPlaylistResponse (
        override val nextPageToken: String?,
        override val data: PagedPlaylistResponseData
) : PagedJsonResponse<PagedPlaylistResponseData>

data class PagedPlaylistResponseData (
        val items: List<SkyjamPlaylist>
)

data class PagedPlentryResponse (
        override val nextPageToken: String?,
        override val data: PagedPlentryResponseData
) : PagedJsonResponse<PagedPlentryResponseData>

data class PagedPlentryResponseData (
        val items: List<SkyjamPlentry>
)

