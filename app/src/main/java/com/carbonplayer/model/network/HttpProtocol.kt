package com.carbonplayer.model.network

import android.content.Context
import android.net.Uri
import com.carbonplayer.BuildConfig
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.exception.ResponseCodeException
import com.carbonplayer.model.network.entity.PagedJsonResponse
import com.carbonplayer.utils.general.Either
import com.carbonplayer.utils.general.IdentityUtils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException

@Suppress("NOTHING_TO_INLINE")
object HttpProtocol {

    private const val MAX_GETLIBRARY_RESULTS = 250

    class RequestCapabilities (
            val acceptCL: Boolean,
            val acceptBearer: Boolean,
            val acceptFree: Boolean
    ) {
        companion object {
            fun new(acceptFree: Boolean = true) =
                    RequestCapabilities(false, false, acceptFree)
            fun cl(acceptFree: Boolean = true) =
                    RequestCapabilities(true, false, acceptFree)
        }
    }

    const val SJ_URL = "https://mclients.googleapis.com/sj/v2.5/" // JSON URL
    const val PA_URL = "https://music-pa.googleapis.com/v1/ij/" // Protobuf URL
    const val STREAM_URL = "https://android.clients.google.com/music/mplay"
    val TYPE_JSON = MediaType.parse("application/json; charset=utf-8")!!

    inline fun Uri.Builder.appendDefaults() =
            this.apply {
                appendQueryParameter("hl", IdentityUtils.localeCode())
                appendQueryParameter("tier", "aa")
                appendQueryParameter("dv", CarbonPlayerApplication.instance.googleBuildNumber)
                appendQueryParameter("client-build-type", "prod")
            }

    inline fun <reified T, reified R> post (
        context: Context,
        uri: Uri.Builder,
        capabilities: RequestCapabilities,
        request: T
    ) : Single<R> = Single.fromCallable {

        val requestAdapter = CarbonPlayerApplication.moshi.adapter(T::class.java)
        val responseAdapter = CarbonPlayerApplication.moshi.adapter(R::class.java)

        val path = uri
            .appendQueryParameter("alt", "json")
            .appendDefaults()
            .build()

        val httpRequest = _bestBuilder(
            context,
            capabilities.acceptCL,
            capabilities.acceptBearer,
            capabilities.acceptFree
        )
                .url(path.toString())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(TYPE_JSON, requestAdapter.toJson(request).toByteArray()))
                .build()

        val response = CarbonPlayerApplication.instance.okHttpClient.newCall(httpRequest).execute()

