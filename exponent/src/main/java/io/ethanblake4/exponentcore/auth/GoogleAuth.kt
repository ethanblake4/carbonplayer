package io.ethanblake4.exponentcore.auth

import android.util.ArrayMap
import android.util.Log
import androidx.annotation.Keep
import io.ethanblake4.exponentcore.Exponent
import io.ethanblake4.exponentcore.Logging
import io.ethanblake4.exponentcore.model.ClientTokenInfo
import io.ethanblake4.exponentcore.model.GoogleAuthInfo
import io.ethanblake4.exponentcore.model.MasterTokenInfo
import io.ethanblake4.exponentcore.model.error.AuthException
import io.ethanblake4.exponentcore.model.error.NeedsBrowserException
import okhttp3.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.GeneralSecurityException

@Keep
object GoogleAuth {

    private val AUTH_URL = "https://android.clients.google.com/auth"

    /**
     * Performs a synchronous login HTTP request using [OkHttpClient]
     *
     * @param url     URL
     * @param body a form body containing login params
     * @return ArrayMap of response values
     * @throws IOException if a network error occurs
     */
    @Throws(IOException::class)
    private fun okLoginCall(url: String, body: FormBody): Map<String, String>? {
        val request = Request.Builder()
                .header("User-Agent", Exponent.loginUserAgent)
                .url(url).post(body).build()
        val r = Exponent.client.newCall(request).execute()
        r.body()?.let {
            return parseAuthResponse(it.byteStream())
        }
        Logging.e(r.code().toString() + " returned by login call 1, no response body so returning null")
        return null
    }

    /**
     * Performs a synchronous login HTTP request using [OkHttpClient]
     *
     * @param url     URL
     * @param body a form body containing login params
     * @return ArrayMap of response values
     * @throws IOException if a network error occurs
     */
    private fun okLoginCallAsync(url: String, body: FormBody, callback: (Map<String, String>?) -> Unit,
                                 errCallback: (Throwable?) -> Unit) {
        val request = Request.Builder()
                .header("User-Agent", Exponent.loginUserAgent)
                .url(url).post(body).build()

        Exponent.client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call?, e: IOException?) { errCallback(e) }

