package com.carbonplayer.model.webview;

import android.webkit.JavascriptInterface;

import com.carbonplayer.ui.intro.IntroActivity;

/**
 * Receives and passes on messages from Javascript
 */
public class GoogleLoginJSInterfaceObject {
    private IntroActivity callingActivity;

    public GoogleLoginJSInterfaceObject(IntroActivity i) {
        callingActivity = i;
    }

    @JavascriptInterface
    public void returnResult(String message) {
        if (message.startsWith("{")) {
            callingActivity.callbackWithJson(message);
        }
    }
}
