package com.carbonplayer.model.network.entity

import android.net.Uri
import com.carbonplayer.model.network.utils.TailInputStream
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.common.base.Preconditions
import java.io.IOException
import java.io.InputStream

class ExoPlayerDataSource(private val streamingContent: StreamingContent) : DataSource {

    private var bytesRead: Long = 0
    private var inputStream: InputStream? = null

    override fun getUri(): Uri? {
        return Uri.parse("DefaultUri")
    }

    override fun open(dataSpec: DataSpec): Long {
        bytesRead = 0
        inputStream = TailInputStream(streamingContent.context, streamingContent, dataSpec.position)
        return dataSpec.length
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        Preconditions.checkNotNull<InputStream>(this.inputStream, "ExoPlayer didn't open the data source")
        val read = inputStream!!.read(buffer, offset, readLength)
        if (read == -1) {
            return -1
        }
        this.bytesRead += read.toLong()
        return read
    }

    @Throws(IOException::class)
    override fun close() {
        if (this.inputStream != null) {
            this.inputStream!!.close()
            this.inputStream = null
        }
    }
}