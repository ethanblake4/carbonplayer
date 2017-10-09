package com.carbonplayer.utils.ui

import android.database.ContentObserver
import android.os.Handler

/**
 * Listens
 */
class VolumeObserver(
        val callback: () -> Unit,
        handler: Handler = Handler()
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)

        callback()
    }

    override fun deliverSelfNotifications(): Boolean {
        return super.deliverSelfNotifications()
    }
}