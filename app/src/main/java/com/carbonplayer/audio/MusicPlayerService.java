package com.carbonplayer.audio;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
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

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.ParcelableMusicTrack;
import com.carbonplayer.model.network.Protocol;
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
import java.util.ArrayList;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Music player background service
 */
public final class MusicPlayerService extends Service implements TrackSelector.EventListener<MappingTrackSelector.MappedTrackInfo>, ExoPlayer.EventListener {

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

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    Handler mainHandler;

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
                updatePlayer();
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
        updatePlayer();
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

    private void updatePlayer(){
        Timber.d("updatePlayer called: %s", tracks.get(currentTrack).getId());
        //if(isPlaying){
            //player = ExoPlayer.Factory.newInstance(1);
            Protocol.getStreamURL(this, tracks.get(currentTrack).getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.from(getMainLooper()))
                    .subscribe(url -> {
                        Timber.d("Stream Url retrieved: %s", url);
                        MediaSource mediaSource = buildMediaSource(new Uri.Builder().path(url).build(), "");
                        player.prepare(mediaSource, true);
                        //player.prepare();
                    }, error -> Timber.d(error, "getstreamURL"));
            //player.prepare(new ExtractorSampleSource());
        //}
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
        String errorString = null;
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            Exception cause = e.getRendererException();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                        (MediaCodecRenderer.DecoderInitializationException) cause;
                if (decoderInitializationException.decoderName == null) {
                    if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                    //    errorString = getString(R.string.error_querying_decoders);
                    } else if (decoderInitializationException.secureDecoderRequired) {
                    //    errorString = getString(R.string.error_no_secure_decoder,
                    //            decoderInitializationException.mimeType);
                    } else {
                    //    errorString = getString(R.string.error_no_decoder,
                    //            decoderInitializationException.mimeType);
                    }
                } else {
                    //errorString = getString(R.string.error_instantiating_decoder,
                    //        decoderInitializationException.decoderName);
                }
            }
        }
        if (errorString != null) {
            //showToast(errorString);
        }
        //playerNeedsSource = true;
        //updateButtonVisibilities();
        //showControls();
    }

    enum Event {
        NextSong, PrevSong, Play, Pause, SendQueue
    }

    private void emit(Event e) {
        Message msg;
        switch(e) {
            case SendQueue:
                msg = Message.obtain(null, e.ordinal(), tracks);
                break;
            default:
                msg = Message.obtain(null, e.ordinal());
        }
        for (Messenger m : clients) {
            try {
                m.send(msg);
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
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        switch (type) {
            /*case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, new ExtractorMediaSource.EventListener());*/
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, error -> Timber.e("Error", error));
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
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
