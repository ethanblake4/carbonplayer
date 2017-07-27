package com.carbonplayer.model.network;

import android.content.Context;
import android.util.Pair;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.ParcelableMusicTrack;
import com.carbonplayer.model.entity.SongID;
import com.carbonplayer.model.entity.TrackCache;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.network.entity.DownloadRequest;
import com.carbonplayer.model.network.entity.FileLocation;
import com.carbonplayer.model.network.entity.ProgressResponseBody;
import com.carbonplayer.model.network.entity.StreamingContent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import timber.log.Timber;

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
        server = new StreamServer(CarbonPlayerApplication.getInstance());
    }

    public StreamManager(List<ParcelableMusicTrack> tracks) throws IOException {
        this();
        tracks = tracks;
    }

    public Observable<Pair<String, Observable<Float>>> getLocalStreamUrlForCurrentTrack(Context context){
        StreamQuality quality = CarbonPlayerApplication.preferences()
                .getPreferredStreamQuality(CarbonPlayerApplication.getInstance());
        SongID id = new SongID(tracks.get(0));

        StreamingContent content = new StreamingContent(context, id, tracks.get(0).getTitle(), quality);

        return Observable.fromCallable(() -> server.serveStream(content))
                .map(stream_url -> new Pair<>(stream_url, content.progressMonitor()));
        /*} else {
            return Protocol.getStreamURL(context, tracks.get(0).getNid())
                .flatMap(streamUrl -> {
                    DownloadRequest request =
                            new DownloadRequest(id, tracks.get(0).getTitle(), 100,
                                    0, new FileLocation(StorageType.CACHE, TrackCache.getTrackFile(context, id)),
                                    true, quality, StreamQuality.UNDEFINED);
                    ProgressResponseBody.ProgressListener listener = (bytesRead, contentLength, done) ->
                            Timber.d("Update with numberOfBytes %d contentlength %d", bytesRead, contentLength);
                    OkHttpClient client = CarbonPlayerApplication.getOkHttpClient(new OkHttpClient.Builder()
                            .addNetworkInterceptor(chain -> {
                                Response originalResponse = chain.proceed(chain.request());
                                return originalResponse.newBuilder()
                                        .body(new ProgressResponseBody(originalResponse.body(), listener))
                                        .build();
                            }));
                    String localUrl = server.serveStream(new StreamingContent(context, request));
                    new Request.Builder()
                            .url(localUrl)
                            .
                    return Observable.error(new Exception());
                });
        }*/

    }
}
