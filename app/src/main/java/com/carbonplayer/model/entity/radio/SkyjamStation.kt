package com.carbonplayer.model.entity.radio

import com.carbonplayer.model.entity.Image
import com.carbonplayer.model.entity.skyjam.SkyjamTrack

data class SkyjamStation(

        val adTargeting: AdTargeting?,
        val artComposites: MutableList<Image>?,
        val byline: String?,
        val clientId: String?,
        val contentTypes: List<Int>?,
        val description: String?,
        val imageType: Int = 0,
        val imageUrl: String?,
        val imageUrls: List<Image>?,
        val deleted: Boolean? /* for public */,
        val inLibrary: Boolean?,
        val lastModifiedTimestamp: Long? = null,
        val name: String,
        val recentTimestamp: Long? /* for public */,
        val id: String?,
        val seed: RadioSeed,
        val sessionToken: String? /* For free radios */,
        val skipEventHistory: List<SkipEvent>?,
        val tracks: List<SkyjamTrack>

) {

    data class AdTargeting (
            val keywords: List<String>?
    )
}