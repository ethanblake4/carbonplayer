package com.carbonplayer.ui.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.carbonplayer.R;
import com.carbonplayer.audio.TrackQueue;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.utils.Constants;

import java.util.List;

/**
 * Manages now playing UI across activities and sends commands to {@link com.carbonplayer.audio.MusicPlayerService}
 */
public final class NowPlayingUIHelper {
    private Activity mActivity;
    private Messenger mMessenger;

    private LinearLayout mainFrame;
    private ImageView thumb;
    private ConstraintLayout detailsView;
    private ImageView playPause;

    public NowPlayingUIHelper(Activity activity){
        mActivity = activity;

        mainFrame   = (LinearLayout)     mActivity.findViewById(R.id.nowplaying_main);
        thumb       = (ImageView)        mActivity.findViewById(R.id.nowplaying_thumb);
        detailsView = (ConstraintLayout) mActivity.findViewById(R.id.nowplaying_details);
        playPause   = (ImageView)        mActivity.findViewById(R.id.nowplaying_playpause);
    }

    public ConstraintLayout getDetailsView() { return detailsView; }

    public void newQueue(List<MusicTrack> tracks){
        Glide.with(mActivity)
                .load(TrackQueue.instance(tracks, true).currentTrack().getAlbumArtURL())
                .into(thumb);
    }

    public void makePlayingScreen(){
        mainFrame.setVisibility(View.VISIBLE);
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        //TranslateAnimation anim = new TranslateAnimation(0,0, IdentityUtils.displayHeight(mActivity), 0);
        anim.setDuration(500);
        anim.setFillAfter(true);
        mainFrame.startAnimation(anim);
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

    @SuppressLint("HandlerLeak")
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.UIACTION.CLICK_PREVIOUS:
                    TrackQueue.instance().prevtrack();
                    updateDrawable();
                    break;
                case Constants.UIACTION.CLICK_NEXT:
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


}
