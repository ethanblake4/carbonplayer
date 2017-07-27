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
        if(throwable != null) {
            FirebaseCrash.logcat(priority, tag, "Exception "+ throwable.localizedMessage + ": stack trace: \n" + throwable.stackTrace.contentToString())
        }
    }

    fun obfuscate(message: String) : String {

        val index = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ[]{}:;\"'<>,.?/\\-_=+~`!@#\$%^&*()"
        val shift = "rstuvwxyzABCDEFGHIabcdefghi+~`!@#\$%^&*()jklmnopq0123456789[]{}:;\"'<>,.?/\\-_=JKLMNOPQRSTUVWXYZ"
        val sb = StringBuilder()
        var isObf = false
        message.forEach { c ->
            if(c == '|') isObf = !isObf
            else {
                if(isObf) sb.append(shift.get(index.indexOf(c)))
                else sb.append(c)
            }
        }
        return sb.toString()
    }
}