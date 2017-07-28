package com.carbonplayer.audio;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.PlaybackParams;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.util.ArrayMap;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.ParcelableMusicTrack;
import com.carbonplayer.model.entity.exception.ServerRejectionException;
import com.carbonplayer.model.network.Protocol;
import com.carbonplayer.model.network.StreamManager;
import com.carbonplayer.ui.main.MainActivity;
import com.carbonplayer.utils.Constants;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelections;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;

import org.parceler.Parcels;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Music player background service
 */
public final class MusicPlayerService extends Service
        implements TrackSelector.EventListener<MappingTrackSelector.MappedTrackInfo>, ExoPlayer.EventListener, MusicFocusable {

    public static final float DUCK_VOLUME = 0.1f;

    AudioFocusHelper audioFocusHelper = null;
    AudioFocus audioFocus;

    private StreamManager streamManager;

    enum AudioFocus {
        NoFocusNoDuck,
        NoFocusCanDuck,
        Focused
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    WifiManager.WifiLock wifiLock;

    private ArrayList<ParcelableMusicTrack> tracks;
    private SimpleExoPlayer player;

    private NotificationCompat.Builder notificationBuilder;

    private Intent notificationIntent;
    private Intent previousIntent;
    private Intent playPauseIntent;
    private Intent nextIntent;

    private int currentTrack;
    private boolean isPlaying = false;

    public ArrayList<Messenger> clients;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private MappingTrackSelector trackSelector;
    private DataSource.Factory mediaDataSourceFactory;

    private long mCurrentExpireTimestamp;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    Handler mainHandler;

    @Override
    public void onCreate() {
        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "carbon");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case Constants.ACTION.START_SERVICE:
                initService(intent);
                break;
            case Constants.ACTION.PREVIOUS:
                Timber.i("Clicked Previous");
                emit(Event.PrevSong);
                break;
            case Constants.ACTION.PLAYPAUSE:
                Timber.i("Clicked Play/Pause");
                isPlaying = !isPlaying;
                emit(isPlaying ? Event.Play : Event.Pause);
                updateNotification(tracks.get(currentTrack));
                updatePlayer(true);
                break;
            case Constants.ACTION.NEXT:
                Timber.i("Clicked Next");
                emit(Event.NextSong);
                break;
            case Constants.ACTION.SEND_QUEUE:
                Timber.i("Sending Queue");
                emit(Event.SendQueue);
            case Constants.ACTION.STOP_SERVICE:
                Timber.i("Received Stop Foreground Intent");
                stopForeground(true);
                stopSelf();
                break;
        }
        return START_STICKY;
    }

    private void initService(Intent intent){

        notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        previousIntent = new Intent(this, MusicPlayerService.class);
        previousIntent.setAction(Constants.ACTION.PREVIOUS);

        playPauseIntent = new Intent(this, MusicPlayerService.class);
        playPauseIntent.setAction(Constants.ACTION.PLAYPAUSE);

        nextIntent = new Intent(this, MusicPlayerService.class);
        nextIntent.setAction(Constants.ACTION.NEXT);

        Bundle bundle = intent.getExtras();
        tracks = Parcels.unwrap(bundle.getParcelable(Constants.KEY.INITITAL_TRACKS));

        // build notification
        notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Carbon Player")
                .setContentText("Playing music")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        currentTrack = 0;

        try {
            streamManager = new StreamManager(tracks);
        } catch (Exception e) {
            e.printStackTrace();
            Timber.e(e, "Exception while creating StreamManager");
        }

        // 1. Create a default TrackSelector
        mediaDataSourceFactory = buildDataSourceFactory(true);
        mainHandler = new Handler();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
        trackSelector.addListener(this);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl());

        player.addListener(this);

        updateNotification(tracks.get(currentTrack));
        updatePlayer(true);
    }

    private void updateNotification(ParcelableMusicTrack track){

        final RemoteViews rv = new RemoteViews(getPackageName(), R.layout.small_notification);

        //rv.setImageViewResource(R.id.remoteview_notification_icon, R.mipmap.future_studio_launcher);

        rv.setTextViewText(R.id.sn_songtitile, track.getTitle());
        rv.setTextViewText(R.id.sn_artist, track.getArtist());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0, previousIntent, 0);
        PendingIntent pplayPauseIntent = PendingIntent.getService(this, 0, playPauseIntent, 0);
        PendingIntent pnextIntent = PendingIntent.getService(this, 0, nextIntent, 0);

        rv.setOnClickPendingIntent(R.id.sn_previous, ppreviousIntent);
        rv.setOnClickPendingIntent(R.id.sn_playpause, pplayPauseIntent);
        rv.setOnClickPendingIntent(R.id.sn_next, pnextIntent);

        // build notification
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle("Carbon Player")
                .setContentText("Playing music")
                //.setContent(rv)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority( NotificationCompat.PRIORITY_DEFAULT );

        startForeground(Constants.ID.MUSIC_PLAYER_SERVICE,
                notificationBuilder.build());
    }

    private void releasePlayer() {
        if (player != null) {
            Timeline timeline = player.getCurrentTimeline();
            //if (timeline != null) {
                //playerWindow = player.getCurrentWindowIndex();
                //Timeline.Window window = timeline.getWindow(playerWindow, new Timeline.Window());
                //if (!window.isDynamic) {
                //    shouldRestorePosition = true;
                //    playerPosition = window.isSeekable ? player.getCurrentPosition() : C.TIME_UNSET;
                //}
            //}
            player.release();
            player = null;
            trackSelector = null;
            //trackSelectionHelper = null;
            //eventLogger = null;
        }
    }

    private void updatePlayer(boolean resetPosition){
        Timber.d("updatePlayer called: %s", tracks.get(currentTrack).getId());
        //if(isPlaying){
            //player = ExoPlayer.Factory.newInstance(1);
        streamManager.getLocalStreamUrlForCurrentTrack(this)
                .subscribeOn(Schedulers.io())
                /*.retry((retries, throwable) -> {
                    if(throwable instanceof ServerRejectionException) {
                        ServerRejectionException exception = (ServerRejectionException) throwable;
                        switch(exception.getRejectionReason()){
                            case DEVICE_NOT_AUTHORIZED:
                                if (retries < 2) return true;
                            default: {}
                        }
                    }
                    return false;
                })*/
                .observeOn(AndroidSchedulers.from(getMainLooper()))
                .subscribe(pair -> {
                    String url = pair.first;
                    pair.second.subscribe(f -> Timber.d("Progress: %f", f));
                    Timber.d("Local stream Url retrieved: %s", url);

                    /*try {
                        Uri uri=Uri.parse(url);
                        //mCurrentExpireTimestamp = Long.parseLong(uri.getQueryParameter("expire")); // get your value
                    } catch(Exception e) {
                        e.printStackTrace();
                        //mCurrentExpireTimestamp = (System.currentTimeMillis()/1000) + 100;
                    }*/

                    MediaSource mediaSource = buildMediaSource(Uri.parse(url), "mp3");
                    player.prepare(mediaSource, resetPosition);
                    if(resetPosition) player.setPlayWhenReady(true);
                }, error -> emit(Event.Error, error));
        /*
            Protocol.getStreamURL(this, tracks.get(currentTrack).getId())
                    .subscribeOn(Schedulers.io())
                    .retry((retries, throwable) -> {
                        if(throwable instanceof ServerRejectionException) {
                            ServerRejectionException exception = (ServerRejectionException) throwable;
                            switch(exception.getRejectionReason()){
                                case DEVICE_NOT_AUTHORIZED:
                                    if (retries < 2) return true;
                                default: {}
                            }
                        }
                        return false;
                    })
                    .observeOn(AndroidSchedulers.from(getMainLooper()))
                    .subscribe(url -> {
                        Timber.d("Stream Url retrieved: %s", url);

                        try {
                            Uri uri=Uri.parse(url);
                            mCurrentExpireTimestamp = Long.parseLong(uri.getQueryParameter("expire")); // get your value
                        } catch(Exception e) {
                            e.printStackTrace();
                            mCurrentExpireTimestamp = (System.currentTimeMillis()/1000) + 100;
                        }

                        MediaSource mediaSource = buildMediaSource(Uri.parse(url), "");
                        player.prepare(mediaSource, resetPosition);
                        if(resetPosition) player.setPlayWhenReady(true);
                    }, error -> emit(Event.Error, error));*/
    }

    @Override
    public void onTrackSelectionsChanged(TrackSelections<? extends MappingTrackSelector.MappedTrackInfo> trackSelections) {
        /*updateButtonVisibilities();
        MappedTrackInfo trackInfo = trackSelections.info;
        if (trackInfo.hasOnlyUnplayableTracks(C.TRACK_TYPE_VIDEO)) {
            showToast(R.string.error_unsupported_video);
        }
        if (trackInfo.hasOnlyUnplayableTracks(C.TRACK_TYPE_AUDIO)) {
            showToast(R.string.error_unsupported_audio);
        }*/
    }
    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            //showControls();
        }
        //updateButtonVisibilities();
    }

    @Override
    public void onPositionDiscontinuity() {
        // Do nothing.
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        // Do nothing.
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        String errorString = "Unknown Error";
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            Exception cause = e.getRendererException();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                        (MediaCodecRenderer.DecoderInitializationException) cause;
                if (decoderInitializationException.decoderName == null) {
                    if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                    //    errorString = getString(R.string.error_querying_decoders);
                        errorString = "Error Querying Decoders";
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        errorString = "Secure Decoder Required";
                    //    errorString = getString(R.string.error_no_secure_decoder,
                    //            decoderInitializationException.mimeType);
                    } else {
                        errorString = "No Decoder Found";
                    //    errorString = getString(R.string.error_no_decoder,
                    //            decoderInitializationException.mimeType);
                    }
                } else {
                    errorString = "Error Instantiating Decoder";
                    //errorString = getString(R.string.error_instantiating_decoder,
                    //        decoderInitializationException.decoderName);
                }
            }
        }
        if (errorString != null) {
            Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
            //showToast(errorString);
        }
        //playerNeedsSource = true;
        //updateButtonVisibilities();
        //showControls();
    }

    enum Event {
        NextSong, PrevSong, Play, Pause, SendQueue, Error
    }

    private void emit(Event e) {
        switch(e) {
            case SendQueue:
                emit(e, tracks);
                break;
            default: emit(e, null);
        }

    }

    private void emit(Event e, Object obj){
        for (Messenger m: clients) {
            try {
                if(obj == null) m.send(Message.obtain(null, e.ordinal()));
                else m.send(Message.obtain(null, e.ordinal(), obj));
            } catch (RemoteException exception) {
                /* The client must've died */
                clients.remove(m);
            }
        }
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return ((CarbonPlayerApplication) getApplication())
                .buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {

        ExtractorMediaSource mm = new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                mainHandler, error -> {
                    Timber.e("Error", error);
                    Timber.d("mcurrentexpireTimestamp: %d vs time: %d", mCurrentExpireTimestamp, System.currentTimeMillis()/1000);
                    if(mCurrentExpireTimestamp <= System.currentTimeMillis()/1000) {
                        //updatePlayer(false);
                    }
        });

        return mm;
    }

    public void onGainedAudioFocus(){

    }

    public void onLostAudioFocus(boolean canDuck) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.i("In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.REGISTER_CLIENT:
                    clients.add(msg.replyTo);
                    break;
                case Constants.MESSAGE.UNREGISTER_CLIENT:
                    clients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
