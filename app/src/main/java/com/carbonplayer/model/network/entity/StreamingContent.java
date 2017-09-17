package com.carbonplayer.model.network.entity;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.SongID;
import com.carbonplayer.model.entity.TrackCache;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.entity.exception.ServerRejectionException;
import com.carbonplayer.model.network.Protocol;

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
import rx.Completable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class StreamingContent {

    private PublishSubject<Float> downloadProgress = PublishSubject.create();
    public boolean downloadInitialized = false;
    private long completed = 0;
    private final Context context;
    private long seekMs;
    private StreamQuality quality;
    private final DownloadRequest downloadRequest;
    private long extraChunkSize = 0;
    private String filepath;
    private boolean waitAllowed = true;
    private final SongID songID;
    private volatile long startReadPoint = 0;
    private String url;
    private long lastWait;
    private long len;

    public StreamingContent(Context context, SongID songId, String trackTitle,
                            StreamQuality quality, boolean doDownload) {

        this.context = context;
        this.songID = songId;
        this.quality = quality;

        if(!TrackCache.has(context, songId, quality)) {
            Timber.i("Creating new DownloadRequest");
            downloadRequest =
                    new DownloadRequest(songId, trackTitle, 100,
                            0, new FileLocation(StorageType.CACHE,
                            TrackCache.getTrackFile(context, songId, quality)),
                            true, quality, StreamQuality.UNDEFINED);
            this.filepath = TrackCache.getTrackFile(context, songId, quality).getAbsolutePath();
            if (doDownload) initDownload();
        } else {
            Timber.i("File already exists");
            File file = TrackCache.getTrackFile(context, songId, quality);
            this.filepath = file.getAbsolutePath();
            this.downloadRequest = null;
            downloadProgress.onNext(1.0f);
            completed = file.length();

        }

    }

    public StreamingContent(Context context, SongID songId, String trackTitle,
                            StreamQuality quality) {

        this(context, songId, trackTitle, quality, true);

    }

    public void initDownload(){
        downloadInitialized = true;
        Timber.i("InitDownload for %s", this.toString());
        if(downloadRequest == null) return;

        ProgressResponseBody.ProgressListener listener = (bytesRead, contentLength, done) ->
                downloadProgress.onNext((float)((bytesRead/100.0)/(contentLength/100.0)));
        ArrayList<okhttp3.Protocol> protocols = new ArrayList<>();
        protocols.add(okhttp3.Protocol.HTTP_1_1);
        OkHttpClient client = CarbonPlayerApplication.Companion.getInstance().getOkHttpClient(new OkHttpClient.Builder()
                .protocols(protocols)
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), listener))
                            .build();
                }));

        Protocol.getStreamURL(context, songID.getNautilusID())
                .retry((tries, err) -> {
                    if (!(err instanceof ServerRejectionException)) return false;
                    if (((ServerRejectionException) err).getRejectionReason() !=
                            ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
                        return false;
                    return tries < 3;
                })
                .flatMap(url -> Completable.create(subscriber -> {
                    try {
                        Response response = client.newCall(new Request.Builder()
                                .url(url).build()).execute();
                        downloadRequest.setState(DownloadRequest.State.DOWNLOADING);

                        BufferedSink sink = Okio.buffer(Okio.sink(new File(filepath)));
                        Source source = response.body().source();

                        len = response.body().contentLength();
                        long writ = 0;

                        while(writ < len) {
                            sink.write(source, Math.min(2048, len-writ));
                            writ += Math.min(2048, len-writ);
                            synchronized (StreamingContent.this) {
                                notifyAll();
                            }
                            //downloadProgress.onNext(writ);
                        }
                        sink.close();
                        downloadRequest.setState(DownloadRequest.State.COMPLETED);
                        Timber.d("Download Completed");
                    } catch (IOException e) {
                        throw Exceptions.propagate(e);
                    }
                }).toSingle(() -> ""))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(context.getMainLooper()))
                .subscribe(x -> {
                    downloadRequest.setState(DownloadRequest.State.COMPLETED);
                    Timber.d("Download Completed");
                }, error -> {
                    Timber.e(error, "Exception getting stream URL");
                    downloadRequest.setState(DownloadRequest.State.FAILED);
                });

        downloadProgress.subscribe(val -> completed = (long)((len/10000f) * val) * 10000L);
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

    public synchronized void waitForData(long amount) throws InterruptedException {
        while (!isFinished() && this.completed < this.extraChunkSize + amount && this.waitAllowed) {
            long uptimeMs = SystemClock.uptimeMillis();
            if (lastWait + 10000 < uptimeMs) {
                this.lastWait = uptimeMs;
                Timber.i("waiting for %d bytes in file: %s", amount, filepath);
//                Timber.i("State: %s", downloadRequest.getState().name());
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
        Timber.d("StreamFile: location: %s", location);
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


    public void setWaitAllowed(boolean waitAllowed){
        this.waitAllowed = waitAllowed;
    }

    public synchronized boolean isFinished() {
        if(downloadRequest == null) return true;
        DownloadRequest.State state = downloadRequest.getState();
        return state == DownloadRequest.State.COMPLETED || state == DownloadRequest.State.CANCELED
                || state == DownloadRequest.State.FAILED;
    }

    public synchronized boolean isDownloaded() {
        if(downloadRequest == null) return true;
        DownloadRequest.State state = downloadRequest.getState();
        return state == DownloadRequest.State.COMPLETED;
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
