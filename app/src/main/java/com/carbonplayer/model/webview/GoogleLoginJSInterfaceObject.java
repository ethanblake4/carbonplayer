package com.carbonplayer.model.webview;

import android.webkit.JavascriptInterface;

import com.carbonplayer.ui.intro.IntroActivity;

import timber.log.Timber;

/**
 * Receives and passes on messages from Javascript
 */
public class GoogleLoginJSInterfaceObject {
    private IntroActivity callingActivity;

    public GoogleLoginJSInterfaceObject(IntroActivity i) {
        callingActivity = i;
    }

    @JavascriptInterface
    public void returnUsername(String username) {
        Timber.d("Retrieved username");
        callingActivity.callbackWithUsername(username);
    }

    @JavascriptInterface
    public void returnPassword(String password) {
        Timber.d("Retrieved password");
        callingActivity.callbackWithPassword(password);
    }
}