package io.ethanblake4.exponentcore.model

/**
 * A set of tokens enabling full access to a Google account
 */
data class MasterTokenInfo (
        /** The 'master token'*/
        val token: String,
        override val auth: String,
        override val SID: String?,
        override val LSID: String?,
        override val services: List<String>?,
        override val Email: String?,
        override val firstName: String,
        override val lastName: String,
        override val GooglePlusUpdate: Int?

) : TokenInfoBase