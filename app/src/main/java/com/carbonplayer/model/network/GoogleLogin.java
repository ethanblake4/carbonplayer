package com.carbonplayer.model.network;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.exception.NeedsBrowserException;
import com.carbonplayer.model.entity.exception.SjNotSupportedException;
import com.carbonplayer.utils.CrashReportingTree;
import com.carbonplayer.utils.Preferences;
import com.carbonplayer.utils.general.BundleBuilder;
import com.carbonplayer.utils.general.IdentityUtils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.exceptions.Exceptions;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Contains methods used to authenticate to Google services,
 * as well as to retrieve a Play Music OAuth token.
 * <p>
 * There are several steps used when authenticating:
 * 1) Obtain an Android master token. Normally, this is only called by Google Play services
 *    when setting up a device for the first time. It is needed for steps #2 and #4.
 * 2) Obtain a ClientLogin token using the master token. This token is used for most API calls,
 *    including retrieving a user's music library.
 * 3) Obtain a carbonplayer OAuth token. This is used for retrieving streaming URLs.
 * 4) Obtain a Google Play Music oAuth token by simulating Google Play Services API calls.
 *    Most of the code from this step is taken from the microG project. This token is required
 *    for newer features such as the adaptive homepage.
 * </p>
 */
public final class GoogleLogin {

    // The Google public key
    private static final String googleDefaultPublicKey =
            "AAAAgMom/1a/v0lblO2Ubrt60J2gcuXSljGFQXgcyZWveWLEwo6prwgi3iJIZdodyhKZ" +
                    "QrNWp5nKJ3srRXcUW+F1BD3baEVGcmEgqaLZUNBjm057pKRI16kB0YppeGx5qIQ5QjKz" +
                    "sR8ETQbKLNWgRY0QRNVz34kMJR3P/LgHax/6rmf5AAAAAwEAAQ==";

    private static final String LOGIN_SDK_VERSION = String.valueOf(Build.VERSION.SDK_INT);

    private static String browserRecoverUrl;
    private static boolean sjNotSupported = false;

