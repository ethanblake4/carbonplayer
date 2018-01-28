package com.carbonplayer.model.network

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.net.http.AndroidHttpClient
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.Artist
import com.carbonplayer.model.entity.ConfigEntry
import com.carbonplayer.model.entity.SearchResponse
import com.carbonplayer.model.entity.TopChartsResponse
import com.carbonplayer.model.entity.enums.NetworkType
import com.carbonplayer.model.entity.enums.RadioFeedReason
import com.carbonplayer.model.entity.enums.StreamQuality
import com.carbonplayer.model.entity.exception.ResponseCodeException
import com.carbonplayer.model.entity.exception.ServerRejectionException
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto.GetHomeRequest
import com.carbonplayer.model.entity.radio.RadioSeed
import com.carbonplayer.model.entity.radio.request.RadioFeedRequest
import com.carbonplayer.model.entity.radio.response.RadioFeedResponse
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamPlentry
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.model.network.entity.*
import com.carbonplayer.model.network.utils.ClientContextFactory
import com.carbonplayer.model.network.utils.IOUtils
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.protocol.URLSigning
import com.carbonplayer.utils.toByteArray
import com.squareup.moshi.JsonAdapter
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.exceptions.Exceptions
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.http.client.methods.HttpPost
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*


/**
 * Contains methods for interacting with Google Play Music APIs.
 */
object Protocol {

    private const val SJ_URL = "https://mclients.googleapis.com/sj/v2.5/"
    private const val PA_URL = "https://music-pa.googleapis.com/v1/ij/"
    private const val STREAM_URL = "https://android.clients.google.com/music/mplay"
    private val TYPE_JSON = MediaType.parse("application/json; charset=utf-8")!!
    private const val MAX_RESULTS = 250

    private fun Uri.Builder.appendDefaults() =
            this.apply {
                appendQueryParameter("hl", IdentityUtils.localeCode())
                appendQueryParameter("tier", "aa")
                appendQueryParameter("dv", CarbonPlayerApplication.instance.googleBuildNumber)
                appendQueryParameter("client-build-type", "prod")
            }

    fun getConfig(context: Activity): Single<LinkedList<ConfigEntry>> {
        val client = CarbonPlayerApplication.instance.okHttpClient
        val getParams = Uri.Builder().appendDefaults()

        return Single.create<LinkedList<ConfigEntry>> { subscriber ->
            val request = defaultBuilder(context.baseContext)
                    .url(SJ_URL + "config?" + getParams.build().encodedQuery)
                    .build()
            try {
                val r = client.newCall(request).execute()
                if (!r.isSuccessful) subscriber.onError(ResponseCodeException())

                val sR = r.body()!!.string()
                Timber.d(sR)
                val j = JSONObject(sR)

                val itemArray = j.getJSONObject("data").getJSONArray("entries")
                val itemList = (0 until itemArray.length())
                        .mapTo(LinkedList()) { ConfigEntry(itemArray.getJSONObject(it)) }

                subscriber.onSuccess(itemList)
            } catch (e: IOException) {
                subscriber.onError(e)
            } catch (e: JSONException) {
                subscriber.onError(e)
            }
        }
    }

