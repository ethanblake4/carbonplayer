package com.carbonplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.api.ExploreTab
import com.carbonplayer.model.entity.api.StationCategory
import com.carbonplayer.model.entity.api.TopChartsGenres
import com.carbonplayer.model.entity.api.TopChartsResponse
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto
import com.carbonplayer.model.network.utils.RealmListJsonAdapterFactory
import com.carbonplayer.ui.main.TopChartsController
import com.carbonplayer.utils.CrashReportingTree
import com.carbonplayer.utils.Preferences
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.jobs.CacheEvictionJob
import com.carbonplayer.utils.jobs.CarbonJobCreator
import com.evernote.android.job.JobManager
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.analytics.FirebaseAnalytics
import com.jakewharton.processphoenix.ProcessPhoenix
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.realm.Realm
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import timber.log.Timber


/**
 * Application base class, contains version and instance-specific variables
 * and initializes libraries
 */
class CarbonPlayerApplication : Application() {

    lateinit var preferences: Preferences
    lateinit var analytics: FirebaseAnalytics

    //Instance variables
    var currentAlbum: Album? = null

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        if (ProcessPhoenix.isPhoenixProcess(this)) return

        analytics = FirebaseAnalytics.getInstance(this)

        analytics.setAnalyticsCollectionEnabled(
                !BuildConfig.DEBUG &&
                !IdentityUtils.isAutomatedTestDevice(this))

        /*if (LeakCanary.isInAnalyzerProcess(this))
            return

        LeakCanary.install(this)*/

        preferences = Preferences()
        preferences.load()

        preferences.textAdditionalContrast = 8

        okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor()).build()
        //androidHttpClient = AndroidHttpClient.newInstance(googleUserAgent, applicationContext)

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

        RxJavaPlugins.setErrorHandler { e -> Timber.e(e) }


        Realm.init(this)
        // Configure default configuration for Realm
        val realmConfig = RealmConfiguration.Builder()
                .schemaVersion(2)
                .migration { realm, oldVersion, newVersion ->
                    if(newVersion <= oldVersion) return@migration
                    if(newVersion == 2L) realm.schema.get("Track")
                            ?.addField("newTrackType", Integer::class.java)
                            ?.transform { obj ->
                                obj.getString("trackType")?.toIntOrNull()?.let {
                                    obj.setInt("newTrackType", it)
                                }
                            }
                            ?.removeField("trackType")
                            ?.renameField("newTrackType", "trackType")
                }.build()

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

    val googleBuildNumberLong = 68381L
    val googleBuildNumber = "68381"
    val googleUserAgent = "Android-Music/" + googleBuildNumber + " (" + Build.PRODUCT + " " + Build.ID + "); gzip"
    val useWebAuthDialog = false
    val useOkHttpForLogin = true
    val useSearchClustering = true
    val useSampleData = false

    val darkCSL = ColorStateList.valueOf(Color.DKGRAY)

    var homeLastResponse: InnerJamApiV1Proto.GetHomeResponse? = null
    var homePdContextToken: String? = null

    var lastNewReleasesResponse: ExploreTab? = null
    var lastStationCategoryRoot: StationCategory? = null

    var topchartsResponseMap: MutableMap<String, TopChartsResponse> = mutableMapOf()
    var lastTopChartsGenres: TopChartsGenres? = null
    var topChartsCurrentChart = TopChartsController.DEFAULT_CHART

    lateinit var okHttpClient: OkHttpClient
    //lateinit var androidHttpClient: AndroidHttpClient

    fun getOkHttpClient(builder: OkHttpClient.Builder): OkHttpClient {
        return builder
                //.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                //.addNetworkInterceptor(StethoInterceptor())
                .build()
    }

    companion object {
        lateinit var instance: CarbonPlayerApplication
        private var _compositeDisposable: CompositeDisposable? = null

        val defaultMtoken = "aas_et/AKppINZpEZk3nrMVIMuXZvCkgO_OlueAqJn-serXl6dxdz0fw6L7AQ5F_MC" +
                "mW-XJmNgv_HnGWOo4wNcpdaNZO9AXqhBegmiduflFvMHcXVZ8ZHiHKEuQYPE9ZIU2TZ4qMg=="

        val compositeDisposable: CompositeDisposable

        get() {
            if (_compositeDisposable == null) _compositeDisposable = CompositeDisposable()
            _compositeDisposable?.isDisposed?.let { if (it) _compositeDisposable = CompositeDisposable() }
            return _compositeDisposable!!
        }

        val moshi = Moshi.Builder()
                .add(RealmListJsonAdapterFactory())
                .add(KotlinJsonAdapterFactory())
                .build()
    }
}
