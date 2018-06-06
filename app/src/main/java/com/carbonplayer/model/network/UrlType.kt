package com.carbonplayer.model.network

import android.net.Uri


enum class UrlType {
    SKYJAM,
    INNERJAM,
    MPLAY;

    fun toPath(): String = when(this) {
        SKYJAM -> HttpProtocol.SJ_URL
        INNERJAM -> HttpProtocol.PA_URL
        MPLAY -> HttpProtocol.STREAM_URL
    }

    fun with(path: String) = Uri.parse(toPath() + path).buildUpon()!!
}