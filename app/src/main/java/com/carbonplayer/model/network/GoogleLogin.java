package com.carbonplayer.model.network;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.primitive.Null;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.Buffer;
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

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import timber.log.Timber;

/**
 * Contains methods used to authenticate to Google services,
 * as well as to retrieve a Play Music OAuth token.
 *
 * TODO update to use OkHTTP
 */
public final class GoogleLogin {

    // The Google public key
    private static final String googleDefaultPublicKey =
            "AAAAgMom/1a/v0lblO2Ubrt60J2gcuXSljGFQXgcyZWveWLEwo6prwgi3iJIZdodyhKZ" +
            "QrNWp5nKJ3srRXcUW+F1BD3baEVGcmEgqaLZUNBjm057pKRI16kB0YppeGx5qIQ5QjKz" +
            "sR8ETQbKLNWgRY0QRNVz34kMJR3P/LgHax/6rmf5AAAAAwEAAQ==";

    private static final String LOGIN_SDK_VERSION = "17";

    /**
     * @param login - your mail, should looks like myemail@gmail.com
     * @param password - your password
     *
     * @return a base64 string containing the encrypted password
     *
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
        byte [] half = new byte[i];
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
        byte[] output = new byte [133];
        System.arraycopy(signature, 0, output, 0, signature.length);
        System.arraycopy(encrypted, 0, output, signature.length, encrypted.length);

        // Done! Just encrypt the result as base64 string and return it
        return Base64.encodeToString(output, Base64.URL_SAFE + Base64.NO_WRAP);
    }

   /**
    * Aux. method, it takes 4 bytes from a byte array and turns the bytes to int
    *
    * Function credits - Dima Kovalenko (http://codedigging.com/blog/2014-06-09-about-encryptedpasswd/)
    */
    private static int readInt(byte[] arrayOfByte, int start) {
        //return 0x0 | (0xFF & arrayOfByte[start]) << 24 | (0xFF & arrayOfByte[(start + 1)]) << 16 | (0xFF & arrayOfByte[(start + 2)]) << 8 | 0xFF & arrayOfByte[(start + 3)];
        return (0xFF & arrayOfByte[start]) << 24 | (0xFF & arrayOfByte[(start + 1)]) << 16 | (0xFF & arrayOfByte[(start + 2)]) << 8 | 0xFF & arrayOfByte[(start + 3)];
    }