    fun radioFeed(context: Context, remoteSeedId: String, maxEntries: Int, reason: RadioFeedReason,
                  seedType: Int, sessionToken: String?): Single<RadioFeedResponse> {
        val adapter = CarbonPlayerApplication.moshi.adapter(RadioFeedResponse::class.java)
        return Single.fromCallable {

            val client = CarbonPlayerApplication.instance.okHttpClient

            val radioRQ = RadioFeedRequest(
                    CarbonPlayerApplication.instance.preferences.contentFilterAsInt,
                    RadioFeedRequest.MixRequest(4, 25),
                    listOf(RadioFeedRequest.RadioStationRequest(
                            false,
                            maxEntries,
                            remoteSeedId,
                            null,
                            null,
                            RadioSeed.create(remoteSeedId, seedType),
                            sessionToken
                    ))
            )

            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()
                    .appendQueryParameter("rz", reason.toApiValue())
                    .build()

            val request = defaultBuilder(context)
                    .url(SJ_URL + "radio/stationfeed?" + getParams)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(TYPE_JSON, radioRQ.toJson().toByteArray()))
                    .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful && response.code() >= 200 && response.code() < 300) {
                response.body()?.source()?.let {
                    return@fromCallable adapter.fromJson(it)
                }
            }
            if (response.code() in 400..499) {
                throw handle400(context, response.code(),
                        response.header("X-Rejection-Reason"))
            }
            throw ResponseCodeException(response.body()!!.string())

        }
    }

    @Suppress("DEPRECATION")
    fun listenNow(context: Activity, previousDistilledContextToken: String?):
            Single<InnerJamApiV1Proto.GetHomeResponse> {

        val builder = GetHomeRequest.newBuilder()
                .setClientContext(ClientContextFactory.create(context))

        previousDistilledContextToken?.let { builder.previousDistilledContextToken = it }

        val homeRequest = builder.build()

        return Single.create<InnerJamApiV1Proto.GetHomeResponse> { subscriber ->

            val deviceId = IdentityUtils.getGservicesId(context, true)

            try {
                val entity = AndroidHttpClient.getCompressedEntity(
                        homeRequest.toByteArray(), context.contentResolver)
                entity.setContentType("application/x-protobuf")
                val httpRequest = HttpPost(PA_URL + "gethome?alt=proto")
                httpRequest.entity = entity
                httpRequest.setHeader("X-Device-ID", deviceId)
                httpRequest.setHeader("X-Device-Logging-ID", IdentityUtils.getLoggingID(context))
                httpRequest.setHeader("Authorization", "Bearer " +
                        CarbonPlayerApplication.instance.preferences.PlayMusicOAuth)
                val response = CarbonPlayerApplication.instance.androidHttpClient
                        .execute(httpRequest)
                if (response.statusLine.statusCode == 401) {
                    GoogleLogin.retryPlayOAuthSync(context)
                    subscriber.onError(ServerRejectionException(
                            ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED
                    ))
                }
                val ent = response.entity
                val homeResponse = InnerJamApiV1Proto.GetHomeResponse
                        .parseFrom(IOUtils.readSmallStream(ent.content, 5242880))

                ent.consumeContent()
                httpRequest.abort()
                subscriber.onSuccess(homeResponse)
            } catch (e: IOException) {
                Timber.e(e, "IOException in listenNow")
                subscriber.onError(e)
            }
        }
    }

    fun getTopCharts(context: Context, offset: Int, pageSize: Int): Observable<TopChartsResponse> {

        val adapter = CarbonPlayerApplication.moshi.adapter(TopChartsResponse::class.java)

        return Observable.fromCallable {
            val client = CarbonPlayerApplication.instance.okHttpClient

            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()
                    .appendQueryParameter("tracksOffset", offset.toString())
                    .appendQueryParameter("albumsOffset", offset.toString())
                    .appendQueryParameter("maxTracks", pageSize.toString())
                    .appendQueryParameter("maxAlbums", pageSize.toString())

            val request = defaultBuilder(context)
                    .url(SJ_URL + "browse/topchart?" + getParams)
                    .header("Content-Type", "application/json")
                    .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.code() >= 200 && response.code() < 300) {
                response.body()?.source()?.let {
                    return@fromCallable adapter.fromJson(it)
                }
            }
            if (response.code() in 400..499) {
                throw handle400(context, response.code(), response.header("X-Rejection-Reason"))
            }
            throw ResponseCodeException(response.body()!!.string())
        }
    }

    fun getNautilusAlbum(context: Context, nid: String): Observable<SkyjamAlbum> {
        return Observable.fromCallable {
            val client = CarbonPlayerApplication.instance.okHttpClient
            val adapter = CarbonPlayerApplication.moshi.adapter(SkyjamAlbum::class.java)

            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()
                    .appendQueryParameter("nid", nid)
                    .appendQueryParameter("include-tracks", "true")
                    .appendQueryParameter("include-description", "true")

            val request = defaultBuilder(context)
                    .url(SJ_URL + "fetchalbum?" + getParams)
                    .header("Content-Type", "application/json")
                    .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.code() >= 200 && response.code() < 300) {
                response.body()?.let { body ->
                    body.source()?.let {
                        val s = adapter.fromJson(it)
                        body.close()
                        return@fromCallable s
                    }
                }
            }
            if (response.code() in 400..499) {
                throw handle400(context, response.code(),
                        response.header("X-Rejection-Reason"))
            }
            response.body()?.string()?.let {
                throw ResponseCodeException(it)
            }; throw ResponseCodeException()
        }
    }

    fun getNautilusArtist(context: Context,  nid: String): Observable<Artist> {

        return Observable.fromCallable {
            val client = CarbonPlayerApplication.instance.okHttpClient
            val adapter = CarbonPlayerApplication.moshi.adapter(Artist::class.java)

            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()
                    .appendQueryParameter("nid", nid)
                    .appendQueryParameter("include-albums", "true")
                    .appendQueryParameter("num-top-tracks", "50")
                    .appendQueryParameter("num-related-artists", "20")

            val request = defaultBuilder(context)
                    .url(SJ_URL + "fetchartist?" + getParams)
                    .header("Content-Type", "application/json")
                    .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.code() >= 200 && response.code() < 300) {
                response.body()?.let { body ->
                    body.source()?.let {
                        val s = adapter.fromJson(it)
                        body.close()
                        return@fromCallable s
                    }
                }
            }
            if (response.code() in 400..499) {
                throw handle400(context, response.code(),
                        response.header("X-Rejection-Reason"))
            }
            throw ResponseCodeException(response.body()!!.string())
        }
    }

    fun getSearch(context: Context, query: String, continuation: String) = Observable.fromCallable {
        val client = CarbonPlayerApplication.instance.okHttpClient
        val adapter = CarbonPlayerApplication.moshi.adapter(SearchResponse::class.java)

        val getParams = Uri.Builder()
                .appendDefaults()
                .appendQueryParameter("q", query)
                .appendQueryParameter("query-type", "1")
                .apply {
                    if (CarbonPlayerApplication.instance.useSearchClustering)
                        appendQueryParameter("ic", "true")
                    if (continuation.isNotEmpty())
                        appendQueryParameter("start-token", continuation)
                }
                .appendQueryParameter("max-results", "100")

                /* 1: Song, 2: Artist, 3: Album, 4: Playlist, 6: Station, 7: Situation,
                 * TODO 8: Video, 9: Podcast */
                .appendQueryParameter("ct", "1,2,3,4,6,7")

                .build()

        val request = defaultBuilder(context)
                .url(SJ_URL + "query?" + getParams)
                .header("Content-Type", "application/json")
                .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful && response.code() in 200..299) {
            response.body()?.source()?.let {
                return@fromCallable adapter.fromJson(it)
            }
        }
        if (response.code() in 400..499) {
            throw handle400(context, response.code(), response.header("X-Rejection-Reason"))
        }
        throw ResponseCodeException(response.body()!!.string())
    }


    private fun pagedJSONFeed(context: Context, urlPart: String): Observable<LinkedList<JSONObject>> {

        val client = CarbonPlayerApplication
                .instance.okHttpClient
        val getParams = Uri.Builder()
                .appendQueryParameter("alt", "json")
                .appendDefaults()

        return Observable.create<LinkedList<JSONObject>> { subscriber ->
            var startToken: String? = ""
            while (startToken != null) {
                Timber.d("startToken: %s", startToken)
                val requestJson = JSONObject()
                try {
                    requestJson.put("max-results", MAX_RESULTS)
                    if ("" != startToken) requestJson.put("start-token", startToken)
                } catch (e: JSONException) {
                    subscriber.onError(e)
                }

                startToken = null

                val request = defaultBuilder(context)
                        .url(SJ_URL + urlPart + "?" + getParams.build().encodedQuery)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(TYPE_JSON, requestJson.toString()))
                        .build()
                try {
                    val r = client.newCall(request).execute()
                    if (!r.isSuccessful) subscriber.onError(ResponseCodeException())
                    val response = r.body()!!.string()

                    val j = JSONObject(response)
                    //Timber.d(response);

                    if (j.has("nextPageToken")) startToken = j.getString("nextPageToken")

                    val itemArray = j.getJSONObject("data").getJSONArray("items")
                    val list = (0 until itemArray.length())
                            .mapTo(LinkedList<JSONObject>()) {
                                itemArray.getJSONObject(it)
                            }

                    subscriber.onNext(list)

                } catch (e: IOException) {
                    subscriber.onError(e)
                } catch (e: JSONException) {
                    subscriber.onError(e)
                }

            }
            subscriber.onComplete()
        }
    }

    private fun <R, T : PagedJsonResponse<R>> rawPagedFeed(context: Context, urlPart: String,
                                                     adapter: JsonAdapter<T>): Observable<R> {

        val client = CarbonPlayerApplication
                .instance.okHttpClient
        val getParams = Uri.Builder()
                .appendQueryParameter("alt", "json")
                .appendDefaults()

        return Observable.create<R> { subscriber ->

            var startToken: String? = ""

            while (startToken != null) {
                Timber.d("startToken: $startToken")
                val requestJson = JSONObject()
                try {
                    requestJson.put("max-results", MAX_RESULTS)
                    if ("" != startToken) requestJson.put("start-token", startToken)
                } catch (e: JSONException) {
                    subscriber.onError(e)
                }

                startToken = null

                val request = defaultBuilder(context)
                        .url(SJ_URL + urlPart + "?" + getParams.build().encodedQuery)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(TYPE_JSON, requestJson.toString()))
                        .build()
                try {

                    val r = client.newCall(request).execute()
                    if (!r.isSuccessful) subscriber.onError(ResponseCodeException())

                    r.body()?.source()?.let { adapter.fromJson(it)?.let { response ->
                        if (response.nextPageToken != null) startToken = response.nextPageToken
                        subscriber.onNext(response.data)
                    } }

                } catch (e: IOException) {
                    subscriber.onError(e)
                } catch (e: JSONException) {
                    subscriber.onError(e)
                }
            }
            subscriber.onComplete()
        }
    }

    @JvmStatic
    fun listTracks(context: Context): Observable<List<SkyjamTrack>> {
        val adapter = CarbonPlayerApplication.moshi.adapter(PagedTrackResponse::class.java)
        return rawPagedFeed<PagedTrackResponseData, PagedTrackResponse>(context, "trackfeed", adapter)
                .map { response -> response.items }
    }

    @JvmStatic
    fun listPlaylists(context: Activity): Observable<List<SkyjamPlaylist>> {
        val adapter = CarbonPlayerApplication.moshi.adapter(PagedPlaylistResponse::class.java)
        return rawPagedFeed(context, "playlistfeed", adapter)
                .map<List<SkyjamPlaylist>> { response -> response.items }
    }

    @JvmStatic
    fun listPlaylistEntries(context: Activity): Observable<List<SkyjamPlentry>> {
        val adapter = CarbonPlayerApplication.moshi.adapter(PagedPlentryResponse::class.java)
        return rawPagedFeed(context, "plentryfeed", adapter)
                .map { response -> response.items }
    }

    fun getStreamURL(context: Context, song_id: String): Single<String> {
        val protocols = ArrayList<okhttp3.Protocol>()
        protocols.add(okhttp3.Protocol.HTTP_1_1)
        val client = CarbonPlayerApplication.instance.getOkHttpClient(
                OkHttpClient().newBuilder()
                        .followRedirects(false)
                        .followSslRedirects(false)
                        .protocols(protocols))
        return Single.create<String> { subscriber ->
            val salt = Date().time.toString()
            var digest = ""
            try {
                digest = URLSigning.sign(song_id, salt)
            } catch (e: NoSuchAlgorithmException) {
                subscriber.onError(Exceptions.propagate(e))
            } catch (e: UnsupportedEncodingException) {
                subscriber.onError(Exceptions.propagate(e))
            } catch (e: InvalidKeyException) {
                subscriber.onError(Exceptions.propagate(e))
            }

            val getParams = Uri.Builder()

            if (song_id.startsWith("T") || song_id.startsWith("D"))
                getParams.appendQueryParameter("mjck", song_id)
            else
                getParams.appendQueryParameter("songid", song_id)

            getParams
                    .appendDefaults()
                    .appendQueryParameter("targetkbps", "180")
                    .appendQueryParameter("audio_formats", "mp3")
                    .appendQueryParameter("p", if (IdentityUtils.getDeviceIsSmartphone(context)) "1" else "0")
                    .appendQueryParameter("opt", getStreamQualityHeader(context))
                    .appendQueryParameter("net", getNetHeader(context))
                    .appendQueryParameter("pt", "e")
                    .appendQueryParameter("adaptive", "true")
                    //.appendQueryParameter("dt", "pc")
                    .appendQueryParameter("slt", salt)
                    .appendQueryParameter("sig", digest)

            val encQuery = getParams.build().encodedQuery
            Timber.d(encQuery)

            val request = bearerBuilder(context)
                    .url(STREAM_URL + "?" + encQuery)
                    .build()
            try {
                val r = client.newCall(request).execute()
                val locHeader = r.headers().get("Location")
                if (r.isRedirect && locHeader != null) {
                    subscriber.onSuccess(locHeader)
                } else {
                    if (r.code() == 401 || r.code() == 402 || r.code() == 403) {
                        val rejectionReason = r.header("X-Rejected-Reason")
                        subscriber.onError(handle400(context, r.code(), rejectionReason))
                    } else if (r.code() in 200..299) {
                        subscriber.onError(ResponseCodeException(String.format(Locale.getDefault(),
                                "Unexpected response code %d", r.code())))
                    } else {
                        subscriber.onError(Exception(r.body()!!.string()))
                    }
                }
            } catch (e: IOException) {
                subscriber.onError(e)
            }
        }
    }

    private fun handle400(context: Context, code: Int, rejectionReason: String?): Exception {
        if (rejectionReason != null) {
            try {
                val rejectionReasonEnum = ServerRejectionException.RejectionReason
                        .valueOf(rejectionReason.toUpperCase())
                Timber.e(ServerRejectionException(rejectionReasonEnum),
                        "getStreamURL: serverRejected")
                when (rejectionReasonEnum) {
                    ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED -> {
                        GoogleLogin.retryGoogleAuth(context)
                        return ServerRejectionException(rejectionReasonEnum)
                    }
                    ServerRejectionException.RejectionReason.ANOTHER_STREAM_BEING_PLAYED,
                    ServerRejectionException.RejectionReason.STREAM_RATE_LIMIT_REACHED,
                    ServerRejectionException.RejectionReason.TRACK_NOT_IN_SUBSCRIPTION,
                    ServerRejectionException.RejectionReason.WOODSTOCK_SESSION_TOKEN_INVALID,
                    ServerRejectionException.RejectionReason.WOODSTOCK_ENTRY_ID_INVALID,
                    ServerRejectionException.RejectionReason.WOODSTOCK_ENTRY_ID_EXPIRED,
                    ServerRejectionException.RejectionReason.WOODSTOCK_ENTRY_ID_TOO_EARLY,
                    ServerRejectionException.RejectionReason.DEVICE_VERSION_BLACKLISTED -> {
                        return ServerRejectionException(rejectionReasonEnum)
                    }
                }
            } catch (e: IllegalArgumentException) {
                try {
                    GoogleLogin.retryGoogleAuthSync(context)
                } catch (s: Exception) {
                    Timber.e(e, "Exception retrying Google Auth")
                }

                return ServerRejectionException(ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
            }

        } else {
            try {
                GoogleLogin.retryGoogleAuthSync(context)
            } catch (e: Exception) {
                Timber.e(e, "Exception retrying Google Auth")
            }

            return ServerRejectionException(
                    ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
        }
    }

    private fun getNetHeader(context: Context): String {
        return when (IdentityUtils.networkType(context)) {
            NetworkType.WIFI -> "wifi"
            NetworkType.ETHER -> "ether"
            NetworkType.MOBILE -> "mob"
            else -> ""
        }
    }

    private fun getStreamQualityHeader(context: Context): String {
        var streamQuality: StreamQuality?
        streamQuality = when (IdentityUtils.networkType(context)) {
            NetworkType.WIFI, NetworkType.ETHER -> {
                CarbonPlayerApplication.instance.preferences
                        .preferredStreamQualityWifi
            }
            else -> CarbonPlayerApplication.instance.preferences
                    .preferredStreamQualityMobile
        }
        if (streamQuality == null)
            streamQuality = StreamQuality.MEDIUM
        return when (streamQuality) {
            StreamQuality.HIGH -> "hi"
            StreamQuality.MEDIUM -> "med"
            StreamQuality.LOW -> "low"
            else -> ""
        }
    }

    private fun defaultBuilder(context: Context): Request.Builder {
        return Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.instance.googleUserAgent)
                .header("Authorization", "GoogleLogin auth=" +
                        CarbonPlayerApplication.instance.preferences.OAuthToken)
                .header("X-Device-ID", IdentityUtils.deviceId(context))
                .header("X-Device-Logging-ID", IdentityUtils.getLoggingID(context))
    }

    private fun bearerBuilder(context: Context): Request.Builder {
        Timber.d("Bearer token: %s", CarbonPlayerApplication.instance.preferences.BearerAuth)

        return Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.instance.googleUserAgent)
                .header("Authorization", "Bearer " +
                        CarbonPlayerApplication.instance.preferences.BearerAuth)
                .header("X-Device-ID", IdentityUtils.getGservicesId(context, true))
                .header("X-Device-Logging-ID", IdentityUtils.getLoggingID(context))
    }
}