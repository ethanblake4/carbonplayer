package com.carbonplayer.model.entity.api

import com.carbonplayer.model.entity.skyjam.SkyjamPlentry

data class SharedPlentryResponse (
        val entries: List<Entry>
) {
    data class Entry (
            val nextPageToken: String?,
            val playlistEntry: List<SkyjamPlentry>,
            val responseCode: String?,
            val shareToken: String?
    )
}