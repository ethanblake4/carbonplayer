package com.carbonplayer.utils

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * Firebase Crash report
 */
class CrashReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        Crashlytics.log(priority, tag, obfuscate(message))

        throwable?.let {
            Crashlytics.logException(it)
        }
    }

    companion object {
        @JvmStatic fun obfuscate(message: String) : String {
            val index = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ[]{}:;\"'<>,.?/\\-_=+~`!@#\$%^&*()"
            val shift = "rstuvwxyzABCDEFGHIabcdefghi+~`!@#\$%^&*()jklmnopq0123456789[]{}:;\"'<>,.?/\\-_=JKLMNOPQRSTUVWXYZ"
            val sb = StringBuilder()
            var isObf = false
            message.forEach { c ->
                if(c == '|') isObf = !isObf
                else {
                    if(isObf) sb.append(shift[index.indexOf(c)])
                    else sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}