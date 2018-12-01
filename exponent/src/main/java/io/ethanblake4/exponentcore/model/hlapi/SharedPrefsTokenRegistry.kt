package io.ethanblake4.exponentcore.model.hlapi

import android.content.SharedPreferences
import io.ethanblake4.exponentcore.model.ClientTokenInfo
import io.ethanblake4.exponentcore.model.GoogleAuthInfo
import io.ethanblake4.exponentcore.model.MasterTokenInfo

class SharedPrefsTokenRegistry (val prefs: SharedPreferences): TokenRegistry {

    override fun getMasterTokenFor(email: String): MasterTokenInfo? = with(prefs) {
        val pre = "$SPRE-MT-$email"
        if(!contains("$pre-Token")) null
        else MasterTokenInfo(
                getString("$pre-Token", ""),
                getString("$pre-Auth", ""),
                getString("$pre-SID", null),
                getString("$pre-LSID", null),
                getString("$pre-services",null).split(",")
                        .dropLastWhile { it.isEmpty() }.toList(),
                email,
                getString("$pre-firstName", ""),
                getString("$pre-lastName", ""),
                getInt("$pre-GooglePlusUpdate", -1).let {
                    if(it == -1) null else it
                }
        )
    }

    override fun getClientLoginTokenFor(email: String, service: String): GoogleAuthInfo? = with(prefs) {
        val pre = "$SPRE-CL-$email-$service"
        if(!contains("$pre-Auth")) null
        else GoogleAuthInfo(
                getString("$pre-Auth", ""),
                getString("$pre-issueAdvice", null),
                getInt("$pre-expiry", -1),
                getString("$pre-storeConsentRemotely",null)
        )
    }

    override fun getOAuthTokenFor(email: String, service: String, app: String): ClientTokenInfo? = with(prefs) {
        val pre = "$SPRE-OA-$email-$service-$app"
        if(!contains("$pre-Auth")) null
        else ClientTokenInfo(
                getString("$pre-Auth", ""),
                getString("$pre-SID", null),
                getString("$pre-LSID", null),
                getString("$pre-services",null).split(",")
                        .dropLastWhile { it.isEmpty() }.toList(),
                email,
                getString("$pre-firstName", ""),
                getString("$pre-lastName", ""),
                getInt("$pre-GooglePlusUpdate", -1).let {
                    if(it == -1) null else it
                }
        )
    }

    override fun saveMasterTokenFor(email: String, token: MasterTokenInfo) {
        val pre = "$SPRE-MT-$email"
        with(prefs.edit()) {
            putString("$pre-Token", token.token)
            putString("$pre-Auth", token.auth)
            token.SID?.let{ putString("$pre-SID", it) }
            token.LSID?.let{ putString("$pre-LSID", it) }
            token.services?.reduce { acc, s -> "$acc,$s" }?.let{ putString("$pre-services", it) }
            putString("$pre-firstName", token.firstName)
            putString("$pre-lastName", token.lastName)
            token.GooglePlusUpdate?.let { putInt("$pre-GooglePlusUpdate", it ) }
            apply()
        }
    }

    override fun saveClientLoginTokenFor(email: String, service: String, token: GoogleAuthInfo) {
        val pre = "$SPRE-CL-$email-$service"
        with(prefs.edit()) {
            putString("$pre-Auth", token.auth)
            token.issueAdvice?.let{ putString("$pre-issueAdvice", it) }
            putInt("$pre-expiry", token.expiry)
            token.storeConsentRemotely?.let{ putString("$pre-storeConsentRemotely", it) }
            apply()
        }
    }

    override fun saveOAuthTokenFor(email: String, service: String, app: String, token: ClientTokenInfo) {
        val pre = "$SPRE-OA-$email-$service-$app"
        with(prefs.edit()) {
            putString("$pre-Auth", token.auth)
            token.SID?.let{ putString("$pre-SID", it) }
            token.LSID?.let{ putString("$pre-LSID", it) }
            token.services?.reduce { acc, s -> "$acc,$s" }?.let{ putString("$pre-services", it) }
            putString("$pre-firstName", token.firstName)
            putString("$pre-lastName", token.lastName)
            token.GooglePlusUpdate?.let { putInt("$pre-GooglePlusUpdate", it ) }
            apply()
        }
    }

    override fun deleteMasterTokenFor(email: String) {
        prefs.edit().remove("$SPRE-MT-$email").apply()
    }

    override fun deleteClientLoginTokensFor(email: String) {
        with(prefs.edit()) {
            prefs.all.filter { it.key.startsWith("$SPRE-CL-$email") }.forEach {
                remove(it.key)
            }
            apply()
        }
    }

    override fun deleteOAuthTokensFor(email: String) {
        with(prefs.edit()) {
            prefs.all.filter { it.key.startsWith("$SPRE-OA-$email") }.forEach {
                remove(it.key)
            }
            apply()
        }
    }

    companion object {
        private const val SPRE = "ExpnT"
    }

}