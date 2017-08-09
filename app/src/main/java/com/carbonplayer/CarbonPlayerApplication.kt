package com.carbonplayer

import android.app.Application
import android.os.Build

import com.carbonplayer.model.entity.Album
import com.carbonplayer.utils.CrashReportingTree
import com.carbonplayer.utils.Preferences
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import com.uphyca.stetho_realm.RealmInspectorModulesProvider

import butterknife.ButterKnife
import icepick.Icepick
import io.realm.Realm
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import rx.plugins.RxJavaHooks
import timber.log.Timber

/**
 * Application base class, contains version and instance-specific variables
 * and initializes libraries
 */
class CarbonPlayerApplication : Application() {

    lateinit var preferences: Preferences

    //Instance variables
    var currentAlbum: Album? = null

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        preferences = Preferences()
        preferences.load()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            Stetho.initialize(
                    Stetho.newInitializerBuilder(this)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                            .build())

            ButterKnife.setDebug(true)
            Icepick.setDebug(true)
        } else {
            Timber.plant(CrashReportingTree())
        }

        RxJavaHooks.setOnError { e -> Timber.e(e.toString()) }

        Realm.init(this)
        // Configure default configuration for Realm
        val realmConfig = RealmConfiguration.Builder().build()
        Realm.setDefaultConfiguration(realmConfig)
    }

    fun buildDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter): DataSource.Factory {
        return DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter))


    }

    fun buildHttpDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(Util.getUserAgent(this, googleUserAgent))
    }

    var googleBuildNumber = "49211"
    var googleUserAgent = "Android-Music/" + googleBuildNumber + " (" + Build.PRODUCT + " " + Build.ID + "); gzip"
    var useWebAuthDialog = false
    var useOkHttpForLogin = true

    val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor()).build()

    fun getOkHttpClient(builder: OkHttpClient.Builder): OkHttpClient {
        return builder.addNetworkInterceptor(StethoInterceptor()).build()
    }

    companion object {
        lateinit var instance: CarbonPlayerApplication
    }
}
