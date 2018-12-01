package io.ethanblake4.exponentcore.hl.mfa

import android.app.Activity
import android.app.Dialog
import android.webkit.WebView
import io.ethanblake4.exponentcore.Exponent
import io.ethanblake4.exponentcore.R
import io.ethanblake4.exponentcore.auth.GoogleAuth
import io.ethanblake4.exponentcore.hl.ExAccountInfo
import io.ethanblake4.exponentcore.hl.ExponentAccounts
import io.ethanblake4.exponentcore.hl.NoTokenException
import io.ethanblake4.exponentcore.model.error.NeedsBrowserException

object AutoMFA {

    /**
     * Coordinates display of an MFA browser recover session,
     * after receiving a [NeedsBrowserException] from [GoogleAuth.masterAuthAsync]
     *
     * @param context Context to create dialog (only used if [webView] is null)
     * @param email Email to use for re-auth. Should be the same as previous master auth call.
     * @param exception The [NeedsBrowserException] thrown by [GoogleAuth.masterAuthAsync]
     * @param onSuccess Code to run after authentication has completed
     * @param onError Code to run if an error is encountered
     * @param tokenCaptured Optional. Code to run after the token has been captured from
     * the [WebView]. For example, you might show a message stating "Logging in" here.
     * @param webView Optional. A custom WebView to control instead of creating one anew.
     */
    @JvmStatic fun handleBrowserRecover(
            context: Activity, email: String, exception: NeedsBrowserException,
            onSuccess: (ExAccountInfo) -> Unit, onError: (Throwable?) -> Unit,
            tokenCaptured: ((Unit) -> Unit)? = null, webView: WebView? = null) {

        val afterTk = { token: String ->
            tokenCaptured?.invoke(Unit)
            GoogleAuth.masterReauthAsync(email, token, { info ->
                if(info == null) onError(NoTokenException())
                else {
                    Exponent.tokenRegistry.saveMasterTokenFor(email, info)
                    ExponentAccounts.setAccount(email)
                    onSuccess(ExponentAccounts.currentAccount!!)
                }
            }, onError)
        }

        if(webView != null) {
            context.runOnUiThread { MFAWebViewUtil.setupWebView(webView, exception.url, afterTk) }
        } else {
            context.runOnUiThread {
                val dialog = Dialog(context)
                dialog.setContentView(R.layout.web_dialog)
                MFAWebViewUtil.setupWebView(
                        dialog.findViewById(R.id.ex_auth_webview), exception.url) {
                    afterTk(it)
                    dialog.hide()
                }
                dialog.show()
            }
        }
    }



}