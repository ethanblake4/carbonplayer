package com.carbonplayer.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Px;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.Display;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Utilities for identifying device and device details
 */
public class IdentityUtils {

    public static @Px int displayWidth(@NonNull Activity context){
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

     public static @Px int displayHeight(@NonNull Activity context){
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    public static int displayWidthDp(@NonNull Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        //float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return (int)dpWidth;
    }

    @SuppressLint("HardwareIds")
    public static String deviceId(@NonNull Context context){
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public static boolean getDeviceIsSmartphone(@NonNull Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        float yInches= metrics.heightPixels/metrics.ydpi;
        float xInches= metrics.widthPixels/metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches*xInches + yInches*yInches);
        if (diagonalInches>=6.5){
            return false;
        }else{
            return true;
        }
    }

    public static String getLoggingID(Context context){
        SharedPreferences getPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        String loggingID = getPrefs.getString("logging_id", "");
        if(loggingID.equals("")){
            loggingID = Long.toHexString(new SecureRandom().nextLong());
            getPrefs.edit().putString("logging_id", loggingID).apply();
        }
        return loggingID;
    }

    public static String localeCode(){
        return Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
    }
}
