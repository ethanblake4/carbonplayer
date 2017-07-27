package com.carbonplayer.model.network.entity;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.SongID;
import com.carbonplayer.model.entity.TrackCache;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class StreamingContent {

    private PublishSubject<Float> downloadProgress = PublishSubject.create();
    private long completed = 0;
    private String contentType;
    private final Context context;
    private long seekMs;
    private StreamQuality quality;
    private final DownloadRequest downloadRequest;
    private long extraChunkSize = 0;
    private String filepath;
    private boolean isInitialized = false;
    private boolean waitAllowed = false;
    private final SongID songID;
    private volatile long startReadPoint = 0;
    private String url;
    private long lastWait;

    public StreamingContent(Context context, SongID songId, String trackTitle, StreamQuality quality) {

        if(!TrackCache.has(context, songId, quality)) {
            downloadRequest =
                    new DownloadRequest(songId, trackTitle, 100,
                            0, new FileLocation(StorageType.CACHE, TrackCache.getTrackFile(context, songId)),
                            true, quality, StreamQuality.UNDEFINED);
            this.filepath = TrackCache.getTrackFile(context, songId).getAbsolutePath();
        } else {
            this.filepath = TrackCache.getTrackFile(context, songId).getAbsolutePath();
            this.downloadRequest = null;
            downloadProgress.onNext(1.0f);
            initDownload();
        }
        this.context = context;
        this.songID = songId;
        this.quality = quality;
    }

    private void initDownload(){
        ProgressResponseBody.ProgressListener listener = (bytesRead, contentLength, done) ->
                downloadProgress.onNext((float)((bytesRead/100.0)/(contentLength/100.0)));
        ArrayList<okhttp3.Protocol> protocols = new ArrayList<>();
        protocols.add(okhttp3.Protocol.HTTP_1_1);
        OkHttpClient client = CarbonPlayerApplication.getOkHttpClient(new OkHttpClient.Builder()
                .protocols(protocols)
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), listener))
                            .build();
                }));

        com.carbonplayer.model.network.Protocol.getStreamURL(context, songID.getNautilusID())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(Looper.getMainLooper()))
                .subscribe(url -> {
                    try {
                        Response response = client.newCall(new Request.Builder()
                                .url(url).build()).execute();
                        downloadRequest.setState(DownloadRequest.State.DOWNLOADING);

                        BufferedSink sink = Okio.buffer(Okio.sink(new File(filepath)));
                        Source source = response.body().source();

                        long len = response.body().contentLength();
                        long writ = 0;

                        while(writ < len) {
                            sink.write(source, 2048);
                            writ += 2048;
                            synchronized (StreamingContent.this) {
                                notifyAll();
                            }
                        }
                        sink.close();
                        downloadRequest.setState(DownloadRequest.State.COMPLETED);
                    } catch (IOException e) {
                        throw Exceptions.propagate(e);
                    }
                }, error -> {
                    Timber.e(error, "Exception getting stream URL");
                    downloadRequest.setState(DownloadRequest.State.FAILED);
                });
    }

    public String toString() {
        return "StreamingContent: { seekMs: " + this.seekMs +
                ", completed: " + this.completed +
                ", DownloadRequest: " + this.downloadRequest +
                " }";
    }

    public Observable<Float> progressMonitor() {
        return downloadProgress;
    }

    public SongID getId(){
        return songID;
    }

    public synchronized void initialize(String httpContentType) throws InterruptedException {
        isInitialized = true;
        contentType = httpContentType;
        if (downloadRequest == null){
            MusicTrack track = MusicLibrary.getInstance().getTrack(songID.getId());
            File location = TrackCache.getTrackFile(context, songID);
            if(location == null) {
                Timber.e("Failed to resolve path for initialize");
                return;
            }
            String extension = DownloadUtils.getFileExtension(location);
            if (extension == null) {
                Timber.e("Failed to parse file extension for location: %s", location.getAbsolutePath());
                return;
            }
            contentType = DownloadUtils.ExtensionToMimeMap.get(extension);
            completed = track.getLocalTrackSizeBytes();
            filepath = location.getAbsolutePath();
            if (seekMs != 0) {
                this.startReadPoint = (long) ((((float) this.completed) * ((float) seekMs)) / ((float) track.getDurationMillis()));
            }
            Timber.d("contentType=%s completed=%d fileName=%s", contentType, completed, filepath);
            Timber.d("contentType: %s", contentType);
        } else if (this.contentType == null && this.downloadRequest.getState() == DownloadRequest.State.COMPLETED) {
            MusicTrack track = MusicLibrary.getInstance().getTrack(downloadRequest.getId().getId());
            if (track == null) {
                Timber.e("Failed to load music file for %s", downloadRequest);
            } else {
                File location = TrackCache.getTrackFile(context, new SongID(track));
                if (location == null) {
                    Timber.w("Failed to resolve path for request: %s", downloadRequest);
                    return;
                }
                String extension = DownloadUtils.getFileExtension(location);
                if (extension == null) {
                    Timber.e("Failed to parse file extension for location: %s", location.getAbsolutePath());
                    return;
                }
                contentType = DownloadUtils.ExtensionToMimeMap.get(extension);
                completed = track.getLocalTrackSizeBytes();
                filepath = location.getAbsolutePath();
                if (seekMs != 0) {
                    this.startReadPoint = (long) ((((float) this.completed) * ((float) seekMs)) / ((float) track.getDurationMillis()));
                }
                Timber.d("contentType=%s completed=%d fileName=%s", contentType, completed, filepath);
                Timber.d("contentType: %s for request %s", contentType, downloadRequest);
            }
        }
    }

    public synchronized void waitForData(long amount) throws InterruptedException {
        while (!isFinished() && this.completed < this.extraChunkSize + amount && this.waitAllowed) {
            long uptimeMs = SystemClock.uptimeMillis();
            if (lastWait + 10000 < uptimeMs) {
                this.lastWait = uptimeMs;
                Timber.i("waiting for %d bytes in file: %s", amount, filepath);
            }
            wait();
        }
    }

    public synchronized RandomAccessFile getStreamFile(long offset) throws IOException {
        RandomAccessFile streamFile;
        File location = null;
        if (this.filepath != null) {
            location = new File(this.filepath);
        } else if(downloadRequest != null){
            location = downloadRequest.getFileLocation().getFullPath();
        }
        if (location == null) {
            streamFile = null;
        } else {
            streamFile = new RandomAccessFile(location, "r");
            streamFile.seek(offset);
        }
        return streamFile;
    }

    public Context getContext() {
        return context;
    }

    public long getStartReadPoint() {
        return startReadPoint;
    }

    SongID getSongId() {
        return this.songID;
    }

    public synchronized void setUrl(String url) {
        this.url = url;
    }

    public synchronized String getUrl() {
        return url;
    }

    public String getContentType(){
        if(!isInitialized){
            throw new IllegalArgumentException("StreamingContent must be initialized");
        }
        return contentType;
    }

    public void setWaitAllowed(boolean waitAllowed){
        this.waitAllowed = waitAllowed;
    }

    public synchronized boolean isFinished() {
        DownloadRequest.State state = downloadRequest.getState();
        return state == DownloadRequest.State.COMPLETED || state == DownloadRequest.State.CANCELED || state == DownloadRequest.State.FAILED;
    }

    public synchronized boolean isCompleted() {
        DownloadRequest.State state = downloadRequest.getState();
        return state == DownloadRequest.State.COMPLETED;
    }

    public synchronized boolean isFailed() {
        DownloadRequest.State state = downloadRequest.getState();
        return state == DownloadRequest.State.FAILED;
    }

    public static String contentListToString(String msg, List<StreamingContent> list) {
        StringBuilder builder = new StringBuilder();
        builder.append(msg);
        builder.append("=[");
        for (StreamingContent content : list) {
            builder.append(content.getSongId());
            builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }


}