    /**
     * Performs a Google login call
     * @param url URL
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
            conn.setRequestProperty("User-Agent", CarbonPlayerApplication.googleUserAgent);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    String[] s = line.split("=");
                    response.put(s[0], s[1]);
                }
            }else{
                return null;
            }

            conn.connect();
        }catch (IOException e){
            e.printStackTrace();
        }

        return response;
    }

    private static ArrayMap<String, String> okLoginCall(String url, FormBody body){
        OkHttpClient client = CarbonPlayerApplication.getOkHttpClient();
        ArrayMap<String, String> response = new ArrayMap<>();
        Request request = new Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.googleUserAgent)
                .url(url)
                .post(body)
                .build();
        try {
            Response r = client.newCall(request).execute();
            if(r.isSuccessful()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(r.body().byteStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] s = line.split("=");
                    response.put(s[0], s[1]);
                }
            } else {
                return null;
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return response;
    }

    private static String performMasterLogin(String email, String password, String androidId){

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
                    .appendQueryParameter("device_country", "us")
                    .appendQueryParameter("operatorCountry", "us")
                    .appendQueryParameter("lang", "en")
                    .appendQueryParameter("sdk_version", LOGIN_SDK_VERSION);

            response = loginCall(url, builder);

            if(response == null) return null;

            if(!response.containsKey("Token")) {Log.d("GPS", "issue"); return null; }
            return response.get("Token");

        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    private static String okPerformMasterLogin(String email, String password, String androidId){

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
                    .add("device_country", "us")
                    .add("operatorCountry", "us")
                    .add("lang", "en")
                    .add("sdk_version", LOGIN_SDK_VERSION)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        ArrayMap<String, String> response;
        response = okLoginCall("https://android.clients.google.com/auth", body);

        if(response == null) return null;

        if(!response.containsKey("Token")) {Log.d("GPS", "issue"); return null; }
        return response.get("Token");
    }

    private static String okPerformOAuth(String email, String masterToken, String androidId){
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
                .add("device_country", "us")
                .add("operatorCountry", "us")
                .add("lang", "en")
                .add("sdk_version", LOGIN_SDK_VERSION)
                .build();
        ArrayMap<String, String> response;
        response = okLoginCall("https://android.clients.google.com/auth", body);

        if(response == null) return null;

        if(!response.containsKey("Auth")) return null;
        return response.get("Auth");
    }

    private static String performOAuth(String email, String masterToken, String androidId){

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
                    .appendQueryParameter("device_country", "us")
                    .appendQueryParameter("operatorCountry", "us")
                    .appendQueryParameter("lang", "en")
                    .appendQueryParameter("sdk_version", LOGIN_SDK_VERSION);

            response = loginCall(url, builder);

            if(response == null) return null;

            if(!response.containsKey("Auth")) return null;
            return response.get("Auth");

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Simple Observable wrapper of login code
     * @param context activity context
     * @param email user email
     * @param password user password
     * @return Observable which will produce err
     */
    public static Observable<Null> login(@NonNull Activity context, @NonNull String email, @NonNull String password) {
        //TODO Rx-ify
        return Observable.create(subscriber -> {
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context.getBaseContext());

            SharedPreferences.Editor edit = prefs.edit();

            String masterToken = CarbonPlayerApplication.useOkHttpForLogin ?
                    okPerformMasterLogin(email, password, androidId) :
                    performMasterLogin(email, password, androidId);
            if(masterToken == null) {
                subscriber.onError(new Exception());
                subscriber.onCompleted();
            }

            String OAuthToken = CarbonPlayerApplication.useOkHttpForLogin ?
                    okPerformOAuth(email, masterToken, androidId) :
                    performOAuth(email, masterToken, androidId);

            if(OAuthToken == null) {
                subscriber.onError(new Exception());
                subscriber.onCompleted();
            }
            edit.putString("OAuthToken", OAuthToken);

            String mAuthToken = "";
            Account[] accounts = AccountManager.get(context).getAccounts();
            try{
                for(Account a: accounts){
                    Timber.d(a.name);
                    Timber.d(a.type);
                }
                //mAuthToken = GoogleAuthUtil.getToken(context, AccountManager.get(context).getAccounts()[0], "oauth2:https://www.googleapis.com/auth/skyjam");
            } catch (Exception ex) {
                subscriber.onError(ex);
            }

            try {
                mAuthToken = GoogleAuthUtil.getToken(context, email /*account*/, "oauth2:https://www.googleapis.com/auth/skyjam");
            } catch (IOException | GoogleAuthException ex) {
                edit.apply();
                subscriber.onError(ex);
                subscriber.onCompleted();
            }

            edit.putString("BearerAuth", mAuthToken);
            edit.apply();

            subscriber.onCompleted();
        });

    }

    public static Observable<Null> retryGoogleAuth(@NonNull Activity context, @NonNull String email){
        return Observable.create(subscriber -> {

            String mAuthToken = "";
            try {
                Account[] accounts = AccountManager.get(context).getAccounts();
                for(Account a: accounts){
                    Timber.d(a.name);
                    Timber.d(a.type);
                }
                mAuthToken = GoogleAuthUtil.getToken(context, AccountManager.get(context).getAccounts()[0], "oauth2:https://www.googleapis.com/auth/skyjam");
            } catch (IOException | GoogleAuthException ex) {
                subscriber.onError(ex);
            }

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context.getBaseContext());

            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("BearerAuth", mAuthToken);
            edit.apply();

            subscriber.onCompleted();
        });
    }

}