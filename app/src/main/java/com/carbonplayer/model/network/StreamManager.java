package com.carbonplayer.model.network;

import android.content.Context;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.SongID;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.network.entity.StreamingContent;


public class StreamManager {
    private static StreamManager instance;

    public static StreamManager getInstance() {
        if (instance == null) instance = new StreamManager();
        return instance;
    }

    public StreamingContent getStream(Context context, SongID id, String title, boolean download) {
        StreamQuality quality = CarbonPlayerApplication.Companion.getInstance().getPreferences()
                .getPreferredStreamQuality(CarbonPlayerApplication.Companion.getInstance());

        StreamingContent content = new StreamingContent(context, id, title, quality, download);

        return content;

    }
}