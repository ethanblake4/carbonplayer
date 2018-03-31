package com.carbonplayer.model.network

import android.content.Context
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.network.entity.Stream

object StreamManager {

    fun getStream(context: Context, track: ITrack, download: Boolean): Stream {
        val quality = CarbonPlayerApplication.instance.preferences
                .getPreferredStreamQuality(CarbonPlayerApplication.instance)

        return Stream(context, track, quality, download)

    }
}