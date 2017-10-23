package com.carbonplayer.utils.general;

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
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;

import com.carbonplayer.model.entity.enums.NetworkType;
import com.carbonplayer.utils.protocol.Gservices;
import com.google.protobuf.Duration;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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

    public static @Px int displayWidth2(@NonNull Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    public static @Px int displayHeight2(@NonNull Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.heightPixels;
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
        int id = res.getIdentifier("config_showNavigationBar", "bool", "android");
        if(id > 0 && res.getBoolean(id)) {
            int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                return res.getDimensionPixelSize(resourceId);
            }
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

    public static @NonNull String getGservicesId(@NonNull Context context, boolean fallback) {
        String gId = String.valueOf(Gservices.getLong(
                context.getContentResolver(), "android_id", 0));
        if (gId.equals("0")) return fallback ? deviceId(context) : "0";
        return gId;
    }

    public static boolean getDeviceIsSmartphone(@NonNull Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        float yInches= metrics.heightPixels/metrics.ydpi;
        float xInches= metrics.widthPixels/metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches*xInches + yInches*yInches);
        return (diagonalInches <= 6.5);
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

    public static Duration getTimezoneOffsetProtoDuration() {
        return Duration.newBuilder()
                .setSeconds(TimeUnit.MILLISECONDS.toSeconds(TimeZone.getDefault().getOffset(new Date().getTime())))
                .build();
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
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo();

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
