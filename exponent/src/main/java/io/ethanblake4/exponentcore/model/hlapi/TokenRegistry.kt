package io.ethanblake4.exponentcore.model.hlapi

import io.ethanblake4.exponentcore.model.ClientTokenInfo
import io.ethanblake4.exponentcore.model.GoogleAuthInfo
import io.ethanblake4.exponentcore.model.MasterTokenInfo

interface TokenRegistry {

    fun getMasterTokenFor(email: String): MasterTokenInfo?
    fun getClientLoginTokenFor(email:String, service: String): GoogleAuthInfo?
    fun getOAuthTokenFor(email: String, service: String, app: String): ClientTokenInfo?

    fun saveMasterTokenFor(email: String, token: MasterTokenInfo)
    fun saveClientLoginTokenFor(email: String, service: String, token: GoogleAuthInfo)
    fun saveOAuthTokenFor(email: String, service: String, app: String, token: ClientTokenInfo)

    fun deleteMasterTokenFor(email: String)
    fun deleteClientLoginTokensFor(email: String)
    fun deleteOAuthTokensFor(email: String)

}