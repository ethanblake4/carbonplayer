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

/**
 * Created by ethanelshyeb on 7/21/17.
 */

public class StreamManager {
    private static StreamManager instance;
    private List<ParcelableMusicTrack> tracks;
    private StreamServer server;
    private StreamingContent currentTrack;
    private StreamingContent nextTrack;

    public static StreamManager getInstance() throws IOException {
        if(instance == null) instance = new StreamManager();
        return instance;
    }

    private StreamManager() throws IOException {
        server = new StreamServer(CarbonPlayerApplication.Companion.getInstance());
    }

    public StreamManager(List<ParcelableMusicTrack> tracks) throws IOException {
        this();
        this.tracks = tracks;
    }

    public void setTracks(List<ParcelableMusicTrack> tracks) {
        this.tracks = tracks;
    }

    public Single<Pair<String, Observable<Float>>> getLocalStreamUrlForCurrentTrack(Context context){
        StreamQuality quality = CarbonPlayerApplication.Companion.getInstance().getPreferences()
                .getPreferredStreamQuality(CarbonPlayerApplication.Companion.getInstance());
        SongID id = new SongID(tracks.get(0));

        StreamingContent content = new StreamingContent(context, id, tracks.get(0).getTitle(), quality);

        return Single.fromCallable(() -> server.serveStream(content))
                .map(stream_url -> new Pair<>(stream_url, content.progressMonitor()));

    }
}
