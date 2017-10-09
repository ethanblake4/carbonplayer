package com.carbonplayer.model.network

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.net.http.AndroidHttpClient
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.ConfigEntry
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.entity.Playlist
import com.carbonplayer.model.entity.PlaylistEntry
import com.carbonplayer.model.entity.enums.NetworkType
import com.carbonplayer.model.entity.enums.StreamQuality
import com.carbonplayer.model.entity.exception.ResponseCodeException
import com.carbonplayer.model.entity.exception.ServerRejectionException
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto.GetHomeRequest
import com.carbonplayer.model.network.utils.ClientContextFactory
import com.carbonplayer.model.network.utils.IOUtils
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.protocol.URLSigning
import io.realm.Realm
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.http.client.methods.HttpPost
import org.json.JSONException
import org.json.JSONObject
import rx.Observable
import rx.Single
import rx.exceptions.Exceptions
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

    private val SJ_URL = "https://mclients.googleapis.com/sj/v2.5/"
    val PA_URL = "https://music-pa.googleapis.com/v1/ij/"
    private val STREAM_URL = "https://android.clients.google.com/music/mplay"
    private val TYPE_JSON = MediaType.parse("application/json; charset=utf-8")
    private val MAX_RESULTS = 250

    fun getConfig(context: Activity): Single<LinkedList<ConfigEntry>> {
        val client = CarbonPlayerApplication.instance.okHttpClient
        val getParams = Uri.Builder()
                .appendQueryParameter("dv", CarbonPlayerApplication.instance.googleBuildNumber)
                .appendQueryParameter("tier", "aa")
                .appendQueryParameter("hl", IdentityUtils.localeCode())

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
                val itemList = (0..itemArray.length() - 1)
                        .mapTo(LinkedList<ConfigEntry>()) {
                            ConfigEntry(itemArray.getJSONObject(it))
                        }

                subscriber.onSuccess(itemList)
            } catch (e: IOException) {
                subscriber.onError(e)
            } catch (e: JSONException) {
                subscriber.onError(e)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun listenNow(context: Activity,
                  previousDistilledContextToken: String?): Single<InnerJamApiV1Proto.GetHomeResponse> {

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

    private fun pagedJSONFeed(context: Context, urlPart: String): Observable<LinkedList<JSONObject>> {

        val client = CarbonPlayerApplication
                .instance.okHttpClient
        val getParams = Uri.Builder()
                .appendQueryParameter("dv", CarbonPlayerApplication
                        .instance.googleBuildNumber)
                .appendQueryParameter("alt", "json")
                .appendQueryParameter("hl", IdentityUtils.localeCode())
                .appendQueryParameter("tier", "aa")

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
                    val list = (0..itemArray.length() - 1)
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
            subscriber.onCompleted()
        }
    }

    @JvmStatic fun listTracks(context: Context): Observable<List<MusicTrack>> {
        return pagedJSONFeed(context, "trackfeed")
                .map<List<MusicTrack>> { jsonObjects ->
                    try {
                        jsonObjects.map { MusicTrack(it) }
                    } catch (e: JSONException) {
                        throw Exceptions.propagate(e)
                    }
                }
    }

    @JvmStatic fun listPlaylists(context: Activity): Observable<List<Playlist>> {
        return pagedJSONFeed(context, "playlistfeed")
                .map<List<Playlist>> { jsonObjects ->
                    try {
                        jsonObjects.map { Playlist(it) }
                    } catch (e: JSONException) {
                        throw Exceptions.propagate(e)
                    }
                }
    }

    @JvmStatic fun listPlaylistEntries(context: Activity): Observable<List<PlaylistEntry>> {
        return pagedJSONFeed(context, "plentryfeed")
                .map<List<PlaylistEntry>> { jsonObjects ->
                    try {
                        val realm = Realm.getDefaultInstance()
                        jsonObjects.map {
                            val t = realm.where(MusicTrack::class.java).equalTo(MusicTrack.ID,
                                    it.getString("id")).findFirst()
                            PlaylistEntry(it, t)
                        }
                    } catch (e: JSONException) {
                        throw Exceptions.propagate(e)
                    }
                }
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
                    .appendQueryParameter("targetkbps", "180")
                    .appendQueryParameter("audio_formats", "mp3")
                    .appendQueryParameter("dv", CarbonPlayerApplication.instance.googleBuildNumber)
                    .appendQueryParameter("p", if (IdentityUtils.getDeviceIsSmartphone(context)) "1" else "0")
                    .appendQueryParameter("opt", getStreamQualityHeader(context))
                    .appendQueryParameter("net", getNetHeader(context))
                    .appendQueryParameter("pt", "e")
                    .appendQueryParameter("adaptive", "true")
                    //.appendQueryParameter("dt", "pc")
                    .appendQueryParameter("slt", salt)
                    .appendQueryParameter("sig", digest)
                    .appendQueryParameter("hl", IdentityUtils.localeCode())
                    .appendQueryParameter("tier", "aa")

            val encQuery = getParams.build().encodedQuery
            Timber.d(encQuery)

            val request = bearerBuilder(context)
                    .url(STREAM_URL + "?" + encQuery)
                    .build()
            try {
                val r = client.newCall(request).execute()
                if (r.isRedirect) {
                    subscriber.onSuccess(r.headers().get("Location"))
                } else {
                    if (r.code() == 401 || r.code() == 402 || r.code() == 403) {
                        val rejectionReason = r.header("X-Rejected-Reason")
                        if (rejectionReason != null) {
                            try {
                                val rejectionReasonEnum = ServerRejectionException.RejectionReason
                                        .valueOf(rejectionReason.toUpperCase())
                                Timber.e(ServerRejectionException(rejectionReasonEnum),
                                        "getStreamURL: serverRejected")
                                when (rejectionReasonEnum) {
                                    ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED -> {
                                        GoogleLogin.retryGoogleAuth(context)
                                        subscriber.onError(ServerRejectionException(rejectionReasonEnum))
                                    }
                                    ServerRejectionException.RejectionReason.ANOTHER_STREAM_BEING_PLAYED,
                                    ServerRejectionException.RejectionReason.STREAM_RATE_LIMIT_REACHED,
                                    ServerRejectionException.RejectionReason.TRACK_NOT_IN_SUBSCRIPTION,
                                    ServerRejectionException.RejectionReason.WOODSTOCK_SESSION_TOKEN_INVALID,
                                    ServerRejectionException.RejectionReason.WOODSTOCK_ENTRY_ID_INVALID,
                                    ServerRejectionException.RejectionReason.WOODSTOCK_ENTRY_ID_EXPIRED,
                                    ServerRejectionException.RejectionReason.WOODSTOCK_ENTRY_ID_TOO_EARLY,
                                    ServerRejectionException.RejectionReason.DEVICE_VERSION_BLACKLISTED -> {
                                        subscriber.onError(ServerRejectionException(rejectionReasonEnum))
                                    }
                                }
                            } catch (e: IllegalArgumentException) {
                                try {
                                    GoogleLogin.retryGoogleAuthSync(context)
                                } catch (s: Exception) {
                                    Timber.e(e, "Exception retrying Google Auth")
                                }

                                subscriber.onError(ServerRejectionException(ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED))
                            }

                        } else {
                            try {
                                GoogleLogin.retryGoogleAuthSync(context)
                            } catch (e: Exception) {
                                Timber.e(e, "Exception retrying Google Auth")
                            }

                            subscriber.onError(ServerRejectionException(
                                    ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED))
                        }
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

    private fun getNetHeader(context: Context): String {
        when (IdentityUtils.networkType(context)) {
            NetworkType.WIFI -> return "wifi"
            NetworkType.ETHER -> return "ether"
            NetworkType.MOBILE -> return "mob"
            else -> return ""
        }
    }

    private fun getStreamQualityHeader(context: Context): String {
        var streamQuality: StreamQuality?
        when (IdentityUtils.networkType(context)) {
            NetworkType.WIFI, NetworkType.ETHER -> {
                streamQuality = CarbonPlayerApplication.instance.preferences
                        .preferredStreamQualityWifi
            }
            else -> streamQuality = CarbonPlayerApplication.instance.preferences
                    .preferredStreamQualityMobile
        }
        if (streamQuality == null)
            streamQuality = StreamQuality.MEDIUM
        when (streamQuality) {
            StreamQuality.HIGH -> return "hi"
            StreamQuality.MEDIUM -> return "med"
            StreamQuality.LOW -> return "low"
            else -> return ""
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