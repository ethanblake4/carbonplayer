package com.carbonplayer.model.network;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.ArrayMap;

import com.carbonplayer.CarbonPlayerApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Contains methods and classes for interacting with Google Play Music. This
 * is the "backbone" of Carbon Player.
 */
public class Protocol {

    private static final String SJ_URL = "https://mclients.googleapis.com/sj/v2.4/";

    private static final MediaType TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    public class Call {

        public class ResponseCodeException extends Exception {

            @SuppressWarnings("unused")
            public ResponseCodeException(){
                super();
            }

            public ResponseCodeException(String message){
                super(message);
            }
        }

        public class ValidationException extends RuntimeException {

            public ValidationException(){
                super();
            }

            @SuppressWarnings("unused")
            public ValidationException(String message){
                super(message);
            }
        }

        protected JSONObject parseResponse(String response, boolean validate) throws JSONException{

            JSONObject json = new JSONObject(response);
            if(validate){
                boolean valid = validateResponse(json);
                if(!valid){
                    throw new ValidationException();
                }
            }

            return json;
        }

        protected boolean validateResponse(JSONObject response){
            return response != null;
        }

        private String http(URL url, Map<String,String> headers, Map<String,String> params, String skyjamToken,
                            String deviceId, RequestBody requestBody) throws IOException, ResponseCodeException{

            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .header("User-Agent", CarbonPlayerApplication.googleUserAgent)
                    .header("Authorization", "GoogleLogin auth="+skyjamToken)
                    .header("X-Device-ID", deviceId);

            if(headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet())
                    builder.header(header.getKey(), header.getValue());
            }

            if(params != null){
                FormBody.Builder form = new FormBody.Builder();
                for (Map.Entry<String, String> param : params.entrySet())
                    form.add(param.getKey(), param.getValue());
                builder.post(form.build());
            }else if(requestBody != null){
                builder.post(requestBody);
            }

            Response response = client.newCall(builder.build()).execute();
            if(!response.isSuccessful()) throw new ResponseCodeException("Unexpected response "+response);

            return response.body().string();

        }

        private String perform(URL url, /*String params,*/ Map<String,String> headers, Map<String,String> params, String skyjamToken,
                             String deviceId, RequestBody requestBody) throws IOException, ResponseCodeException{
            return http(url, headers, params, skyjamToken, deviceId, requestBody);
        }
    }

    public class ConfigCall extends Call{

        @Override
        protected boolean validateResponse(JSONObject response){
            try {
                if (response != null && response.getString("kind").equals("sj#configList"))
                    return true;
            }catch(Exception e){
                e.printStackTrace();
            }
            return false;
        }

        public JSONObject execute(Activity context) throws ResponseCodeException, IOException{

            Uri.Builder params = new Uri.Builder();

            params.appendQueryParameter("dv", "0");
            params.appendQueryParameter("hl", Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry());


            @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context.getBaseContext());

            Timber.d(prefs.getString("OAuthToken", ""));

            try {
                URL url = new URL(SJ_URL + "config?" + params.build().getEncodedQuery());
                return super.parseResponse(super.perform(url, null, null, prefs.getString("OAuthToken", ""), androidId, null), true);
            }catch(MalformedURLException | JSONException e){
                e.printStackTrace();
            }

            return null;

        }
    }

    public class ListTracksCall extends Call {

        @Override
        protected boolean validateResponse(JSONObject response){
            try {
                if (response != null)
                    return true;
            }catch(Exception e){
                e.printStackTrace();
            }
            return false;
        }

        public JSONObject execute(Activity context, String nextPageToken, int maxResults) throws ResponseCodeException, IOException{

            Uri.Builder params = new Uri.Builder();
            params.appendQueryParameter("alt", "json");
            params.appendQueryParameter("hl", Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry());
            params.appendQueryParameter("tier", "aa");

            ArrayMap<String,String> headers = new ArrayMap<>();
            headers.put("Content-Type", "application/json");

            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context.getBaseContext());

            JSONObject requestJson = new JSONObject();

            try {
                if (maxResults != 0) requestJson.put("max-results", maxResults);
                if (nextPageToken != null) requestJson.put("start-token", nextPageToken);
            }catch (JSONException e){
                e.printStackTrace();
            }

            String skyjamToken = prefs.getString("OAuthToken", "");

            try {
                RequestBody body = RequestBody.create(TYPE_JSON, requestJson.toString());
                URL url = new URL(SJ_URL + "trackfeed?"+params.build().getEncodedQuery());
                Timber.d("Posting request: %s", requestJson.toString());
                return super.parseResponse(super.perform(url, headers, null, skyjamToken, androidId, body), true);
            }catch(MalformedURLException | JSONException e){
                e.printStackTrace();
            }

            return null;

        }
    }


}


