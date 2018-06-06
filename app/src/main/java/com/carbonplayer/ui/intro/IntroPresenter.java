package com.carbonplayer.ui.intro;

import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.exception.NeedsBrowserException;
import com.carbonplayer.model.entity.exception.NoNautilusException;
import com.carbonplayer.model.entity.exception.SjNotSupportedException;
import com.carbonplayer.model.network.GoogleLogin;
import com.carbonplayer.utils.ExtensionsKt;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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

    IntroPresenter(@NonNull IntroActivity activity) {
        mActivity = activity;
    }

    void setAuthDialog(@NonNull Dialog authDialog) {
        this.authDialog = authDialog;
    }

    void callbackWithUsername(String u) {
        username = u;
        if (username != null && password != null) {
            tryLogin(username, password);
        }
    }

    void callbackWithPassword(String p) {
        password = p;
        if (username != null && password != null) {
            tryLogin(username, password);
        }
    }

    void tryLogin(String username, String password) {
        Timber.d("retrieved username=|%s| and password=|%s|", username, password);
        if (!username.contains("@")) username = username + "@gmail.com";

        this.username = username;

        try {
            authDialog.dismiss();
        } catch (NullPointerException e) {
            Timber.e(e.toString());
        }
        mActivity.enableSwitching = true;
        mActivity.runOnUiThread(() -> mActivity.mPager.setCurrentItem(3));
        mActivity.web = null;
        authDialog = null;

        ExtensionsKt.addToAutoDispose(GoogleLogin.login(mActivity, username, password, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::doConfig,
                        this::handleLoginException
                ));
    }

    void tryContinueLogin(String token) {
        Timber.d("retrieved username=|%s| and token=|%s|", username, token);
        if (!username.contains("@")) username = username + "@gmail.com";

        mActivity.web = null;
        authDialog = null;

        ExtensionsKt.addToAutoDispose(GoogleLogin.login(mActivity, username, "", token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::doConfig,
                        this::handleLoginException
                ));
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR) {
            if (resultCode == RESULT_OK) {
                ExtensionsKt.addToAutoDispose(GoogleLogin.retryGoogleAuth(mActivity, username)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::doConfig,
                                this::handleLoginException
                        ));
            } else {
                Timber.e("Play Services Recover failure");
                mActivity.makeLibraryError(R.string.intro_slide3_issue);
            }
        }
    }

    private void handleLoginException(Throwable t) {
        if (t instanceof GooglePlayServicesAvailabilityException) {
            // The Google Play services APK is old, disabled, or not present.
            // Show a dialog created by Google Play services that allows
            // the user to update the APK
            mActivity.runOnUiThread(() -> {
                int statusCode = ((GooglePlayServicesAvailabilityException) t)
                        .getConnectionStatusCode();
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                        mActivity,
                        REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                dialog.show();
            });
        } else if (t instanceof UserRecoverableAuthException) {
            mActivity.runOnUiThread(() -> {
                Intent intent = ((UserRecoverableAuthException) t).getIntent();
                mActivity.startActivityForResult(intent,
                        REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
            });
        } else if(t instanceof NeedsBrowserException) {
            Timber.d(((NeedsBrowserException) t).getUrl());
            Dialog dialog =
                    mActivity.showWebDialog(((NeedsBrowserException) t).getUrl(), false);
            dialog.show();
            dialog.setCancelable(false);
        } else if (t instanceof SjNotSupportedException) {
            Timber.e(t, "Skyjam not supported");
            t.printStackTrace();
            mActivity.makeLibraryError(R.string.err_sj_not_supported);
        } else {
            Timber.e(t, "Login Exception unhandled");
            t.printStackTrace();
            mActivity.makeLibraryError(R.string.intro_slide3_issue);
        }
    }

    private void doConfig() {
        if (!jsonCallbackCompleted) {
            jsonCallbackCompleted = true;
            MusicLibrary.INSTANCE.config(mActivity,
                    t -> {
                        Timber.e(t, "Login Exception in doConfig");
                        if (t instanceof NoNautilusException) {
                            if( CarbonPlayerApplication.instance.preferences.isCarbonTester ) {
                                promptTesterCode();
                            } else {
                                mActivity.makeLibraryError(R.string.intro_slide3_no_nautilus);
                            }
                        }
                        else mActivity.makeLibraryError(R.string.intro_slide3_issue);
                    }, this::getLibrary);
        }
    }

    void promptTesterCode() {
        Dialog dialog = mActivity.showTesterCodeDialog();
        dialog.show();
        dialog.setCancelable(false);
    }

    void continueAfterTesterCode() {
        ExtensionsKt.addToAutoDispose(GoogleLogin.testLogin(mActivity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::getLibrary,
                        this::handleLoginException
                ));
    }

    private void getLibrary() {
        mActivity.slide3Progress(false, 0);
        MusicLibrary.INSTANCE.getMusicLibrary(mActivity,
            t -> {
                t.printStackTrace();
                Timber.e(t);
                mActivity.makeLibraryError(R.string.intro_slide3_issue);
            },
            false,
            i -> mActivity.slide3Progress(i.first, i.second),
            mActivity::endSuccessfully);
    }

}
