package com.carbonplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.enums.CarbonThemeBase;
import com.carbonplayer.model.entity.enums.PaletteMode;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.utils.general.IdentityUtils;

import java.lang.reflect.Field;

import timber.log.Timber;

/**
 * Preferences, enables saving and loading data through reflection
 */

public class Preferences {

    /*Shared Variables**/

    public boolean firstStart = true;

    //Device
    public String userEmail;

    public StreamQuality preferredStreamQualityWifi = StreamQuality.HIGH;
    public StreamQuality preferredStreamQualityMobile = StreamQuality.MEDIUM;

    @NonNull public PaletteMode primaryPaletteMode = PaletteMode.POPULOUS;
    @NonNull public PaletteMode secondaryPaletteMode = PaletteMode.VIBRANT;

    public String masterToken;
    public String BearerAuth;
    public String OAuthToken;
    public String PlayMusicOAuth;
    public String testPlayOAuth;

    public int textAdditionalContrast = 8;

    public int maxAudioCacheSizeMB = 1024;

    public CarbonThemeBase themeBase = CarbonThemeBase.MATERIAL_MIXED;

    public boolean filterExplicit = false;

    public boolean isCarbonTester = true;
    public boolean useTestToken = false;

    public int getContentFilterAsInt() {
        if(filterExplicit) return 2;
        return 1;
    }

    public float getScrimifyAmount() {
        return ((float)textAdditionalContrast) / 100f;
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
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(CarbonPlayerApplication.Companion.getInstance());

        for (Field f : getClass().getDeclaredFields()) {
            String name = f.getName();
            if(prefs.contains(name)) {
                if(f.getType().isEnum()){
                    try {
                        getClass().getField(name).set(this,
                                f.getType().getEnumConstants()[prefs.getInt(name, 0)]);
                    } catch (Exception e){
                        Timber.e(e, "Preferences: fail loading enum %s", name);
                    }
                    continue;
                }
                try {
                    Field field = getClass().getField(name);
                    switch (f.getType().toString()) {
                        case "int": field.set(this, prefs.getInt(name, 0));
                            break;
                        case "class java.lang.String": field.set(this, prefs.getString(name, null));
                            break;
                        case "boolean": field.set(this, prefs.getBoolean(name, false));
                            break;
                        case "long": field.set(this, prefs.getLong(name, 0));
                            break;
                        default: Timber.d("Load: Unrecognized type %s",
                                f.getType().toString());
                            break;
                    }
                } catch(Exception e) {
                    Timber.e(e, "Preferences: failed to load %s \"%s\"",
                            f.getType().toString(), name);
                }
            }
        }
    }

    public void saveSync() {
        saveImpl().commit();
    }

    private SharedPreferences.Editor saveImpl() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(CarbonPlayerApplication.Companion.getInstance());
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
        return edit;
    }

    public void save(){
        saveImpl().apply();
    }
}