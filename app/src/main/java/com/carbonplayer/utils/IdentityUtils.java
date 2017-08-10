package com.carbonplayer.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Px;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;

import com.carbonplayer.model.entity.enums.NetworkType;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Utilities for identifying device and device details
 */
public final class IdentityUtils {

    public static @Px int displayWidth(@NonNull Activity context){
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public static int getStatusBarHeight(Resources res) {
        int result = 0;
        int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavbarHeight(Resources res) {
        int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return res.getDimensionPixelSize(resourceId);
        }
        return 0;
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

    public static String getDeviceLanguage(){
        return Locale.getDefault().getLanguage();
    }

    public static String getDeviceCountryCode(){
        return Locale.getDefault().getCountry().toLowerCase();
    }

    public static String getOperatorCountryCode(Context context){
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimCountryIso().toLowerCase();
    }

    public static NetworkType networkType(Context context) {
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_MOBILE: return NetworkType.MOBILE;
                case ConnectivityManager.TYPE_WIFI: return NetworkType.WIFI;
                case ConnectivityManager.TYPE_ETHERNET: return NetworkType.ETHER;
                default: return NetworkType.UNKNOWN;
            }
        }

        return NetworkType.DISCONNECTED;
    }
}
