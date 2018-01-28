package com.carbonplayer.model.network.entity

import android.content.Context
import android.os.SystemClock
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.entity.TrackCache
import com.carbonplayer.model.entity.enums.StorageType
import com.carbonplayer.model.entity.enums.StreamQuality
import com.carbonplayer.model.entity.exception.ServerRejectionException
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.utils.addToAutoDispose
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.Exceptions
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*

class Stream @JvmOverloads constructor(val context: Context, val id: SongID, trackTitle: String,
                                       private val quality: StreamQuality, doDownload: Boolean = true) {

    private val downloadProgress = PublishSubject.create<Float>()
    var downloadInitialized = false
    private var completed: Long = 0
    private val seekMs: Long = 0
    private val downloadRequest: DownloadRequest?
    private val extraChunkSize: Long = 0
    private var filepath: String? = null
    var waitAllowed = true
    @Volatile var startReadPoint: Long = 0
    @get:Synchronized
    @set:Synchronized
    var url: String? = null
    private var lastWait: Long = 0
    private var len: Long = 0

    val isFinished: Boolean
        @Synchronized get() {
            if (downloadRequest == null) return true
            val state = downloadRequest.state
            return (state == DownloadRequest.State.COMPLETED || state == DownloadRequest.State.CANCELED
                    || state == DownloadRequest.State.FAILED)
        }

    val isDownloaded: Boolean
        @Synchronized get() {
            if (downloadRequest == null) return true
            val state = downloadRequest.state
            return state == DownloadRequest.State.COMPLETED
        }

    val isCompleted: Boolean
        @Synchronized get() {
            val state = downloadRequest!!.state
            return state == DownloadRequest.State.COMPLETED
        }

    val isFailed: Boolean
        @Synchronized get() {
            val state = downloadRequest!!.state
            return state == DownloadRequest.State.FAILED
        }

    init {

        if (!TrackCache.has(context, id, quality)) {
            Timber.i("Creating new DownloadRequest")
            downloadRequest = DownloadRequest(id, trackTitle, 100,
                    0, FileLocation(StorageType.CACHE,
                    TrackCache.getTrackFile(context, id, quality)),
                    true, quality, StreamQuality.UNDEFINED)
            this.filepath = TrackCache.getTrackFile(context, id, quality).absolutePath
            if (doDownload) initDownload()
        } else {
            Timber.i("File already exists")
            val file = TrackCache.getTrackFile(context, id, quality)
            this.filepath = file.absolutePath
            this.downloadRequest = null
            downloadProgress.onNext(1.0f)
            completed = file.length()
        }

    }

    fun initDownload() {
        downloadInitialized = true
        Timber.i("InitDownload for %s", this.toString())
        if (downloadRequest == null) return


        val protocols = ArrayList<okhttp3.Protocol>()
        protocols.add(okhttp3.Protocol.HTTP_1_1)
        val client = CarbonPlayerApplication.instance.getOkHttpClient(OkHttpClient.Builder()
                .protocols(protocols)
                .addNetworkInterceptor { chain ->
                    val originalResponse = chain.proceed(chain.request())
                    originalResponse.newBuilder()
                            .body(ProgressResponseBody(originalResponse.body(), { bytes, len, _ ->
                                downloadProgress.onNext((bytes / 100.0 / (len / 100.0)).toFloat())
                            }))
                            .build()
                })

        var useId = id.id
        if (useId == null) useId = id.storeId
        if (useId == null) useId = id.nautilusID

        Protocol.getStreamURL(context, useId!!)
                .retry { tries, err ->
                    if (err !is ServerRejectionException) return@retry false
                    if (err.rejectionReason !== ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
                        return@retry false
                    tries < 3
                }
                .flatMap { url ->
                    Completable.create { subscriber ->
                        try {
                            val response = client.newCall(Request.Builder()
                                    .url(url).build()).execute()
                            downloadRequest.state = DownloadRequest.State.DOWNLOADING

                            val sink = Okio.buffer(Okio.sink(File(filepath!!)))
                            val source = response.body()!!.source()

                            len = response.body()!!.contentLength()
                            var writ: Long = 0

                            while (writ < len) {
                                sink.write(source, Math.min(2048, len - writ))
                                writ += Math.min(2048, len - writ)
                                synchronized(this@Stream) {
                                    (this@Stream as java.lang.Object).notifyAll()
                                }
                                //downloadProgress.onNext(writ);
                            }
                            sink.close()
                            downloadRequest.state = DownloadRequest.State.COMPLETED
                            Timber.d("Download Completed")
                        } catch (e: IOException) {
                            throw Exceptions.propagate(e)
                        }
                        subscriber.onComplete()
                    }.toSingle { "" }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(context.mainLooper))
                .subscribe({ x ->
                    downloadRequest.state = DownloadRequest.State.COMPLETED
                    Timber.d("Download Completed")
                }) { error ->
                    if (error is ServerRejectionException)
                        Timber.e(error.rejectionReason.name)
                    Timber.e(error, "Exception getting stream URL")
                    downloadRequest.state = DownloadRequest.State.FAILED
                }.addToAutoDispose()

        downloadProgress.subscribe { `val` -> completed = (len / 10000f * `val`!!).toLong() * 10000L }
                .addToAutoDispose()
    }

    override fun toString(): String {
        return "Stream: { seekMs: " + this.seekMs +
                ", completed: " + this.completed +
                ", DownloadRequest: " + this.downloadRequest +
                " }"
    }

    fun progressMonitor(): Observable<Float> {
        return downloadProgress
    }

    @Synchronized
    @Throws(InterruptedException::class)
    fun waitForData(amount: Long) {
        while (!isFinished && this.completed < this.extraChunkSize + amount && this.waitAllowed) {
            val uptimeMs = SystemClock.uptimeMillis()
            if (lastWait + 10000 < uptimeMs) {
                this.lastWait = uptimeMs
                Timber.i("waiting for %d bytes in file: %s", amount, filepath)
                //                Timber.i("State: %s", downloadRequest.getState().name());
            }
            (this@Stream as java.lang.Object).wait()
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun getStreamFile(offset: Long): RandomAccessFile? {
        val streamFile: RandomAccessFile?
        var location: File? = null
        if (this.filepath != null) {
            location = File(this.filepath!!)
        } else if (downloadRequest != null) {
            location = downloadRequest.fileLocation.fullPath
        }
        Timber.d("StreamFile: location: %s", location)
        if (location == null) {
            streamFile = null
        } else {
            streamFile = RandomAccessFile(location, "r")
            streamFile.seek(offset)
        }

        return streamFile
    }

    internal fun getSongId(): SongID {
        return this.id
    }


    companion object {

        fun contentListToString(msg: String, list: List<Stream>): String {
            val builder = StringBuilder()
            builder.append(msg)
            builder.append("=[")
            for (content in list) {
                builder.append(content.getSongId())
                builder.append(", ")
            }
            builder.append("]")
            return builder.toString()
        }
    }


}
