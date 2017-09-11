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
    private StreamServer server;

    public static StreamManager getInstance(Context context) throws IOException {
        if(instance == null) instance = new StreamManager(context);
        return instance;
    }

    private StreamManager(Context context) throws IOException {
        server = new StreamServer(context);
    }

    public Single<Pair<String, Observable<Float>>> getUrl(Context context, SongID id, String title){
        StreamQuality quality = CarbonPlayerApplication.Companion.getInstance().getPreferences()
                .getPreferredStreamQuality(CarbonPlayerApplication.Companion.getInstance());

        StreamingContent content = new StreamingContent(context, id, title, quality);

        return Single.fromCallable(() -> server.serveStream(content))
                .map(stream_url -> new Pair<>(stream_url, content.progressMonitor()));

    }

    public StreamingContent getStream(Context context, SongID id, String title){
        StreamQuality quality = CarbonPlayerApplication.Companion.getInstance().getPreferences()
                .getPreferredStreamQuality(CarbonPlayerApplication.Companion.getInstance());

        StreamingContent content = new StreamingContent(context, id, title, quality);

        return content;

    }
}