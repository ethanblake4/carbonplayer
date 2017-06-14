package com.carbonplayer;
import android.app.Application;
import com.carbonplayer.model.entity.Album;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.plugins.RxJavaHooks;
import timber.log.Timber;

/**
 * Application base class, contains version and instance-specific variables
 * and initializes libraries
 */
public final class CarbonPlayerApplication extends Application{

    private static CarbonPlayerApplication mInstance;

    //Static variables (multiple-use version dependent)
    //public static String googleUserAgent = "CarbonGSF/0.2";
    public static String googleUserAgent = "Android-Music/41201 (shieldtablet MRA58K); gzip";
    public static boolean useWebAuthDialog = false;

    //Instance variables
    public Album currentAlbum;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        initializeTimber();
        RxJavaHooks.setOnError(e -> Timber.e(e.toString()));

        Realm.init(this);
        // Configure default configuration for Realm
        RealmConfiguration realmConfig = new RealmConfiguration.Builder().build();
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

    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(this, googleUserAgent));
    }
}
