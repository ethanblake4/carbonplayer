package com.carbonplayer.utils;

import android.content.Context;
import android.util.Base64;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utilities for Javascript
 */
public class JavascriptUtils {

    /**
     * encodes Javascript file in Base64 and injects into webview
     * @param context context to use for opening file
     * @param webview webview to inject file into
     * @param pathToFile path to javascript file
     */
    public static void injectCovertly(Context context, WebView webview,  String pathToFile) {
        try {
            InputStream input = context.getAssets().open(pathToFile);
            byte[] buffer = new byte[input.available()];
            //noinspection ResultOfMethodCallIgnored
            input.read(buffer);
            input.close();

            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);

            injectJavascript(webview,
                    "(function() {var parent=document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type='text/javascript';" +
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Evaluates javascript on webview
     * @param webview WebView to use
     * @param javascript Javascript to inject
     */
    public static void injectJavascript(WebView webview, String javascript) {
        if (webview != null) {
            webview.evaluateJavascript(javascript, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {}
            });
        }
    }
}
