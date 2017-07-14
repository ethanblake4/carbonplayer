package com.carbonplayer.ui.intro;

import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.exception.NoNautilusException;
import com.carbonplayer.model.network.GoogleLogin;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

/**
 * Presenter for IntroActivity
 */
class IntroPresenter {

    private IntroActivity mActivity;
    private Dialog authDialog;

    private String username;
    private String password;

    private boolean jsonCallbackCompleted = false;
    private final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 547;

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
            tryLogin(username, password);
        }
    }

    void tryLogin(String username, String password){
        Timber.d("retrieved username=%s and password=%s", username, password);
        if (!username.contains("@")) username = username + "@gmail.com";

        try{
            authDialog.dismiss();
        } catch (NullPointerException e) {
            Timber.e(e.toString());
        }
        mActivity.enableSwitching = true;
        mActivity.runOnUiThread(() -> mActivity.mPager.setCurrentItem(3));
        mActivity.web = null;
        authDialog = null;

        GoogleLogin.login(mActivity, username, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    n -> {},
                    this::handleLoginException,
                    this::doConfig
            );
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR){
            if (resultCode == RESULT_OK){
                GoogleLogin.retryGoogleAuth(mActivity, username)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            n->{},
                            this::handleLoginException,
                            this::doConfig
                    );
            } else {
                mActivity.makeLibraryError(R.string.intro_slide3_issue);
            }
        }
    }

    private void handleLoginException(Throwable t){
        if (t instanceof GooglePlayServicesAvailabilityException) {
            // The Google Play services APK is old, disabled, or not present.
            // Show a dialog created by Google Play services that allows
            // the user to update the APK
            mActivity.runOnUiThread(()-> {
                int statusCode = ((GooglePlayServicesAvailabilityException) t)
                        .getConnectionStatusCode();
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                        mActivity,
                        REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                dialog.show();
            });
        } else if (t instanceof UserRecoverableAuthException) {
            mActivity.runOnUiThread(()-> {
                Intent intent = ((UserRecoverableAuthException) t).getIntent();
                mActivity.startActivityForResult(intent,
                        REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
            });
        } else {
            t.printStackTrace();
            mActivity.makeLibraryError(R.string.intro_slide3_issue);
        }
    }

    private void doConfig(){
        if(!jsonCallbackCompleted) {
            jsonCallbackCompleted = true;
            MusicLibrary.getInstance().config(mActivity,
                    t -> {
                        if (t instanceof NoNautilusException)
                            mActivity.makeLibraryError(R.string.intro_slide3_no_nautilus);
                        else mActivity.makeLibraryError(R.string.intro_slide3_issue);
                    }, this::getLibrary);
        }
    }

    private void getLibrary(){
        mActivity.slide3Progress(0);
        MusicLibrary.getInstance().updateMusicLibrary(mActivity,
            t -> mActivity.makeLibraryError(R.string.intro_slide3_issue),
            i -> mActivity.slide3Progress(i),
            mActivity::endSuccessfully);
    }

}
