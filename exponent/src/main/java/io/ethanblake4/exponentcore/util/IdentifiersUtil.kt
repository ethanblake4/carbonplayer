package io.ethanblake4.exponentcore.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.telephony.TelephonyManager
import java.util.*

object IdentifiersUtil {

    fun makeUserAgent(appName: String, appBuildNumber: String,
                      buildProduct: String, buildID: String, gzip: Boolean? = true) =
            "$appName/$appBuildNumber ($buildProduct $buildID)" +
                    gzip?.let { if(it) return@let ";gzip" else "" }.orEmpty()

    @SuppressLint("HardwareIds")
    fun getAndroidID(context: Context): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)!!

    fun getDeviceLanguage(): String = Locale.getDefault().language

    fun getDeviceCountryCode(): String = Locale.getDefault().country.toLowerCase()

    fun getOperatorCountryCode(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.simCountryIso.toLowerCase()
    }

    fun getGservicesID(context: Context, fallback: Boolean): String {
        val gId = Gservices.getLong(
                context.contentResolver, "android_id", 0).toString()
        return if (gId == "0") if (fallback) getAndroidID(context) else "0" else gId
    }
}