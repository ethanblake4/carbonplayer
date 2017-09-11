package com.carbonplayer.model.network;

import android.content.Context;
import android.util.Pair;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.ParcelableMusicTrack;
import com.carbonplayer.model.entity.SongID;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.network.entity.StreamingContent;

import java.io.IOException;
import java.util.List;

import rx.Observable;
import rx.Single;
import rx.functions.Func1;


public class StreamManager {
    private static StreamManager instance;

    public static StreamManager getInstance() {
        if(instance == null) instance = new StreamManager();
        return instance;
    }


    public StreamingContent getStream(Context context, SongID id, String title){
        StreamQuality quality = CarbonPlayerApplication.Companion.getInstance().getPreferences()
                .getPreferredStreamQuality(CarbonPlayerApplication.Companion.getInstance());

        StreamingContent content = new StreamingContent(context, id, title, quality);

        return content;

    }
}