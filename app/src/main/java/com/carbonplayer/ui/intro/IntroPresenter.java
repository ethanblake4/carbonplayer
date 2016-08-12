package com.carbonplayer.ui.intro;

import android.app.Dialog;
import android.support.annotation.NonNull;
import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.exception.NoNautilusException;
import com.carbonplayer.model.network.GoogleLogin;

import org.json.JSONException;
import org.json.JSONObject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Presenter for IntroActivity
 */
class IntroPresenter {

    private IntroActivity mActivity;
    private Dialog authDialog;

    private String username;
    private String password;

    IntroPresenter(@NonNull IntroActivity activity){
        mActivity = activity;
    }

    void setAuthDialog(@NonNull Dialog authDialog){
        this.authDialog = authDialog;
    }

    void callbackWithJson(String json) {
        try {
            JSONObject j = new JSONObject(json);
            String candidateType = j.getString("candidateType");
            if (candidateType.equals("field")) {
                String fieldId = j.getString("fieldId");
                if (fieldId.equals("Email")) username = j.getString("value");
                if (fieldId.equals("Passwd")) password = j.getString("value");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (username != null && password != null) {
            Timber.d("retrieved username and password");
            if (!username.contains("@")) username = username + "@gmail.com";

            authDialog.dismiss();
            mActivity.enableSwitching = true;
            mActivity.runOnUiThread(() -> mActivity.mPager.setCurrentItem(3));
            mActivity.web = null;
            authDialog = null;

            GoogleLogin.login(mActivity, username, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                n -> {},
                e -> mActivity.makeLibraryError(R.string.intro_slide3_issue),
                this::doConfig
            );
        }
    }

    private void doConfig(){
        MusicLibrary.getInstance().config(mActivity,
            t -> {
                if(t instanceof NoNautilusException)
                    mActivity.makeLibraryError(R.string.intro_slide3_no_nautilus);
                else mActivity.makeLibraryError(R.string.intro_slide3_issue);
            }, this::getLibrary);
    }

    private void getLibrary(){
        mActivity.slide3Progress(0);
        MusicLibrary.getInstance().updateMusicLibrary(mActivity,
            t -> mActivity.makeLibraryError(R.string.intro_slide3_issue),
            i -> mActivity.slide3Progress(i),
            mActivity::endSuccessfully);
    }

}