            override fun onResponse(call: Call?, r: Response?) {
                r?.body()?.let {
                    callback(parseAuthResponse(it.byteStream()))
                }
            }
        })
    }

    /**
     * Parses a response from the AUTH_URL endpoint into key-value pairs
     * @param in an InputStream returned from a call to okLoginCall
     * @return An ArrayMap of response key-value pairs
     * @throws IOException if the InputStream cannot be read
     */
    @Throws(IOException::class)
    private fun parseAuthResponse(`in`: InputStream): ArrayMap<String, String> {
        val response = ArrayMap<String, String>()
        BufferedReader(InputStreamReader(`in`)).useLines { it.forEach { line ->
            Logging.i(line)
            val s = line.split("=".toRegex(), 2).toTypedArray()
            response[s[0]] = s[1]
        } }
        return response
    }

    /**
     * Create a FormBody Builder with base values used in all GPS auth calls
     *
     * @param email The user's email, including domain
     * @param device_id A device ID parameter. This is sometimes the actual Android ID and sometimes
     * the Play services advertising ID.
     * @param service The service to obtain a Token or Auth for
     * @return A builder that can be extended with values for specific calls
     */
    private fun baseFormBuilder(email: String, device_id: String, service: String): FormBody.Builder {
        return FormBody.Builder()
                .add("accountType", "HOSTED_OR_GOOGLE")
                .add("Email", email)
                .add("has_permission", "1")
                .add("source", "android")
                .add("androidId", device_id)
                .add("service", service)
                .add("device_country", Exponent.deviceCountryCode)
                .add("operatorCountry", Exponent.operatorCountryCode)
                .add("lang", Exponent.deviceLanguage)
                .add("sdk_version", Exponent.loginSDKVersion.toString())
    }

    /**
     * Create a FormBody for retrieving a mater token for an app
     * @param email User email
     * @param password User password
     * @return A FormBody used for obtaining master auth tokens
     * @throws IOException If the encoding used is unsupported
     * @throws GeneralSecurityException If password encryption fails
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    private fun createMasterAuthForm(email: String, password: String): FormBody {
        return baseFormBuilder(email, Exponent.androidID, "ac2dm")
                .add("EncryptedPasswd", AuthEncryptor.encrypt(email, password))
                .build()
    }

    /**
     * Create a FormBody for retrieving a mater token for an app after 2FA
     * @param email User email
     * @param token MFA oAuth token
     * @return A FormBody used for obtaining master auth tokens
     */
    private fun createMasterReauthForm(email: String, token: String): FormBody {
        return baseFormBuilder(email, Exponent.androidID, "ac2dm")
                .add("Token", token)
                .add("ACCESS_TOKEN", "1")
                .build()
    }

    /**
     * Create a FormBody for retrieving a ClientLogin auth token for an app
     *
     * @param email User email
     * @param masterToken The master auth token to use
     * @param service The service to obtain a token for (eg. 'sj')
     * @param app The calling app, usually a package name (e.g. 'com.google.android.music')
     * @param sig The Google Play services app signature, or null to use the actual signature
     * @return A FormBody used for obtaining master auth tokens
     */
    private fun createClientLoginForm(email: String, masterToken: String, service: String,
                                      app: String, sig: String? = null): FormBody {
        return baseFormBuilder(email, Exponent.gsfID, service)
                .add("add_account", "1")
                .add("EncryptedPasswd", masterToken)
                .add("app", app)
                .add("client_sig", sig ?: "38918a453d07199354f8b19af05ec6562ced5788")
                .build()
    }

    private fun createOAuthForm(email: String, masterToken: String,
                                service: String, app: String, sig: String?,
                                callerSig: String?, system: Boolean): FormBody {
        return baseFormBuilder(email, Exponent.gsfID, service)
                .add("app", "com.google.android.music")
                .add("callerPkg", app)
                .add("callerSig", callerSig ?: "38918a453d07199354f8b19af05ec6562ced5788")
                .add("client_sig", sig ?: "38918a453d07199354f8b19af05ec6562ced5788")
                .add("ACCESS_TOKEN", "1")
                .add("system_partition", if (system) "1" else "0")
                .add("Token", masterToken)
                .build()
    }

    @Throws(NeedsBrowserException::class, AuthException::class)
    private fun masterAuthInfo(response: Map<String, String>): MasterTokenInfo? {

        var services: List<String>? = null

        if (!response.containsKey("Token")) {
            Logging.w("masterAuthInfo(): Token not found in response")
            if (response.containsKey("Error")) {
                Logging.w("Response contains error: " + response["Error"])
                if (response["Error"]!!.trim { it <= ' ' } == "NeedsBrowser") {
                    throw NeedsBrowserException(response["Url"]!!)
                } else {
                    throw AuthException(response["Error"]!!)
                }
            }
            return null
        }

        if (response.containsKey("services"))
            services = response["services"]!!
                    .split(",").dropLastWhile { it.isEmpty() }.toList()

        var GooglePlusUpdate = -1
        try {
            GooglePlusUpdate = Integer.parseInt(response["GooglePlusUpdate"]!!)
        } catch (e: NumberFormatException) {
            Logging.i("Failed to parse GooglePlusUpdate", e)
        }

        return MasterTokenInfo(
                response["Token"]!!, response["Auth"]!!,
                response["SID"], response["LSID"], services,
                response["Email"],
                response["firstName"] ?: "",
                response["lastName"] ?: "",
                GooglePlusUpdate)
    }

    @Throws(AuthException::class)
    private fun clientOAuthInfo(response: Map<String, String>): ClientTokenInfo? {

        var services: List<String>? = null

        if (!response.containsKey("Token")) {
            Logging.w("clientOAuthInfo(): Token not found in response")
            if (response.containsKey("Error")) {
                Logging.w("Response contains error: " + response["Error"])
                throw AuthException(response["Error"]!!)
            }
            return null
        }

        if (response.containsKey("services"))
            services = response["services"]!!
                    .split(",").dropLastWhile { it.isEmpty() }.toList()

        var GooglePlusUpdate = -1
        try {
            GooglePlusUpdate = Integer.parseInt(response["GooglePlusUpdate"]!!)
        } catch (e: NumberFormatException) {
            Logging.i("Failed to parse GooglePlusUpdate", e)
        }

        return ClientTokenInfo(response["Auth"]!!,
                response["SID"], response["LSID"], services,
                response["Email"],
                response["firstName"]?: "",
                response["lastName"] ?: "",
                GooglePlusUpdate)
    }

    private fun extractAuth(response: Map<String, String>): GoogleAuthInfo {
        var auth = ""
        var issueAdvice: String? = null
        var storeConsentRemotely: String? = null
        var expiry = -1
        if (response.containsKey("Auth")) auth = response["Auth"]!!
        if (response.containsKey("issueAdvice")) issueAdvice = response["issueAdvice"]
        if (response.containsKey("storeConsentRemotely"))
            storeConsentRemotely = response["storeConsentRemotely"]
        if (response.containsKey("Expiry")) expiry = Integer.parseInt(response["Expiry"]!!)
        return GoogleAuthInfo(auth, issueAdvice, expiry, storeConsentRemotely)
    }

    /* -------------- PUBLIC METHODS --------------- */

    /**
     * Retrieve an Android master token synchronously
     * This endpoint is usually called by Google Play services to register a device
     * on initial activation.
     *
     * @param email user's email
     * @param password user's password, will be encrypted
     * @return the master token
     *
     * @throws AuthException if authorization fails
     * @throws NeedsBrowserException if authorization failure can be handled by URL redirect
     * @throws IOException If the encoding used is unsupported
     * @throws GeneralSecurityException If password encryption fails
     */
    @Keep @JvmStatic
    @Throws(NeedsBrowserException::class, AuthException::class, IOException::class, GeneralSecurityException::class)
    fun masterAuthSync(email: String, password: String): MasterTokenInfo? {

        val response = okLoginCall(AUTH_URL, createMasterAuthForm(email, password))

        if (response == null) {
            Logging.d("Null response received in masterAuthSync()")
            return null
        }

        return masterAuthInfo(response)
    }

    /**
     * Retrieve an Android master token asynchronously
     * This endpoint is usually called by Google Play services to register a device
     * on initial activation.
     *
     * @param email user's email
     * @param password user's password, will be encrypted
     * @param onSuccess Callback receiving a [MasterTokenInfo] if the call was successful
     * @param onError Callback receiving a [Throwable] if the call failed
     * The throwable may be a [NeedsBrowserException], [AuthException], other, or null
     */
    @Keep @JvmStatic
    fun masterAuthAsync(email: String, password: String, onSuccess: (MasterTokenInfo?) -> Unit,
                        onError: (Throwable?) -> Unit) {

        okLoginCallAsync(AUTH_URL, createMasterAuthForm(email, password), { response ->
            if(response == null) onError(null)
            else {
                Log.d("EXCORE", response.entries.joinToString { it.key + "=" + it.value })
                try {
                    onSuccess(masterAuthInfo(response))
                } catch (e: Throwable) {
                    onError(e)
                }
            }
        }, { e -> onError(e)})
    }

    /**
     * Retrieve an Android master token asynchronously after 2FA
     * This endpoint is usually called by Google Play services to register a device
     * on initial activation, after 2FA has been succesfully used.
     *
     * @param email user's email
     * @param token 2FA oAuth token
     * @param onSuccess Callback receiving a [MasterTokenInfo] if the call was successful
     * @param onError Callback receiving a [Throwable] if the call failed
     * The throwable may be an [AuthException], other, or null
     */
    @Keep @JvmStatic
    fun masterReauthAsync(email: String, token: String, onSuccess: (MasterTokenInfo?) -> Unit,
                        onError: (Throwable?) -> Unit) {

        okLoginCallAsync(AUTH_URL, createMasterReauthForm(email, token), { response ->
            if(response == null) onError(null)
            else try {
                onSuccess(masterAuthInfo(response))
            } catch (e: Throwable) {
                onError(e)
            }
        }, { e -> onError(e)})
    }

    /**
     * Retrieve a ClientLogin token for a specified service synchronously
     * It is unclear whether these tokens are actually scoped
     *
     * @param email user's email
     * @param masterToken Master Token to use
     * @param service Service to retrieve token for, e.g. 'sj'
     * @param app App to retrieve token for
     *
     * @return the GoogleAuthInfo containing the token as the 'Auth' parameter
     */
    @Keep @JvmStatic
    @Throws(IOException::class)
    fun clientLoginSync(email: String, masterToken: String, service: String, app: String): GoogleAuthInfo? {

        val response = okLoginCall(AUTH_URL,
                createClientLoginForm(email, masterToken, service, app))

        if (response == null) {
            Logging.d("Null response received in clientLoginSync()")
            return null
        }

        return extractAuth(response)
    }

    /**
     * Retrieve a ClientLogin token for a specified service asynchronously
     * It is unclear whether these tokens are actually scoped
     *
     * @param email user's email
     * @param masterToken Master Token to use
     * @param service Service to retrieve token for, e.g. 'sj'
     * @param app App to retrieve token for
     * @param onSuccess Success callback with [GoogleAuthInfo]
     * @param onError Failure callback with [Throwable] or null
     */
    @Keep @JvmStatic
    fun clientLoginAsync(email: String, masterToken: String, service: String, app: String,
                         onSuccess: (GoogleAuthInfo?) -> Unit, onError: (Throwable?) -> Unit) {

        okLoginCallAsync(AUTH_URL, createClientLoginForm(email, masterToken, service, app), { r ->
            if(r == null) onError(null) else onSuccess(extractAuth(r))
        }, { e -> onError(e)})
    }

    /**
     * Retrieve a scoped OAuth token for a specified service synchronously
     *
     * @param email user's email
     * @param masterToken Master Token to use
     * @param service Service to retrieve token for, e.g. 'sj'
     * @param app App to retrieve token for
     * @param callerSig The SHA-1 signature of the app that is requesting the token
     * @param system whether the app is a pre-installed system app
     *
     * @return the ClientTokenInfo containing the token as the 'Auth' parameter
     * @throws AuthException If authorization fails
     */
    @Keep @JvmStatic
    @Throws(IOException::class, AuthException::class)
    fun oAuthSync(email: String, masterToken: String, service: String, app: String,
                  callerSig: String, system: Boolean): ClientTokenInfo? {

        val response = okLoginCall(AUTH_URL,
                createOAuthForm(email, masterToken, service, app, null, callerSig, system))

        if (response == null) {
            Logging.d("Null response received in clientLoginSync()")
            return null
        }

        return clientOAuthInfo(response)
    }

}