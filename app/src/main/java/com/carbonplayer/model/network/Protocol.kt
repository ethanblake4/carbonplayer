package com.carbonplayer.model.network

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.net.http.AndroidHttpClient
import android.widget.Toast
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.ConfigEntry
import com.carbonplayer.model.entity.api.*
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.*
import com.carbonplayer.model.entity.exception.ResponseCodeException
import com.carbonplayer.model.entity.exception.ServerRejectionException
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto.GetHomeRequest
import com.carbonplayer.model.entity.radio.RadioSeed
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.model.entity.radio.request.RadioFeedRequest
import com.carbonplayer.model.entity.radio.response.RadioFeedResponse
import com.carbonplayer.model.entity.skyjam.*
import com.carbonplayer.model.network.HttpProtocol.RequestCapabilities
import com.carbonplayer.model.network.entity.PagedPlaylistResponse
import com.carbonplayer.model.network.entity.PagedPlentryResponse
import com.carbonplayer.model.network.entity.PagedTrackResponse
import com.carbonplayer.model.network.entity.PagedTrackResponseData
import com.carbonplayer.model.network.utils.ClientContextFactory
import com.carbonplayer.model.network.utils.IOUtils
import com.carbonplayer.utils.general.Either
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.protocol.URLSigning
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.exceptions.Exceptions
import io.realm.Realm
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.ClientContext
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.protocol.BasicHttpContext
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

    private const val SJ_URL = "https://mclients.googleapis.com/sj/v2.5/" // JSON URL
    private const val PA_URL = "https://music-pa.googleapis.com/v1/ij/" // Protobuf URL
    private const val STREAM_URL = "https://android.clients.google.com/music/mplay"
    private val TYPE_JSON = MediaType.parse("application/json; charset=utf-8")!!


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
            val request = playBuilder(context.baseContext, true)
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
            } catch (e: Throwable) {
                subscriber.onError(e)
            }
        }
    }

    /**
     * Gets a radio station feed based on a [RadioSeed]
     *
     * @param maxEntries Usually 25
     * @param reason should usually be [RadioFeedReason.INSTANT_MIX] except in case of
     * [RadioFeedReason.ARTIST_SHUFFLE]
     * @param sessionToken Only for free radios (unsupported in Carbon currently)
     *
     * TODO this should support [RadioFeedRequest.RadioStationRequest.recentlyPlayed]
     */
    fun radioFeed(context: Context, remoteSeedId: String, maxEntries: Int, reason: RadioFeedReason,
                  seedType: Int, sessionToken: String?): Single<RadioFeedResponse> {

        val radioRQ = RadioFeedRequest(
                CarbonPlayerApplication.instance.preferences.contentFilterAsInt,
                null,
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

        return HttpProtocol.post(
                context,
                UrlType.SKYJAM.with("radio/stationfeed")
                        .appendQueryParameter("rz", reason.toApiValue()),
                RequestCapabilities.new(false),
                radioRQ)
    }

    /**
     * Gets an explore "tab" (these are not displayed as tabs in the Play Music app,
     * and are not necessarily related to Carbon's "Explore" tab)
     * @see ExploreTabType for the different possible types
     *
     * @param context Context instance
     * @param tabType The [ExploreTabType] of tab to get
     * @param genre Only for [ExploreTabType.TOP_CHARTS] - the genre to use. This parameter
     * is never used in Carbon because we use [getTopCharts] instead which uses a different
     * endpoint (confused yet?)
     * @param maxEntries Maximum entries, usually 100
     */
    fun exploreTab(context: Context, tabType: ExploreTabType, genre: String? = null,
                   maxEntries: Int = 100): Single<ExploreTab> {
        val path = UrlType.SKYJAM.with("explore/tabs")
                .appendQueryParameter("tabs", tabType.ordinal.toString())
                .appendQueryParameter("num-items", maxEntries.toString())
                .apply { if (genre != null) appendQueryParameter("genre", genre) }

        return HttpProtocol.get<ExploreTabsResponse>(context, path,
                RequestCapabilities(true, false, true))
                .map { it.tabs.getOrNull(0) }

    }

    /**
     * Gets categories for radio stations for Explore tab
     *
     * @param context Context instance
     */
    fun stationCategories(context: Context): Single<StationCategory> {
        return HttpProtocol.get<StationCategoryResponse>(
                context, UrlType.SKYJAM.with("browse/stationcategories"),
                RequestCapabilities.cl())
                .map { it.root }
    }

    /**
     * Gets list of stations from a category returned by [stationCategories]
     *
     * @param context Context instance
     */
    fun stations(
            context: Context, category: StationCategory
    ): Single<List<SkyjamStation>> {
        return HttpProtocol.get<StationsResponse>(
                context, UrlType.SKYJAM.with("browse/stations/${category.id}"),
                RequestCapabilities.cl())
                .map { it.stations }
    }

    /**
     * Gets "listen now" (actually the Home page, or "AdaptiveHome" in Google-speak)
     *
     * This endpoint uses Protocol Buffers instead of JSON because f*ck you why not
     *
     * That means we have to use [AndroidHttpClient] instead of OkHttp (we probably
     * don't actually but it's too hard to fix properly)
     *
     * Calls to this function *will* fail every other time, for some reason
     *
     * @param context Context instance
     * @param previousDistilledContextToken The
     * [InnerJamApiV1Proto.GetHomeResponse.distilledContextToken_] that was returned
     * with the last request to this function, or null. This enables the endpoint to
     * send an empty response if nothing has changed.
     */
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
                val cookieStore = BasicCookieStore()

                // Create local HTTP context
                val localContext = BasicHttpContext()
                // Bind custom cookie store to the local context
                localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore)

                val httpRequest = HttpPost(PA_URL + "gethome?alt=proto")
                httpRequest.entity = entity
                httpRequest.setHeader("X-Device-ID", deviceId)
                httpRequest.setHeader("X-Device-Logging-ID", IdentityUtils.getLoggingID(context))
                httpRequest.setHeader("Authorization", "Bearer " +
                        CarbonPlayerApplication.instance.preferences.PlayMusicOAuth)
                val response = CarbonPlayerApplication.instance.androidHttpClient
                        .execute(httpRequest, localContext)
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

    /**
     * Gets the default Top Charts page
     * - Should we use [exploreTab] for this instead? What is the difference?
     *
     * @param context Context instance
     * @param offset Offset for paging (unused)
     * @param pageSize Number of entries to retrieve
     */
    fun getTopCharts(context: Context, offset: Int, pageSize: Int): Single<TopChartsResponse> {

        return HttpProtocol.get(
                context,
                UrlType.SKYJAM.with("browse/topchart")
                        .appendQueryParameter("tracksOffset", offset.toString())
                        .appendQueryParameter("albumsOffset", offset.toString())
                        .appendQueryParameter("maxTracks", pageSize.toString())
                        .appendQueryParameter("maxAlbums", pageSize.toString()),
                RequestCapabilities.cl())

    }

    /**
     * Gets a list of Top Charts genres
     * Should only be called very occasionally, as these almost never change
     *
     * @param context Context instance
     */
    fun getTopChartsGenres(context: Context): Single<TopChartsGenres> {
        return HttpProtocol.get(
                context, UrlType.SKYJAM.with("browse/topchartgenres"),
                RequestCapabilities(true, false, true))
    }

    /**
     * Gets a Top Charts page for a specified [genre]
     * - Should we use [exploreTab] for this instead? What is the difference?
     *
     * @param context Context instance
     * @param genre Selected [TopChartsGenres.Genre.id]
     * @param offset Offset for paging (unused)
     * @param pageSize Number of entries to retrieve
     */
    fun getTopChartsFor(context: Context, genre: String, offset: Int, pageSize: Int)
            : Single<TopChartsResponse> {

        return HttpProtocol.get(
                context,
                UrlType.SKYJAM.with("browse/topchartforgenre/$genre")
                        .appendQueryParameter("tracksOffset", offset.toString())
                        .appendQueryParameter("albumsOffset", offset.toString())
                        .appendQueryParameter("maxTracks", pageSize.toString())
                        .appendQueryParameter("maxAlbums", pageSize.toString()),
                RequestCapabilities.cl())
    }


    fun getSharedPlentries(context: Context, sharedToken: String): Observable<List<SkyjamPlentry>> {
        val client = CarbonPlayerApplication.instance.okHttpClient

        return Observable.create { subscriber ->
            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()


            val adapter = CarbonPlayerApplication.moshi.adapter(SharedPlentryResponse::class.java)
            val rqAdapter = CarbonPlayerApplication.moshi.adapter(SharedPlentryRequest::class.java)

            var continuationToken: String? = null

            do {
                val rqJson = SharedPlentryRequest.Entry(
                        250, sharedToken, continuationToken, 0L)
                        .asRequest()
                val request = bestBuilder(context, true, false, true)
                        .url(SJ_URL + "plentries/shared?" + getParams)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(TYPE_JSON, rqAdapter.toJson(rqJson).toByteArray()))
                        .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful && response.code() >= 200 && response.code() < 300) {
                    response.body()?.let { body ->
                        body.source()?.let {
                            val s = adapter.fromJson(it)
                            body.close()
                            s?.entries?.firstOrNull()?.playlistEntry?.let { e ->
                                subscriber.onNext(e)
                            }
                            continuationToken = s?.entries?.firstOrNull()?.nextPageToken
                        }
                    }
                }
            } while (continuationToken != null)

            subscriber.onComplete()
        }
    }

    fun getNautilusAlbum(context: Context, nid: String): Single<SkyjamAlbum> {
        return Single.fromCallable {
            val client = CarbonPlayerApplication.instance.okHttpClient
            val adapter = CarbonPlayerApplication.moshi.adapter(SkyjamAlbum::class.java)

            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()
                    .appendQueryParameter("nid", nid)
                    .appendQueryParameter("include-tracks", "true")
                    .appendQueryParameter("include-description", "true")

            val request = bestBuilder(context, true, false, true)
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
                        response.header("X-Rejection-Reason"), true, false, true)
            }
            response.body()?.string()?.let {
                throw ResponseCodeException(it)
            }; throw ResponseCodeException()
        }
    }

    fun getNautilusArtist(context: Context, nid: String): Observable<SkyjamArtist> {

        return Observable.fromCallable {
            val client = CarbonPlayerApplication.instance.okHttpClient
            val adapter = CarbonPlayerApplication.moshi.adapter(SkyjamArtist::class.java)

            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()
                    .appendQueryParameter("nid", nid)
                    .appendQueryParameter("include-albums", "true")
                    .appendQueryParameter("num-top-tracks", "50")
                    .appendQueryParameter("num-related-artists", "20")

            val request = bestBuilder(context, true, false, true)
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
                        response.header("X-Rejection-Reason"), true, false, true)
            }
            throw ResponseCodeException(response.body()!!.string())
        }
    }

    fun getNautilusPlaylist(context: Context, shareToken: String): Observable<SkyjamPlaylist> {

        return Observable.fromCallable {
            val client = CarbonPlayerApplication.instance.okHttpClient
            val adapter = CarbonPlayerApplication.moshi.adapter(SkyjamPlaylist::class.java)

            val getParams = Uri.Builder()
                    .appendQueryParameter("alt", "json")
                    .appendDefaults()

            val request = bestBuilder(context, true, false, true)
                    .url(SJ_URL + "playlists/$shareToken" + getParams)
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
                        response.header("X-Rejection-Reason"), true, false, true)
            }
            throw ResponseCodeException(response.body()!!.string())
        }
    }

    /**
     * Searches Google Play Music for [query]
     *
     * @param context [Context] instance
     * @param query the thing to search for
     * @param startToken unused
     */
    fun search(context: Context, query: String, startToken: String = ""): Single<SearchResponse> {

        val path = UrlType.SKYJAM.with("query")
                .appendQueryParameter("q", query)
                .appendQueryParameter("query-type", "1")
                .apply {
                    if (CarbonPlayerApplication.instance.useSearchClustering)
                        appendQueryParameter("ic", "true")
                    if (startToken.isNotEmpty())
                        appendQueryParameter("start-token", startToken)
                }
                .appendQueryParameter("max-results", "100")
                /* 1: Song, 2: Artist, 3: Album, 4: Playlist, 5: Genre
                 * 6: Station, 7: Situation, TODO 8: Video, 9: Podcast */
                .appendQueryParameter("ct", "1,2,3,4,6,7")

        return HttpProtocol.get(
                context,
                path,
                RequestCapabilities.new(false))

    }

    /**
     * Gets a list of search suggestions. Essentially a lighter version of [search].
     *
     * @param context [Context] instance
     * @param query the thing to search for
     */
    fun suggest(context: Context, query: String): Single<SuggestResponse> {

        val suggestRequest = SuggestRequest(
                SuggestRequest.SuggestCapabilities(listOf(
                        1, 2, 3, 4
                ), true), query)

        return HttpProtocol.post(
                context,
                UrlType.SKYJAM.with("querysuggestion"),
                RequestCapabilities.new(query.isEmpty()),
                suggestRequest)

    }

    @JvmStatic
    fun listTracks(context: Context): Observable<com.google.common.base.Optional<List<SkyjamTrack>>> {
        val adapter = CarbonPlayerApplication.moshi.adapter(PagedTrackResponse::class.java)
        return HttpProtocol.rawPagedFeed<PagedTrackResponseData?, PagedTrackResponse>(
                context, "trackfeed", adapter
        )
                .map { response ->
                    if (response is Either.Left) {
                        Timber.d("mapped existent optional")
                        com.google.common.base.Optional.fromNullable(response.value?.items)
                    } else {
                        Timber.d("mapped nonexistent optional")
                        com.google.common.base.Optional.absent()
                    }
                }
    }

    @JvmStatic
    fun listPlaylists(context: Activity): Observable<com.google.common.base.Optional<List<SkyjamPlaylist>>> {
        val adapter = CarbonPlayerApplication.moshi.adapter(PagedPlaylistResponse::class.java)
        return HttpProtocol.rawPagedFeed(
                context, "playlistfeed", adapter
        )
                .map { response ->
                    if (response is Either.Left)
                        com.google.common.base.Optional.fromNullable(response.value?.items)
                    else com.google.common.base.Optional.absent()
                }
    }

    @JvmStatic
    fun listPlaylistEntries(context: Activity): Observable<com.google.common.base.Optional<List<SkyjamPlentry>>> {
        val adapter = CarbonPlayerApplication.moshi.adapter(PagedPlentryResponse::class.java)
        return HttpProtocol.rawPagedFeed(
                context, "plentryfeed", adapter
        )
                .map { response ->
                    if (response is Either.Left)
                        com.google.common.base.Optional.fromNullable(response.value?.items)
                    else com.google.common.base.Optional.absent()
                }
    }

    fun getStreamURL(context: Context, song_id: String): Single<String> {
        // Stream URL endpoint only supports HTTP 1.1
        val protocols = ArrayList<okhttp3.Protocol>()
        protocols.add(okhttp3.Protocol.HTTP_1_1)

        // Give us the redirects!
        val client = CarbonPlayerApplication.instance.getOkHttpClient(
                OkHttpClient().newBuilder()
                        .followRedirects(false)
                        .followSslRedirects(false)
                        .protocols(protocols))

        return Single.create<String> { subscriber ->

            // Stream URLs need to be signed to prove that we're actually Google
            // We're not... but we're doing it anyway, psych!
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

            // Remote track
            if (song_id.startsWith("T") || song_id.startsWith("D"))
                getParams.appendQueryParameter("mjck", song_id)
            else // Library track
                getParams.appendQueryParameter("songid", song_id)

            getParams
                    .appendDefaults()
                    .appendQueryParameter("targetkbps", "180") // :: Does this do anything?
                    .appendQueryParameter("audio_formats", "mp3") // :: Are there other options?
                    .appendQueryParameter("p", // This means "is it a phone?"
                            if (IdentityUtils.getDeviceIsSmartphone(context)) "1" else "0")
                    .appendQueryParameter("opt", getStreamQualityHeader(context)) // quality
                    .appendQueryParameter("net", getNetHeader(context)) // network type
                    .appendQueryParameter("pt", "e") // :: what the fuck
                    .appendQueryParameter("adaptive", "true") // :: ???
                    //.appendQueryParameter("dt", "pc")
                    .appendQueryParameter("slt", salt)
                    .appendQueryParameter("sig", digest)

            val encQuery = getParams.build().encodedQuery
            Timber.d(encQuery)

            val request = bestBuilder(context, false, true, false)
                    .url(STREAM_URL + "?" + encQuery)
                    .build()
            try {
                // Capture the redirect, which will be the stream URL
                // (what a weird method)
                val r = client.newCall(request).execute()
                val locHeader = r.headers().get("Location")
                Timber.d("Location is $locHeader")
                if (r.isRedirect && locHeader != null) {
                    subscriber.onSuccess(locHeader)
                } else {
                    if (r.code() == 401 || r.code() == 402 || r.code() == 403) {
                        val rejectionReason = r.header("X-Rejected-Reason")
                        subscriber.onError(handle400(context, r.code(), rejectionReason,
                                false, true, false))
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

    /**
     * Creates, deletes, or updates an [ITrack] in the Library
     *
     * Technically, we should batch these calls using a SyncAdapter.
     * But who's got time for that?
     *
     * There is another endpoint specifically for non-batch updates but it doesn't work :|
     */
    fun mutateEntity(context: Context, entity: ITrack, operation: MutateOperation): Completable {
        val mutateRequest = MutateTrackRequest(
                if (operation == MutateOperation.CREATE) entity.syncable().forAdd() else null,
                if (operation == MutateOperation.DELETE) entity.storeId else null,
                if (operation == MutateOperation.UPDATE) entity.syncable() else null
        )
        return HttpProtocol.post<MutateTrackRequest.Batch, MutateTrackResponse>(
                context,
                UrlType.SKYJAM.with("trackbatch"),
                RequestCapabilities(true, false, true),
                MutateTrackRequest.Batch(listOf(mutateRequest)))
                .map {
                    if (it.wasSuccessful) it
                    else throw ResponseCodeException(it.response_code ?: "Unknown")
                }.toCompletable()
    }

    /**
     * Creates, deletes, or updates an [IAlbum] in the Library
     *
     * Technically, we should batch these calls using a SyncAdapter.
     * But who's got time for that?
     */
    fun mutateEntity(
            context: Context, entity: IAlbum, operation: MutateOperation
    ) : Single<IAlbum> {

        if(operation == MutateOperation.CREATE &&
                entity is SkyjamAlbum && entity.tracks?.isEmpty() != false) {
            // We need tracks to do a Create
            return getNautilusAlbum(context, entity.albumId)
                    .flatMap {
                        mutateEntity(context, it, operation).map { it }
                    }
        } else if (operation == MutateOperation.DELETE && entity !is Album) {
            return mutateEntity(context, Realm.getDefaultInstance().where(Album::class.java)
                    .equalTo(Album.ID, entity.albumId)
                    .findFirst()!!, operation)
        }

        val mutations = MusicLibrary.getAllAlbumTracks(entity)
                .map {
                    MutateTrackRequest(
                            if (operation == MutateOperation.CREATE) it.syncable().forAdd() else null,
                            if (operation == MutateOperation.DELETE) it.id ?: it.storeId else null,
                            if (operation == MutateOperation.UPDATE) it.syncable() else null
                    )
                }

        return HttpProtocol.post<MutateTrackRequest.Batch, MutateTrackResponse>(
                context,
                UrlType.SKYJAM.with("trackbatch"),
                RequestCapabilities(true, false, true),
                MutateTrackRequest.Batch(mutations))
                .map {
                    if (it.wasSuccessful) entity
                    else throw ResponseCodeException(it.response_code ?: "Unknown")
                }
    }

    /** Handles a 400 error
     * Sometimes, these errors return a X-Rejection-Reason header which this method
     * can interpret into [ServerRejectionException]s
     *
     * Also, in many cases this method will try to re-auth the correct token.
     * If using [Observable.retry] this can seamlessly resolve auth expiry without
     * interrupting the user flow.
     */
    fun handle400(context: Context,
                          code: Int,
                          rejectionReason: String?,
                          acceptCL: Boolean,
                          acceptBearer: Boolean,
                          sameWithFree: Boolean = true): Exception {
        if (rejectionReason != null) {
            try {
                val rejectionReasonEnum = ServerRejectionException.RejectionReason
                        .valueOf(rejectionReason.toUpperCase())
                Timber.e(ServerRejectionException(rejectionReasonEnum),
                        "getStreamURL: serverRejected")
                when (rejectionReasonEnum) {
                    ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED -> {
                        reAuthSync(context, acceptCL, acceptBearer, sameWithFree)
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
                    reAuthSync(context, acceptCL, acceptBearer, sameWithFree)
                } catch (s: Exception) {
                    Timber.e(e, "Exception retrying Google Auth")
                }

                return ServerRejectionException(ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
            }
        } else {
            try {
                reAuthSync(context, acceptCL, acceptBearer, sameWithFree)
            } catch (e: Exception) {
                Timber.e(e, "Exception retrying Google Auth")
            }

            return ServerRejectionException(
                    ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
        }
    }

    private fun reAuthSync(
            context: Context,
            acceptCL: Boolean,
            acceptBearer: Boolean,
            sameWithFree: Boolean
    ) {
        if(acceptCL && !CarbonPlayerApplication.instance.preferences.useTestToken) {
            Toast.makeText(
                    context,
                    "Your Google account is no longer valid. Go to Settings > Account, sign out, and log back in.",
                    Toast.LENGTH_LONG).show()
        } else if(acceptBearer)
            GoogleLogin.retryGoogleAuthSync(context)
        else if( sameWithFree || !CarbonPlayerApplication.instance.preferences.useTestToken)
            GoogleLogin.retryPlayOAuthSync(context)
        else GoogleLogin.retryTestOAuthSync(context)
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

    private fun bestBuilder(
            context: Context,
            acceptCL: Boolean,
            acceptBearer: Boolean,
            sameWithFree: Boolean = true
    ) : Request.Builder {
        if(acceptCL && !CarbonPlayerApplication.instance.preferences.useTestToken)
            return defaultBuilder(context)
        if(acceptBearer && (sameWithFree ||
                !CarbonPlayerApplication.instance.preferences.useTestToken))
            return bearerBuilder(context)
        return playBuilder(context, sameWithFree)
    }

    private fun bearerBuilder(context: Context): Request.Builder {
        if(IdentityUtils.isAutomatedTestDevice(context)) return playBuilder(context)
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

    private fun playBuilder(context: Context, sameWithFree: Boolean = true): Request.Builder {

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