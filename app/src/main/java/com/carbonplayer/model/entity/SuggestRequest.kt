package com.carbonplayer.model.entity

data class SuggestRequest (
        val capabilities: SuggestCapabilities,
        val query: String
) {
    data class SuggestCapabilities (
            val content_types: List<Int>,
            val entity_suggest: Boolean
    )
}