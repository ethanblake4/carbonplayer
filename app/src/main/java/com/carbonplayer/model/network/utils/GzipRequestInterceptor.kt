package com.carbonplayer.model.network.utils

import okhttp3.Interceptor
import okhttp3.MediaType
import okio.GzipSink
import okio.Okio
import okio.BufferedSink
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException


internal class GzipRequestInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
            return chain.proceed(originalRequest)
        }



        val compressedRequest = originalRequest.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(originalRequest.method(), gzip(originalRequest.body() as RequestBody))
                .build()
        return chain.proceed(compressedRequest)
    }

    private fun gzip(body: RequestBody): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return body.contentType()
            }

            override fun contentLength(): Long {
                return -1 // We don't know the compressed length in advance!
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val gzipSink = Okio.buffer(GzipSink(sink))
                body.writeTo(gzipSink)
                gzipSink.close()
            }
        }
    }
}
