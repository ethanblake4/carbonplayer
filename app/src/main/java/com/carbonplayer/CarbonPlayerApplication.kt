package com.carbonplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.http.AndroidHttpClient
import android.os.Build
import com.carbonplayer.model.entity.Album
import com.carbonplayer.utils.CrashReportingTree
import com.carbonplayer.utils.Preferences
import com.carbonplayer.utils.jobs.CacheEvictionJob
import com.carbonplayer.utils.jobs.CarbonJobCreator
import com.evernote.android.job.JobManager
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import com.squareup.leakcanary.LeakCanary
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
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

        if (LeakCanary.isInAnalyzerProcess(this))
            return

        LeakCanary.install(this)

        preferences = Preferences()
        preferences.load()

        okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor()).build()
        androidHttpClient = AndroidHttpClient.newInstance(googleUserAgent, applicationContext)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            Stetho.initialize(
                    Stetho.newInitializerBuilder(this)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                            .build())

            //ButterKnife.setDebug(true)
            //Icepick.setDebug(true)
        } else {
            Timber.plant(CrashReportingTree())
        }

        JobManager.create(this).addJobCreator(CarbonJobCreator())
        CacheEvictionJob.schedule()

        RxJavaHooks.setOnError { e -> Timber.e(e, e.toString()) }

        Realm.init(this)
        // Configure default configuration for Realm
        val realmConfig = RealmConfiguration.Builder().build()
        Realm.setDefaultConfiguration(realmConfig)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val description = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW

            val mChannel = NotificationChannel("default", name, importance)
            mChannel.description = description
            mChannel.vibrationPattern = longArrayOf(0)
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(mChannel)
        }
    }

    fun buildDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter?): DataSource.Factory {
        return DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory())

    }

    fun buildHttpDataSourceFactory(): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(Util.getUserAgent(this, googleUserAgent))
    }

    val googleBuildNumberLong = 49211L
    val googleBuildNumber = "49211"
    val googleUserAgent = "Android-Music/" + googleBuildNumber + " (" + Build.PRODUCT + " " + Build.ID + "); gzip"
    val useWebAuthDialog = false
    val useOkHttpForLogin = true
    val useSearchClustering = false

    val darkCSL = ColorStateList.valueOf(Color.DKGRAY)

    var homePdContextToken: String? = null

    lateinit var okHttpClient: OkHttpClient
    lateinit var androidHttpClient: AndroidHttpClient

    fun getOkHttpClient(builder: OkHttpClient.Builder): OkHttpClient {
        return builder
                //.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                //.addNetworkInterceptor(StethoInterceptor())
                .build()
    }

    companion object {
        lateinit var instance: CarbonPlayerApplication
    }
}
