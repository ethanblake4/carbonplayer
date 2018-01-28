package com.carbonplayer.model.network

import android.content.Context
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.network.entity.Stream

object StreamManager {

    fun getStream(context: Context, id: SongID, title: String, download: Boolean): Stream {
        val quality = CarbonPlayerApplication.instance.preferences
                .getPreferredStreamQuality(CarbonPlayerApplication.instance)

        return Stream(context, id, title, quality, download)

    }
}