        if (response.isSuccessful && response.code() >= 200 && response.code() < 300) {
            response.body()?.source()?.let {
                return@fromCallable responseAdapter.fromJson(it)
            }
        }
        if (response.code() in 400..499) {
            throw Protocol.handle400(context, response.code(),
                   response.header("X-Rejection-Reason"),
                   capabilities.acceptCL, capabilities.acceptBearer, capabilities.acceptFree)
        }
        throw ResponseCodeException(response.body()?.string() ?: response.code().toString())
    }

    inline fun <reified R> get (
            context: Context,
            uri: Uri.Builder,
            capabilities: RequestCapabilities
    ) : Single<R> = Single.fromCallable {

        val responseAdapter = CarbonPlayerApplication.moshi.adapter(R::class.java)

        val path = uri
                .appendDefaults()
                .build().toString()

        Timber.d("Path: $path")

        val httpRequest = _bestBuilder(
                context,
                capabilities.acceptCL,
                capabilities.acceptBearer,
                capabilities.acceptFree
        )
                .url(path)
                .build()

        val response = CarbonPlayerApplication.instance.okHttpClient.newCall(httpRequest).execute()

        if (response.isSuccessful && response.code() >= 200 && response.code() < 300) {
            if(BuildConfig.DEBUG) {
                response.body()?.string()?.let {
                    try {
                        return@fromCallable responseAdapter.fromJson(it)
                    } catch (e: JsonDataException) {
                        Timber.i(it)
                        throw e
                    }
                }
            } else {
                response.body()?.source()?.let {
                    return@fromCallable responseAdapter.fromJson(it)
                }
            }
        }
        if (response.code() in 400..499) {
            throw Protocol.handle400(context, response.code(),
                    response.header("X-Rejection-Reason"),
                    capabilities.acceptCL, capabilities.acceptBearer, capabilities.acceptFree)
        }
        throw ResponseCodeException(response.body()?.string() ?: response.code().toString())

    }

    fun <R, T : PagedJsonResponse<R>> rawPagedFeed(
            context: Context, urlPart: String, adapter: JsonAdapter<T>
    ): Observable<Either<R, Unit>> {

        val client = CarbonPlayerApplication
                .instance.okHttpClient
        val getParams = Uri.Builder()
                .appendQueryParameter("alt", "json")
                .appendDefaults()

        return Observable.create<Either<R, Unit>> { subscriber ->

            var startToken: String? = ""

            while (startToken != null) {
                Timber.d("startToken: $startToken")
                val requestJson = JSONObject()
                try {
                    requestJson.put("max-results", MAX_GETLIBRARY_RESULTS)
                    if ("" != startToken) requestJson.put("start-token", startToken)
                } catch (e: JSONException) {
                    subscriber.onError(e)
                }

                startToken = null

                val request = _bestBuilder(context, true,
                        false, true)
                        .url(SJ_URL + urlPart + "?" + getParams.build().encodedQuery)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(TYPE_JSON, requestJson.toString()))
                        .build()
                try {
                    val r = client.newCall(request).execute()
                    if (!r.isSuccessful) {
                        subscriber.onError(ResponseCodeException())
                    }

                    r.body()?.source()?.let {
                        adapter.fromJson(it)?.let { response ->
                            startToken = response.nextPageToken
                            Timber.d("recieve startToken as $startToken")
                            response.data?.let {
                                subscriber.onNext(Either.Left(it))
                            }
                            if (startToken == "" || startToken == null) {
                                subscriber.onNext(Either.Right(Unit))
                                subscriber.onComplete()
                            }
                            it
                        } ?: {
                            subscriber.onNext(Either.Right(Unit))
                            subscriber.onComplete()
                            it
                        }.invoke()
                    } ?: {
                        subscriber.onNext(Either.Right(Unit))
                        subscriber.onComplete()
                    }.invoke()

                } catch (e: IOException) {
                    subscriber.onError(e)
                } catch (e: JSONException) {
                    subscriber.onError(e)
                }
            }
            subscriber.onComplete()
        }
    }

    inline fun _bestBuilder(
            context: Context,
            acceptCL: Boolean,
            acceptBearer: Boolean,
            sameWithFree: Boolean = true
    ) : Request.Builder {
        if(acceptCL && !CarbonPlayerApplication.instance.preferences.useTestToken)
            return _defaultBuilder(context)
        if(acceptBearer && (sameWithFree ||
                !CarbonPlayerApplication.instance.preferences.useTestToken))
            return _bearerBuilder(context)
        return _playBuilder(context, sameWithFree)
    }

    inline fun _defaultBuilder(context: Context): Request.Builder {
        return Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.instance.googleUserAgent)
                .header("Authorization", "GoogleLogin auth=" +
                        CarbonPlayerApplication.instance.preferences.OAuthToken)
                .header("X-Device-ID", IdentityUtils.deviceId(context))
                .header("X-Device-Logging-ID", IdentityUtils.getLoggingID(context))
    }

    inline fun _bearerBuilder(context: Context): Request.Builder {
        if(IdentityUtils.isAutomatedTestDevice(context)) return _playBuilder(context)
        Timber.d("Bearer token: %s", CarbonPlayerApplication.instance.preferences.BearerAuth)
        Timber.d("DeviceID: ${IdentityUtils.getGservicesId(context, true)}")
        Timber.d("UserAgent: ${CarbonPlayerApplication.instance.googleUserAgent}")
        return Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.instance.googleUserAgent)
                .header("Authorization", "Bearer " +
                        CarbonPlayerApplication.instance.preferences.BearerAuth)

                .header("X-Device-ID", IdentityUtils.getGservicesId(context, true))
                .header("X-Device-Logging-ID", IdentityUtils.getLoggingID(context))
    }

    inline fun _playBuilder(context: Context, sameWithFree: Boolean = true): Request.Builder {

        Timber.d("DeviceID: ${IdentityUtils.getGservicesId(context, true)}")
        Timber.d("UserAgent: ${CarbonPlayerApplication.instance.googleUserAgent}")

        val token = if (sameWithFree || !CarbonPlayerApplication.instance.preferences.useTestToken)
            CarbonPlayerApplication.instance.preferences.PlayMusicOAuth
        else CarbonPlayerApplication.instance.preferences.testPlayOAuth

        Timber.d("sameWithFree: $sameWithFree Play token: $token")

        return Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.instance.googleUserAgent)
                .header("Authorization", "Bearer $token")
                .header("X-Device-ID", IdentityUtils.getGservicesId(context, true))
                .header("X-Device-Logging-ID", IdentityUtils.getLoggingID(context))
    }
}