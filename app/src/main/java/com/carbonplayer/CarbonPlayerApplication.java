package com.carbonplayer;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.functions.Action1;
import rx.plugins.RxJavaHooks;
import timber.log.Timber;

/**
 * Application base class, contains version and instance-specific variables
 * and initializes libraries
 */
public final class CarbonPlayerApplication extends Application{

    private static CarbonPlayerApplication mInstance;

    //Static variables (multiple-use version dependent)
    public static String googleUserAgent = "CarbonGSF/0.2";

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        initializeTimber();
        RxJavaHooks.setOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                Timber.e(e.toString());
            }
        });

        // Configure default configuration for Realm
        RealmConfiguration realmConfig = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(realmConfig);
    }

    /**
     * Initialize Timber logging
     */
    private void initializeTimber() {
        Timber.plant(new Timber.DebugTree());
    }

    public static CarbonPlayerApplication getInstance() {
        return mInstance;
    }
}
