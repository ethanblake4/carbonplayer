package com.carbonplayer.model.network.entity

import android.content.Context
import android.os.SystemClock
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.TrackCache
import com.carbonplayer.model.entity.base.ITrack
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
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*

class Stream (
        val context: Context,
        var track: ITrack,
        private val quality: StreamQuality,
        doDownload: Boolean = true
) {
    private val id = SongID(track)
    private val downloadProgress = BehaviorSubject.create<Long>()
    var downloadInitialized = false
    private var completed: Long = 0
    private val seekMs: Long = 0
    private val downloadRequest: DownloadRequest?
    private val extraChunkSize: Long = 2048
    private var filepath: String? = null
    private var waitAllowed = true
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

    init {

        if (id.localId == -1L || !TrackCache.has(context, id, quality)) {
            if(track is ParcelableTrack) createInDb()

            Timber.i("Creating new DownloadRequest")
            downloadRequest = DownloadRequest(id, track.title, 100,
                    0, FileLocation(StorageType.CACHE,
                    TrackCache.getTrackFile(context, id, quality)),
                    true, quality, StreamQuality.UNDEFINED)

            if (doDownload) {
                this.filepath = TrackCache.getTrackFile(context, id, quality).absolutePath
                initDownload()
            }
        } else {
            Timber.i("File already exists")
            val file = TrackCache.getTrackFile(context, id, quality)
            filepath = file.absolutePath
            downloadRequest = null
            downloadProgress.onNext(file.length())
            completed = file.length()
        }

    }

    @Synchronized
    private fun createInDb() {
        /*Realm.getDefaultInstance().executeTransaction { rlm ->
            (track as ParcelableTrack).let {
                track = MusicLibrary.addOneToDatabase(rlm, it, true, track.inLibrary)
            }
        }*/
    }

    fun initDownload() {
        downloadInitialized = true
        Timber.i("InitDownload for $this")
        if (downloadRequest == null) return

        val protocols = ArrayList<okhttp3.Protocol>()

        protocols.add(okhttp3.Protocol.HTTP_1_1)

        val client = CarbonPlayerApplication.instance.getOkHttpClient(OkHttpClient.Builder()
                .protocols(protocols)
                .addNetworkInterceptor { chain -> ProgressResponseBody.intercept(chain, { bytes, _, _ ->
                    downloadProgress.onNext(bytes)
                }) })


        Protocol.getStreamURL(context, id.id ?: id.storeId!!)
                .retry { tries, err ->
                    if (err !is ServerRejectionException) return@retry false
                    if (err.rejectionReason !== ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
                        return@retry false
                    tries < 3
                }
                .flatMap { url ->
                    Completable.create { subscriber ->

                        try {

                            Timber.d("Request to $url")

                            val response = client.newCall(Request.Builder()
                                    .url(url).build()).execute()

                            downloadRequest.state = DownloadRequest.State.DOWNLOADING

                            val location: File = if (filepath != null) {
                                File(this.filepath!!)
                            } else {
                                downloadRequest.fileLocation.fullPath
                            }

                            val sink = Okio.buffer(Okio.sink(location))

                            val source = response.body()!!.source()

                            len = response.body()!!.contentLength()
                            var writ = 0L

                            Timber.d("len is $len")

                            if(len != -1L)
                                while (writ < len) {

                                    sink.write(source, Math.min(2048, len - writ))
                                    writ += Math.min(2048, len - writ)
                                    completed = writ
                                    synchronized(this@Stream) {
                                        (this as java.lang.Object).notifyAll()
                                    }

                                }
                            else /* What causes this? Very annoying... */
                                try {
                                    while (true /* wait for exception */) {
                                        sink.write(source, 2048)
                                        writ += 2048
                                        completed = writ
                                        synchronized(this@Stream) {
                                            (this as java.lang.Object).notifyAll()
                                        }
                                    }
                                } catch (e: Exception) { // Expected
                                    Timber.d("-1-indexed source completed")
                                }

                            sink.close()

                            downloadRequest.state = DownloadRequest.State.COMPLETED
                            Timber.d("Download Completed")
                        } catch (e: Exception) {
                            throw Exceptions.propagate(e)
                        }
                        subscriber.onComplete()
                    }.toSingle { "" }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(context.mainLooper))
                .subscribe({ _ ->
                    downloadRequest.state = DownloadRequest.State.COMPLETED
                    Timber.d("Download Completed")
                }) { error ->
                    if (error is ServerRejectionException)
                        Timber.e(error.rejectionReason.name)
                    Timber.e(error, "Exception getting stream URL")
                    downloadRequest.state = DownloadRequest.State.FAILED
                }.addToAutoDispose()
    }

    override fun toString()= "Stream: { seekMs: $seekMs, completed: $completed, DownloadRequest: $downloadRequest }"

    fun progressMonitor(): Observable<Float> = downloadProgress.map { it.toFloat() / len.toFloat() }

    @Synchronized
    @Throws(InterruptedException::class)
    fun waitForData(amount: Long) {
        while (!isFinished && completed < extraChunkSize + amount && waitAllowed && downloadRequest != null) {
            val uptimeMs = SystemClock.uptimeMillis()
            if (lastWait < uptimeMs) {
                this.lastWait = uptimeMs
                Timber.i("current $completed, waiting for ${amount + extraChunkSize} bytes in file $filepath")
            }
            (this@Stream as java.lang.Object).wait()
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun getStreamFile(offset: Long): RandomAccessFile? {
        val streamFile: RandomAccessFile?
        var location: File? = null
        if (filepath != null) {
            location = File(this.filepath!!)
        } else if (downloadRequest != null) {
            location = downloadRequest.fileLocation.fullPath
        }
        Timber.d("StreamFile: location: %s", location)
        if (location == null) {
            streamFile = null
        } else {
            //location.createNewFile()
            streamFile = RandomAccessFile(location, "r")
            streamFile.seek(offset)
        }

        return streamFile
    }


}
