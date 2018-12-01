package io.ethanblake4.exponentcore.model

data class GoogleAuthInfo (
        /** The requested auth token **/
        val auth: String,
        /** Usually 'auto' **/
        val issueAdvice: String?,
        /** Token expiry timestamp */
        var expiry: Int,
        /** Unknown **/
        var storeConsentRemotely: String?
)