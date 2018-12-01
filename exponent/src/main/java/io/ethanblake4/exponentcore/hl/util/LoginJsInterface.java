package io.ethanblake4.exponentcore.hl.util;

import android.util.Log;
import android.webkit.JavascriptInterface;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class LoginJsInterface {
    private String username;
    private Function2<String, String, Unit> callback;

    public LoginJsInterface(Function2<String, String, Unit> cb) {
       this.callback = cb;
    }

    @JavascriptInterface
    public void returnUsername(String username) {
        Log.d("RETURN USERNAME", username);
        this.username = username;
    }

    @JavascriptInterface
    public void returnPassword(String password) {
        Log.d("RETURN PASS", password);
        this.callback.invoke(username, password);
    }
}
