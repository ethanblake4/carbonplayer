package com.carbonplayer.ui.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.carbonplayer.R;
import com.carbonplayer.audio.MusicPlayerService;
import com.carbonplayer.audio.TrackQueue;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.utils.Constants;

import org.parceler.Parcels;

import java.util.List;

import timber.log.Timber;

/**
 * Manages now playing UI across activities and sends commands to {@link com.carbonplayer.audio.MusicPlayerService}
 */
public final class NowPlayingUIHelper {

    private static NpUIHelperManager npUIHelperManager;

    private Activity mActivity;
    private Messenger mMessenger;

    private LinearLayout mainFrame;
    private ImageView thumb;
    private ConstraintLayout detailsView;
    private ImageView playPause;

    private TrackQueue queue;

    public NowPlayingUIHelper(Activity activity){
        mActivity = activity;

        if(npUIHelperManager == null) npUIHelperManager = new NpUIHelperManager();

        mainFrame   = (LinearLayout)     mActivity.findViewById(R.id.nowplaying_main);
        thumb       = (ImageView)        mActivity.findViewById(R.id.nowplaying_thumb);
        detailsView = (ConstraintLayout) mActivity.findViewById(R.id.nowplaying_details);
        playPause   = (ImageView)        mActivity.findViewById(R.id.nowplaying_playpause);

        if(npUIHelperManager.uiVisible){
            mainFrame.setVisibility(View.VISIBLE);
        }
    }


    public ConstraintLayout getDetailsView() { return detailsView; }

    public void newQueue(List<MusicTrack> tracks){
        queue = TrackQueue.instance(tracks, true);
        Glide.with(mActivity)
                .load(queue.currentTrack().getAlbumArtURL())
                .into(thumb);
        mActivity.startService(buildServiceIntent());
    }

    public void makePlayingScreen(){
        mainFrame.setVisibility(View.VISIBLE);
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        //TranslateAnimation anim = new TranslateAnimation(0,0, IdentityUtils.displayHeight(mActivity), 0);
        anim.setDuration(500);
        anim.setFillAfter(true);
        mainFrame.startAnimation(anim);
        mActivity.startService(buildServiceIntent());
    }

    public void makePlayingScreen(Drawable drawable){
        mainFrame.setVisibility(View.VISIBLE);
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        //TranslateAnimation anim = new TranslateAnimation(0,0, IdentityUtils.displayHeight(mActivity), 0);
        anim.setDuration(500);
        anim.setFillAfter(true);
        mainFrame.startAnimation(anim);
        thumb.setImageDrawable(drawable);
    }

    public void updateDrawable(){
        String url = TrackQueue.instance().currentTrack().getAlbumArtURL();
        Glide.with(mActivity).load(url).into(thumb);
    }

    private Intent buildServiceIntent(){
        Intent i = new Intent(mActivity, MusicPlayerService.class);
        i.setAction(Constants.ACTION.START_SERVICE);
        i.putExtra(Constants.KEY.INITITAL_TRACKS, Parcels.wrap(queue.getParcelable()));
        mActivity.bindService(i, mConnection, Context.BIND_DEBUG_UNBIND);
        return i;
    }

    @SuppressLint("HandlerLeak")
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.EVENT.BufferProgress:
                    Timber.d("Received bufferProgress %f", (Float) msg.obj);
                    break;
                case Constants.EVENT.NextSong:
                    TrackQueue.instance().prevtrack();
                    updateDrawable();
                    break;
                case Constants.EVENT.PrevSong:
                    TrackQueue.instance().nexttrack();
                    updateDrawable();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMessenger = new Messenger(service);
            try {
                Message msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT);
                msg.replyTo = new Messenger(new IncomingHandler());
                mMessenger.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
                mMessenger = null;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mMessenger = null;
        }
    };

    private class NpUIHelperManager {
        public boolean uiVisible = false;

    }


}
