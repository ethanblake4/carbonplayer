package com.carbonplayer;
import android.app.Application;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.utils.CrashReportingTree;
import com.carbonplayer.utils.Preferences;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import butterknife.ButterKnife;
import icepick.Icepick;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.OkHttpClient;
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
    public static String googleBuildNumber = "49211";
    public static String googleUserAgent = "Android-Music/" + googleBuildNumber + " (shieldtablet MRA58K); gzip";
    public static boolean useWebAuthDialog = false;
    public static boolean useOkHttpForLogin = true;

    public Preferences preferences;

    //Instance variables
    public Album currentAlbum;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        preferences = new Preferences(getApplicationContext());
        preferences.load();

        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree());

            Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                    .build());
            ButterKnife.setDebug(true);
            Icepick.setDebug(true);
        } else {
            Timber.plant(new CrashReportingTree());
        }

        RxJavaHooks.setOnError(e -> Timber.e(e.toString()));

        Realm.init(this);
        // Configure default configuration for Realm
        RealmConfiguration realmConfig = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(realmConfig);
    }

    public static CarbonPlayerApplication getInstance() {
        return mInstance;
    }

    public static OkHttpClient getOkHttpClient(){
        return new OkHttpClient.Builder().addNetworkInterceptor(new StethoInterceptor()).build();
    }

    public static OkHttpClient getOkHttpClient(OkHttpClient.Builder builder){
        return builder.addNetworkInterceptor(new StethoInterceptor()).build();
    }

    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(this, googleUserAgent));
    }
}
