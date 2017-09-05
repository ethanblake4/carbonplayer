package com.carbonplayer.model.network.utils

import okhttp3.MediaType
import okio.Okio
import okio.BufferedSink
import okhttp3.RequestBody
import okhttp3.internal.Util
import okio.Source
import java.io.IOException
import java.io.InputStream


object RequestBodyFactory {

    fun create(mediaType: MediaType, inputStream: InputStream): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return mediaType
            }

            override fun contentLength(): Long {
                try {
                    return inputStream.available().toLong()
                } catch (e: IOException) {
                    return 0
                }

            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                var source: Source? = null
                try {
                    source = Okio.source(inputStream)
                    sink.writeAll(source!!)
                } finally {
                    Util.closeQuietly(source)
                }
            }
        }
    }
}