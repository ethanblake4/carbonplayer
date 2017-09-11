package com.carbonplayer.audio;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Rating;
import android.media.session.MediaSession;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.carbonplayer.R;
import com.carbonplayer.model.entity.ParcelableMusicTrack;
import com.carbonplayer.ui.main.MainActivity;
import com.carbonplayer.utils.Constants;

import org.parceler.Parcels;

import java.util.ArrayList;

import timber.log.Timber;

/**
 * Music player background service
 */
public final class MusicPlayerService extends Service implements MusicFocusable {

    public static final float DUCK_VOLUME = 0.1f;

    AudioFocusHelper audioFocusHelper = null;
    AudioFocus audioFocus;

    enum AudioFocus {
        NoFocusNoDuck,
        NoFocusCanDuck,
        Focused
    }

    private MediaSession mediaSession;

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    WifiManager.WifiLock wifiLock;

    private ArrayList<ParcelableMusicTrack> tracks;

    private NotificationCompat.Builder notificationBuilder;

    private Intent notificationIntent;
    private Intent previousIntent;
    private Intent playPauseIntent;
    private Intent nextIntent;

    private int currentTrack;
    private boolean isPlaying = false;

    private MusicPlayback playback;

    public ArrayList<Messenger> clients = new ArrayList<>();

    final Messenger mMessenger = new Messenger(new IncomingHandler());

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
                emit(Constants.EVENT.PrevSong);
                break;
            case Constants.ACTION.PLAYPAUSE:
                Timber.i("Clicked Play/Pause");
                isPlaying = !isPlaying;
                emit(isPlaying ? Constants.EVENT.Play : Constants.EVENT.Pause);
                updateNotification(tracks.get(currentTrack));
                if(isPlaying) {
                    playback.play();
                    audioFocusHelper.requestFocus();
                }
                else {
                    playback.pause();
                    audioFocusHelper.abandonFocus();
                }
                break;
            case Constants.ACTION.NEXT:
                Timber.i("Clicked Next");
                emit(Constants.EVENT.NextSong);
                break;
            case Constants.ACTION.SEND_QUEUE:
                Timber.i("Sending Queue");
                emit(Constants.EVENT.SendQueue);
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
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        previousIntent = new Intent(this, MusicPlayerService.class);
        previousIntent.setAction(Constants.ACTION.PREVIOUS);

        playPauseIntent = new Intent(this, MusicPlayerService.class);
        playPauseIntent.setAction(Constants.ACTION.PLAYPAUSE);

        nextIntent = new Intent(this, MusicPlayerService.class);
        nextIntent.setAction(Constants.ACTION.NEXT);

        Bundle bundle = intent.getExtras();
        tracks = Parcels.unwrap(bundle.getParcelable(Constants.KEY.INITITAL_TRACKS));

        playback = new MusicPlayback(this);
        playback.setup();
        playback.newQueue(tracks);

        mediaSession = new MediaSession(this, "CarbonMusic");
        mediaSession.setCallback(mediaSessionCallback);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);


        notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Carbon Player")
                .setContentText("Playing music")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        currentTrack = 0;

        updateNotification(tracks.get(currentTrack));
        playback.play();
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

    /*@Override
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
    }*/

    private void emit(int e) {
        switch(e) {
            case Constants.EVENT.SendQueue:
                emit(e, tracks);
                break;
            default: emit(e, null);
        }

    }

    private void emit(int e, Object obj){
        for (Messenger m: clients) {
            try {
                if(obj == null) m.send(Message.obtain(null, e));
                else m.send(Message.obtain(null, e, obj));
            } catch (RemoteException exception) {
                /* The client must've died */
                clients.remove(m);
            }
        }
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

    MediaSession.Callback mediaSessionCallback = new MediaSession.Callback() {
        @Override
        public void onCommand(@NonNull String command, @Nullable Bundle args,
                              @Nullable ResultReceiver cb) {
            super.onCommand(command, args, cb);
        }

        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        @Override
        public void onPrepare() {
            super.onPrepare();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            super.onPrepareFromSearch(query, extras);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
        }

        @Override
        public void onPlay() {
            super.onPlay();
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

        @Override
        public void onSetRating(@NonNull Rating rating) {
            super.onSetRating(rating);
        }

        @Override
        public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {
            super.onCustomAction(action, extras);
        }
    };

}
