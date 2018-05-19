package com.carbonplayer.model.entity.api

data class SuggestRequest (
        val capabilities: SuggestCapabilities,
        val query: String
) {
    data class SuggestCapabilities (
            val content_types: List<Int>,
            val entity_suggest: Boolean
    )
}