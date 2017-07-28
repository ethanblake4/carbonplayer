package com.carbonplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.carbonplayer.model.entity.enums.StreamQuality;

import java.lang.reflect.Field;

import okhttp3.CookieJar;
import timber.log.Timber;

/**
 * Preferences, enables saving and loading data through reflection
 */

public class Preferences {

    private Context appContext;

    /*Shared Variables**/

    //Device
    public String userEmail;

    public StreamQuality preferredStreamQualityWifi = StreamQuality.MEDIUM;
    public StreamQuality preferredStreamQualityMobile = StreamQuality.MEDIUM;

    public String BearerAuth;
    public String OAuthToken;

    public int maxAudioCacheSizeMB = 1024;

    public Preferences(Context context){
        appContext = context;
    }

    public StreamQuality getPreferredStreamQuality(Context context){
        StreamQuality preferred;
        switch(IdentityUtils.networkType(context)) {
            case WIFI:
            case ETHER:
                preferred = preferredStreamQualityWifi;
                break;
            case MOBILE:
            default:
                preferred = preferredStreamQualityMobile;
        }
        if(preferred == null) preferred = StreamQuality.MEDIUM;
        return preferred;
    }

    public int getTargetKbps(){
        if (preferredStreamQualityWifi == StreamQuality.HIGH) return 320;
        if (preferredStreamQualityWifi == StreamQuality.MEDIUM) return 160;
        return 128;
    }

    public void load(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);

        for (Field f : getClass().getDeclaredFields()) {
            String name = f.getName();
            if(prefs.contains(name)) {
                if(f.getType().isEnum()){
                    try {
                        getClass().getField(name).set(this, f.getType().getEnumConstants()[prefs.getInt(name, 0)]);
                    } catch (Exception e){
                        Timber.e(e, "Preferences: fail loading enum %s", name);
                    }
                    continue;
                }
                try {
                    switch (f.getType().toString()) {
                        case "int": getClass().getField(name).set(this, prefs.getInt(name, 0));
                            break;
                        case "class java.lang.String": getClass().getField(name).set(this, prefs.getString(name, null));
                            break;
                        case "boolean": getClass().getField(name).set(this, prefs.getBoolean(name, false));
                            break;
                        case "long": getClass().getField(name).set(this, prefs.getLong(name, 0));
                            break;
                        default: Timber.d("Load: Unrecognized type %s", f.getType().toString());
                            break;
                    }
                } catch(Exception e) {
                    Timber.e(e, "Preferences: failed to load %s \"%s\"", f.getType().toString(), name);
                }
            }

        }
    }

    public void save(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor edit = prefs.edit();
        //Write all variables to database
        for (Field f : getClass().getDeclaredFields()) {
            String name = f.getName();
            if(f.getType().isEnum()) {
                try {
                    Enum e = (Enum) f.get(this);
                    edit.putInt(name, e.ordinal());
                } catch (Exception e) {
                    Timber.e(e, "Preferences: fail saving enum %s", name);
                }
                continue;
            }
            try {
                switch (f.getType().toString()) {
                    case "int": edit.putInt(name, f.getInt(this));
                        break;
                    case "class java.lang.String": edit.putString(name, (String) f.get(this));
                        break;
                    case "boolean": edit.putBoolean(name, f.getBoolean(this));
                        break;
                    case "long": edit.putLong(name, f.getLong(this));
                        break;
                    default: Timber.d("Save: Unrecognized type %s", f.getType().toString());
                        break;
                }
            } catch (Exception e){
                Timber.e(e, "Preferences: failed to save %s \"%s\"", f.getType().toString(), name);
            }
        }
        edit.apply();
    }
}