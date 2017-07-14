package com.carbonplayer.utils

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import timber.log.Timber

/**
 * Created by ethanelshyeb on 7/13/17.
 */
class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        FirebaseCrash.logcat(priority, tag, message)
        if(throwable != null) FirebaseCrash.report(throwable)
    }
}