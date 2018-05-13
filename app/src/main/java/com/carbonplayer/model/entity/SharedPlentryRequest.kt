package com.carbonplayer.model.entity

data class SharedPlentryRequest (
        val entries: List<Entry>,
        val includeDeleted: Boolean = false
) {
    data class Entry (
            val maxResults: Int,
            val shareToken: String,
            val startToken: String?,
            val updatedMin: Long
    ) {
        fun asRequest() = SharedPlentryRequest(listOf(this))
    }
}