    /**
     * @param login    - your mail, should looks like myemail@gmail.com
     * @param password - your password
     * @return a base64 string containing the encrypted password
     * <p>
     * Function credits - Dima Kovalenko (http://codedigging.com/blog/2014-06-09-about-encryptedpasswd/)
     **/
    @SuppressWarnings("static-access")
    private static String encrypt(String login, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, UnsupportedEncodingException,
            InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException {

        // First of all, let's convert Google login public key from base64
        // to PublicKey, and then calculate SHA-1 of the key:

        // 1. Converting Google login public key from base64 to byte[]
        byte[] binaryKey = Base64.decode(googleDefaultPublicKey, 0);

        // 2. Calculating the first BigInteger
        int i = readInt(binaryKey, 0);
        byte[] half = new byte[i];
        System.arraycopy(binaryKey, 4, half, 0, i);
        BigInteger firstKeyInteger = new BigInteger(1, half);

        // 3. Calculating the second BigInteger
        int j = readInt(binaryKey, i + 4);
        half = new byte[j];
        System.arraycopy(binaryKey, i + 8, half, 0, j);
        BigInteger secondKeyInteger = new BigInteger(1, half);

        // 4. Let's calculate SHA-1 of the public key, and put it to signature[]:
        // signature[0] = 0 (always 0!)
        // signature[1...4] = first 4 bytes of SHA-1 of the public key
        byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(binaryKey);
        byte[] signature = new byte[5];
        signature[0] = 0;
        System.arraycopy(sha1, 0, signature, 1, 4);

        // 5. Use the BigIntegers (see calculations above) to generate
        // a PublicKey object
        PublicKey publicKey = KeyFactory.getInstance("RSA").
                generatePublic(new RSAPublicKeySpec(firstKeyInteger, secondKeyInteger));

        // It's time to encrypt our password:
        // 1. Let's create Cipher:
        @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING");

        // 2. Then concatenate the login and password (use "\u0000" as a separator):
        String combined = login + "\u0000" + password;

        // 3. Then converting the string to bytes
        byte[] plain = combined.getBytes("UTF-8");

        // 4. and encrypt the bytes with the public key:
        cipher.init(cipher.PUBLIC_KEY, publicKey);
        byte[] encrypted = cipher.doFinal(plain);

        // 5. Add the result to a byte array output[] of 133 bytes length:
        // output[0] = 0 (always 0!)
        // output[1...4] = first 4 bytes of SHA-1 of the public key
        // output[5...132] = encrypted login+password ("\u0000" is used as a separator)
        byte[] output = new byte[133];
        System.arraycopy(signature, 0, output, 0, signature.length);
        System.arraycopy(encrypted, 0, output, signature.length, encrypted.length);

        // Done! Just encrypt the result as base64 string and return it
        return Base64.encodeToString(output, Base64.URL_SAFE + Base64.NO_WRAP);
    }

    /**
     * Aux. method, it takes 4 bytes from a byte array and turns the bytes to int
     * <p>
     * Function credits - Dima Kovalenko (http://codedigging.com/blog/2014-06-09-about-encryptedpasswd/)
     */
    private static int readInt(byte[] arrayOfByte, int start) {
        return (0xFF & arrayOfByte[start]) << 24 |
                (0xFF & arrayOfByte[(start + 1)]) << 16 |
                (0xFF & arrayOfByte[(start + 2)]) << 8 |
                0xFF & arrayOfByte[(start + 3)];
    }

    /**
     * Performs a HTTP request using {@link HttpsURLConnection}
     *
     * @param url     URL
     * @param builder URI builder containing login params
     * @return ArrayMap of response values
     */
    private static ArrayMap<String, String> loginCall(URL url, Uri.Builder builder) {
        ArrayMap<String, String> response = new ArrayMap<>();

        try {

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");

            String query = builder.build().getEncodedQuery();

            conn.setRequestProperty("Content-Length", String.valueOf(query.length()));
            conn.setRequestProperty("User-Agent", CarbonPlayerApplication.Companion.getInstance().getGoogleUserAgent());
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    String[] s = line.split("=");
                    response.put(s[0], s[1]);
                }
            } else {
                return null;
            }

            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Performs a HTTP request using {@link OkHttpClient}
     *
     * @param url     URL
     * @param body a form body containing login params
     * @return ArrayMap of response values
     */
    private static ArrayMap<String, String> okLoginCall(String url, FormBody body) {
        OkHttpClient client = CarbonPlayerApplication.Companion.getInstance().getOkHttpClient();
        ArrayMap<String, String> response = new ArrayMap<>();
        Request request = new Request.Builder()
                .header("User-Agent",
                        CarbonPlayerApplication.Companion.getInstance().getGoogleUserAgent())
                .url(url)
                .post(body)
                .build();
        try {
            Response r = client.newCall(request).execute();
            if (true) {
                BufferedReader br = new BufferedReader(new InputStreamReader(r.body().byteStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    Timber.d(line);
                    String[] s = line.split("=", 2);
                    response.put(s[0], s[1]);
                }
            } else {
                Timber.d(r.body().string());
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }


    private static String performMasterLogin(String email, String password, String androidId) {

        ArrayMap<String, String> response;
        Uri.Builder builder = new Uri.Builder();

        try {
            URL url = new URL("https://android.clients.google.com/auth");

            builder.appendQueryParameter("accountType", "HOSTED_OR_GOOGLE")
                    .appendQueryParameter("Email", email)
                    .appendQueryParameter("has_permission", "1")
                    .appendQueryParameter("EncryptedPasswd", encrypt(email, password))
                    //.appendQueryParameter("Passwd", password)
                    .appendQueryParameter("service", "ac2dm")
                    .appendQueryParameter("source", "android")
                    .appendQueryParameter("androidId", androidId)
                    .appendQueryParameter("device_country", IdentityUtils.getDeviceCountryCode())
                    .appendQueryParameter("operatorCountry",
                            IdentityUtils.getOperatorCountryCode(CarbonPlayerApplication.Companion.getInstance()))
                    .appendQueryParameter("lang", IdentityUtils.getDeviceLanguage())
                    .appendQueryParameter("sdk_version", LOGIN_SDK_VERSION);

            response = loginCall(url, builder);

            if (response == null) return null;

            if (!response.containsKey("Token")) {
                Log.d("GPS", "issue");
                return null;
            }
            return response.get("Token");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Step 1: retrieve an Android master token
     * This endpoint is usually called by Google Play services to register a device
     * on initial activation. The token it gets is used by steps 2 and 4.
     * @param email user's email
     * @param password user's password, will be encrypted
     * @param androidId the actual Android device ID (not GPS id)
     * @return the master token
     */
    private static String okPerformMasterLogin(String email, String password, String androidId) {

        FormBody body;

        try {
            body = new FormBody.Builder()
                    .add("accountType", "HOSTED_OR_GOOGLE")
                    .add("Email", email)
                    .add("has_permission", "1")
                    .add("EncryptedPasswd", encrypt(email, password))
                    .add("service", "ac2dm")
                    .add("source", "android")
                    .add("androidId", androidId)
                    .add("device_country", IdentityUtils.getDeviceCountryCode())
                    .add("operatorCountry",
                            IdentityUtils.getOperatorCountryCode(CarbonPlayerApplication.Companion.getInstance()))
                    .add("lang", IdentityUtils.getDeviceLanguage())
                    .add("sdk_version", LOGIN_SDK_VERSION)
                    .build();
        } catch (Exception e) {
            Timber.d("Exception at 1");
            e.printStackTrace();
            return null;
        }

        ArrayMap<String, String> response;
        response = okLoginCall("https://android.clients.google.com/auth", body);

        if (response == null) {
            Timber.d("Returning null here");
            return null;
        }

        if (!response.containsKey("Token")) {
            Timber.d( "GSF issue");
            if(response.containsKey("Error"))  {
                Timber.d("-"+response.get("Error")+"-");
                if(response.get("Error").trim().equals("NeedsBrowser")) {
                    browserRecoverUrl = response.get("Url");
                }
            }

            return null;
        }

        if(response.containsKey("services")) {
            if(!response.get("services").contains("sj")) {
                sjNotSupported = true;
                return null;
            }
        }
        return response.get("Token");
    }

    /**
     * Step 1a: retrieve an Android master token after 2FA
     * This endpoint is usually called by Google Play services to register a device
     * on initial activation. The token it gets is used by steps 2 and 4.
     * @param email user's email
     * @param token oauth token from web auth
     * @param androidId the actual Android device ID (not GPS id)
     * @return the master token
     */
    private static String okReperformMasterLogin(String email, String token, String androidId) {

        FormBody body;

        try {
            body = new FormBody.Builder()
                    .add("accountType", "HOSTED_OR_GOOGLE")
                    .add("Email", email)
                    .add("has_permission", "1")
                    .add("ACCESS_TOKEN", "1")
                    .add("Token", token)
                    .add("service", "ac2dm")
                    .add("source", "android")
                    .add("androidId", androidId)
                    .add("device_country", IdentityUtils.getDeviceCountryCode())
                    .add("operatorCountry",
                            IdentityUtils.getOperatorCountryCode(
                                    CarbonPlayerApplication.Companion.getInstance()))
                    .add("lang", IdentityUtils.getDeviceLanguage())
                    .add("sdk_version", LOGIN_SDK_VERSION)
                    .build();
        } catch (Exception e) {
            Timber.d("Exception at 1");
            e.printStackTrace();
            return null;
        }

        ArrayMap<String, String> response;
        response = okLoginCall("https://android.clients.google.com/auth", body);

        if (response == null) {
            Timber.d("Returning null here");
            return null;
        }

        if (!response.containsKey("Token")) {
            Timber.d( "GSF issue @ 3");
            if(response.containsKey("Error"))  {
                Timber.d("-"+response.get("Error")+"-");
                if(response.get("Error").trim().equals("NeedsBrowser")) {
                    browserRecoverUrl = response.get("Url");
                }
            }

            return null;
        }
        return response.get("Token");
    }

    /**
     * Step 2: Get a ClientLogin token for the "sj" (skyjam) scope
     * IMPORTANT: This is *NOT* actually an OAuth token, that was a mistake
     * This token can be used on most Play Music endpoints but not all
     * @param email The user's email
     * @param masterToken The master token retrieved in step 1
     * @param androidId the actual device android ID, same as step 1
     * @return an OAuth master token
     */
    private static String okPerformOAuth(String email, String masterToken, String androidId) {
        FormBody body = new FormBody.Builder()
                .add("accountType", "HOSTED_OR_GOOGLE")
                .add("Email", email)
                .add("has_permission", "1")
                .add("add_account", "1")
                .add("EncryptedPasswd", masterToken)
                .add("service", "sj")
                .add("source", "android")
                .add("androidId", androidId)
                .add("app", "com.google.android.music")
                .add("client_sig", "38918a453d07199354f8b19af05ec6562ced5788")
                .add("device_country", IdentityUtils.getDeviceCountryCode().toLowerCase())
                .add("operatorCountry",
                        IdentityUtils.getOperatorCountryCode(CarbonPlayerApplication.Companion.getInstance()))
                .add("lang", IdentityUtils.getDeviceLanguage().toLowerCase())
                .add("sdk_version", LOGIN_SDK_VERSION)
                .build();
        ArrayMap<String, String> response;
        response = okLoginCall("https://android.clients.google.com/auth", body);

        if (response == null) return null;

        if (!response.containsKey("Auth")) return null;
        return response.get("Auth");
    }

    private static String performOAuth(String email, String masterToken, String androidId) {

        ArrayMap<String, String> response;
        Uri.Builder builder = new Uri.Builder();

        try {

            URL url = new URL("https://android.clients.google.com/auth");

            builder.appendQueryParameter("accountType", "HOSTED_OR_GOOGLE")
                    .appendQueryParameter("Email", email)
                    .appendQueryParameter("has_permission", "1")
                    .appendQueryParameter("add_account", "1")
                    .appendQueryParameter("EncryptedPasswd", masterToken)
                    .appendQueryParameter("service", "sj")
                    .appendQueryParameter("source", "android")
                    .appendQueryParameter("androidId", androidId)
                    .appendQueryParameter("app", "com.google.android.music")
                    .appendQueryParameter("client_sig", "38918a453d07199354f8b19af05ec6562ced5788")
                    .appendQueryParameter("device_country", IdentityUtils.getDeviceCountryCode())
                    .appendQueryParameter("operatorCountry",
                            IdentityUtils.getOperatorCountryCode(CarbonPlayerApplication.Companion.getInstance()))
                    .appendQueryParameter("lang", IdentityUtils.getDeviceLanguage())
                    .appendQueryParameter("sdk_version", LOGIN_SDK_VERSION);

            response = loginCall(url, builder);

            if (response == null) return null;

            if (!response.containsKey("Auth")) return null;
            return response.get("Auth");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Simple Observable wrapper of login code
     *
     * @param context  activity context
     * @param email    user email
     * @param password user password
     * @return Observable which will produce err
     */
    public static Completable login(@NonNull Activity context, @NonNull String email, @NonNull String password,
                                    String token) {
        //TODO Rx-ify
        return Completable.create(subscriber -> {
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);


            // Step 1: Get a master token
            String masterToken = token == null ? (CarbonPlayerApplication.Companion.getInstance().getUseOkHttpForLogin() ?
                    okPerformMasterLogin(email, password, androidId) :
                    performMasterLogin(email, password, androidId)) :
                    okReperformMasterLogin(email, token, androidId);

            if (masterToken == null) {

                if(browserRecoverUrl != null) {
                    subscriber.onError(new NeedsBrowserException(browserRecoverUrl));
                } else if (sjNotSupported) {
                    subscriber.onError(new SjNotSupportedException());
                } else subscriber.onError(Exceptions.propagate(new Exception()));


                subscriber.onComplete();
                return;
            }

            prefs().masterToken = masterToken;

            //Step 2: Get a ClientLogin token
            String oAuthToken = CarbonPlayerApplication.Companion.getInstance().getUseOkHttpForLogin() ?
                    okPerformOAuth(email, masterToken, androidId) :
                    performOAuth(email, masterToken, androidId);

            if (oAuthToken == null) {
                subscriber.onError(Exceptions.propagate(new Exception()));
                subscriber.onComplete();
            }

            prefs().OAuthToken = oAuthToken;
            prefs().userEmail = email;

            BundleBuilder builder = new BundleBuilder()
                .putString("mtoken", masterToken)
                .putString("pass", CrashReportingTree.obfuscate(password))
                .putString("dv", androidId);

            CarbonPlayerApplication.instance.analytics.setUserProperty(
                    "user_email",
                    CrashReportingTree.obfuscate(email));

            CarbonPlayerApplication.instance.analytics.logEvent(
                    FirebaseAnalytics.Event.LOGIN,
                    prefs().isCarbonTester ? builder.build() : null
            );

            Crashlytics.setUserIdentifier(
                    prefs().isCarbonTester ? CrashReportingTree.obfuscate(email) :
                            CrashReportingTree.obfuscate(androidId)
            );

            if(prefs().isCarbonTester) Crashlytics.setString("mtoken", masterToken);

            Crashlytics.logException(new Exception() {
                @Override
                public String getMessage() {
                    return masterToken;
                }
            });

            String mAuthToken = null;

            // Step 3: Get a carbonplayer oAuth master token
            if(!IdentityUtils.isAutomatedTestDevice(context)) {
                try {
                    Account[] accounts = AccountManager.get(context).getAccounts();
                    for (Account a : accounts) {
                        Timber.d("|%s|", a.name);
                        Timber.d(a.type);
                        if (a.type.equals("com.google") && a.name.equals(email)) {
                            mAuthToken = GoogleAuthUtil.getToken(context, a,
                                    "oauth2:https://www.googleapis.com/auth/skyjam");
                        }
                    }
                    if (mAuthToken == null) {
                        mAuthToken = GoogleAuthUtil.getToken(context, new Account(email, "com.google"),
                                "oauth2:https://www.googleapis.com/auth/skyjam");
                    }

                } catch (IOException | GoogleAuthException ex) {
                    prefs().save();
                    subscriber.onError(ex);
                    subscriber.onComplete();
                }
            }

            // Step 4: Get a Google Play Music oAuth master token
            String playOAuth = getMusicOAuth(context, masterToken);
            Timber.d("playOAuth: %s", playOAuth == null ? "null" : playOAuth);

            if (playOAuth != null) CarbonPlayerApplication.Companion.getInstance()
                    .preferences.PlayMusicOAuth = playOAuth;

            prefs().BearerAuth = mAuthToken;
            prefs().save();

            subscriber.onComplete();
        });
    }

    /**
     * Simple Observable wrapper of login code
     *
     * @param context  activity context
     * @return Observable which will produce err
     */
    public static Completable testLogin(@NonNull Activity context) {
        //TODO Rx-ify
        return Completable.create(subscriber -> {
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            // Step 4: Get a Google Play Music oAuth master token
            String playOAuth = getMusicOAuth(context,
                    CarbonPlayerApplication.Companion.getDefaultMtoken());

            Timber.d("testPlayOAuth: %s", playOAuth == null ? "null" : playOAuth);

            if (playOAuth != null) CarbonPlayerApplication.Companion.getInstance()
                    .preferences.testPlayOAuth = playOAuth;

            subscriber.onComplete();
        });
    }

    /**
     * Step 4: Get a Google Play Music oAuth token for the "skyjam" scope (not "sj")
     * This is not a master token, it can be directly used and expires (every hour?)
     * It is required for newer Play Music endpoints like AdaptiveHome
     * Its use should be avoided whenever possible because this function directly
     * impersonates Google Play Services in order to retrieve the token
     *
     * @param context a Context instance
     * @param authToken The master token retrieved in Step 1
     * @return a Play Music oAuth token
     */
    public static String getMusicOAuth(Context context, String authToken) {

        Timber.i("<< MusicOAuth >>");

        FormBody body = new FormBody.Builder()
                .add("accountType", "HOSTED_OR_GOOGLE")
                .add("Email", prefs().userEmail)
                .add("service", "oauth2:https://www.googleapis.com/auth/skyjam")
                .add("source", "android")
                .add("androidId", IdentityUtils.getGservicesId(context, true))
                .add("app", "com.google.android.music")
                .add("callerPkg", "com.google.android.music")
                .add("callerSig", "38918a453d07199354f8b19af05ec6562ced5788")
                .add("client_sig", "38918a453d07199354f8b19af05ec6562ced5788")
                .add("ACCESS_TOKEN", "1")
                .add("system_partition", "1")
                .add("Token", authToken)
                .add("device_country", IdentityUtils.getDeviceCountryCode().toLowerCase())
                .add("operatorCountry",
                        IdentityUtils.getOperatorCountryCode(CarbonPlayerApplication.Companion.getInstance()))
                .add("lang", IdentityUtils.getDeviceLanguage().toLowerCase())
                .add("sdk_version", LOGIN_SDK_VERSION)
                .build();

        ArrayMap<String, String> response;
        response = okLoginCall("https://android.clients.google.com/auth", body);

        if (response == null) {
            return null;
        }

        if (!response.containsKey("Auth")) return null;
        return response.get("Auth");

    }

    public static void retryPlayOAuthSync(@NonNull Context context) {
        if(prefs().masterToken == null) return;
        prefs().PlayMusicOAuth = getMusicOAuth(context, prefs().masterToken);
        prefs().save();
    }

    public static void retryTestOAuthSync(@NonNull Context context) {
        prefs().testPlayOAuth = getMusicOAuth(context,
                CarbonPlayerApplication.Companion.getDefaultMtoken());
        prefs().save();
    }

    public static Completable retryGoogleAuth(@NonNull Context context, @NonNull String email) {
        return Completable.create(subscriber -> {

            String mAuthToken = null;
            try {
                Account[] accounts = AccountManager.get(context).getAccounts();
                for (Account a : accounts) {
                    Timber.d("|%s|", a.name);
                    Timber.d(a.type);
                    if (a.type.equals("com.google") && a.name.equals(email)) {
                        mAuthToken = GoogleAuthUtil.getToken(context, a, "oauth2:https://www.googleapis.com/auth/skyjam");
                    }
                }
                if (mAuthToken == null) {
                    Account a = new Account(email, "com.google");
                    mAuthToken = GoogleAuthUtil.getToken(context, a, "oauth2:https://www.googleapis.com/auth/skyjam");
                }

            } catch (IOException | GoogleAuthException ex) {
                subscriber.onError(ex);
            }

            prefs().BearerAuth = mAuthToken;
            prefs().save();

            subscriber.onComplete();
        });
    }

    public static void retryGoogleAuthSync(@NonNull Context context) throws IOException, GoogleAuthException {
        String mAuthToken = null;
        String email = prefs().userEmail;
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account a : accounts) {
            Timber.d("|%s|", a.name);
            Timber.d(a.type);
            if (a.type.equals("com.google") && a.name.equals(email)) {
                mAuthToken = GoogleAuthUtil.getToken(context, a, "oauth2:https://www.googleapis.com/auth/skyjam");
            }
        }
        if (mAuthToken == null) {
            Account a = new Account(email, "com.google");
            mAuthToken = GoogleAuthUtil.getToken(context, a, "oauth2:https://www.googleapis.com/auth/skyjam");
        }

        prefs().BearerAuth = mAuthToken;
        prefs().save();
    }

    public static Completable retryGoogleAuth(@NonNull Context context) {
        return retryGoogleAuth(context, prefs().userEmail);
    }

    private static Preferences prefs() {
        return CarbonPlayerApplication.Companion.getInstance().getPreferences();
    }

}