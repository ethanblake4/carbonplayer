package io.ethanblake4.exponentcore.hl.mfa

import android.webkit.CookieManager
import android.webkit.WebView

object MFAWebViewUtil {

    /**
     * Sets up a [WebView] to capture an MFA oAuth recover token.
     * Use [AutoMFA] for a higher level interface
     * @param webView The [WebView] to use
     * @param url The URL of the recover page
     * @param onSuccess The callback to run when an MFA token has been retrieved
     */
    @JvmStatic fun setupWebView(webView: WebView, url: String, onSuccess: (String) -> Unit) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        webView.settings.javaScriptEnabled = true
        webView.settings.displayZoomControls = false
        webView.settings.useWideViewPort = false
        webView.settings.loadWithOverviewMode = true
        webView.loadUrl(url)

        webView.webViewClient = TokenCaptureWebViewClient (onSuccess)
    }

}