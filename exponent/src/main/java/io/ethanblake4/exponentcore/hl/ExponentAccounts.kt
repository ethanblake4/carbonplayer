package io.ethanblake4.exponentcore.hl

import android.util.Log
import io.ethanblake4.exponentcore.Exponent
import io.ethanblake4.exponentcore.auth.GoogleAuth
import io.ethanblake4.exponentcore.model.MasterTokenInfo

object ExponentAccounts {

    var currentAccount: ExAccountInfo? = null
        private set
    var master: MasterTokenInfo? = null
        private set

    @JvmStatic fun setAccount(email: String): Boolean {
        Exponent.tokenRegistry.getMasterTokenFor(email)?.let {
            master = it
            Log.d("EX_ACCT", it.toString())
            currentAccount = ExAccountInfo(email, it.firstName, it.lastName)
            return true
        }
        return false
    }

    @JvmStatic fun clearAccount() {
        master = null
        currentAccount = null
    }

    @JvmOverloads @JvmStatic fun login(email: String, password: String,
                                       onSuccess: (ExAccountInfo) -> Unit,
                                       onError: (Throwable?) -> Unit,
                                       forceReauth: Boolean = false) {
        // Try to access our existing information
        if(!forceReauth && setAccount(email)) {
            onSuccess(currentAccount!!); return
        }
        // Perform (Re-) authorization
        GoogleAuth.masterAuthAsync(email, password, { token ->
            if(token == null) onError(NoTokenException())
            else {
                Exponent.tokenRegistry.saveMasterTokenFor(email, token)
                setAccount(email)
                onSuccess(currentAccount!!)
            }
        }, onError)
    }

    @JvmStatic fun logout(email: String) {
        if(currentAccount?.email == email) clearAccount()
        Exponent.tokenRegistry.apply {
            deleteMasterTokenFor(email)
            deleteClientLoginTokensFor(email)
            deleteOAuthTokensFor(email)
        }
    }

}