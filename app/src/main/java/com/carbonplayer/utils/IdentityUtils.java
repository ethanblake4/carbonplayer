package com.carbonplayer.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.provider.Settings;
import android.view.Display;

import java.util.Locale;

/**
 * Utilities for identifying device and device details
 */
public class IdentityUtils {

    public static int displayWidth(Activity context){
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    @SuppressLint("HardwareIds")
    public static String deviceId(Context context){
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public static String localeCode(){
        return Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
    }
}
