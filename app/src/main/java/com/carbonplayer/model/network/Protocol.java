package com.carbonplayer.model.network;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.ConfigEntry;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.exception.ResponseCodeException;
import com.carbonplayer.utils.IdentityUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import timber.log.Timber;

/**
 * Contains methods for interacting with Google Play Music.
 */
public class Protocol {

    private static final String SJ_URL = "https://mclients.googleapis.com/sj/v2.4/";
    private static final MediaType TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RESULTS = 250;

    public class Call {

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
                            String deviceId, RequestBody requestBody) throws IOException, ResponseCodeException {

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

        private String perform(URL url, Map<String,String> headers, Map<String,String> params, String skyjamToken,
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

    public static Observable<LinkedList<ConfigEntry>> getConfig(@NonNull final Activity context){
        final OkHttpClient client = new OkHttpClient();
        final Uri.Builder getParams = new Uri.Builder()
                .appendQueryParameter("dv", "0")
                .appendQueryParameter("hl", IdentityUtils.localeCode());

        return Observable.create(subscriber -> {
            Request request = defaultBuilder(context)
                    .url(SJ_URL + "config?" + getParams.build().getEncodedQuery())
                    .build();
            try {
                Response r = client.newCall(request).execute();
                if (!r.isSuccessful()) subscriber.onError(new ResponseCodeException());

                String sR = r.body().string();
                JSONObject j = new JSONObject(sR);

                LinkedList<ConfigEntry> itemList = new LinkedList<>();
                JSONArray itemArray = j.getJSONObject("data").getJSONArray("entries");
                for(int i = 0; i<itemArray.length();i++)
                    itemList.add(new ConfigEntry(itemArray.getJSONObject(i)));

                subscriber.onNext(itemList);
            } catch (IOException | JSONException e) {
                subscriber.onError(e);
            }
            subscriber.onCompleted();
        });
    }

    public static Observable<LinkedList<MusicTrack>> listTracks(@NonNull final Activity context){
        final OkHttpClient client = new OkHttpClient();
        final Uri.Builder getParams = new Uri.Builder()
                .appendQueryParameter("alt", "json")
                .appendQueryParameter("hl", IdentityUtils.localeCode())
                .appendQueryParameter("tier", "aa");

        return Observable.create(subscriber -> {
            String startToken = "";
            while(startToken != null) {
                Timber.d("startToken: %s", startToken);
                JSONObject requestJson = new JSONObject();
                try {
                    requestJson.put("max-results", MAX_RESULTS);
                    if(!"".equals(startToken)) requestJson.put("start-token", startToken);
                } catch (JSONException e) {
                    subscriber.onError(e);
                }
                startToken = null;

                Request request = defaultBuilder(context)
                        .url(SJ_URL + "trackfeed?" + getParams.build().getEncodedQuery())
                        .header("Content-Type", "application/json")
                        .post( RequestBody.create(TYPE_JSON, requestJson.toString()) )
                        .build();
                try {
                    Response r = client.newCall(request).execute();
                    if (!r.isSuccessful()) subscriber.onError(new ResponseCodeException());

                    JSONObject j = new JSONObject(r.body().string());
                    if(j.has("nextPageToken")) startToken = j.getString("nextPageToken");

                    LinkedList<MusicTrack> list = new LinkedList<>();
                    JSONArray itemArray = j.getJSONObject("data").getJSONArray("items");
                    for(int i = 0; i<itemArray.length();i++) {
                        list.add(new MusicTrack(itemArray.getJSONObject(i)));
                    }
                    subscriber.onNext(list);

                } catch (IOException | JSONException e) {
                    subscriber.onError(e);
                }
            }
            subscriber.onCompleted();
        });
    }

    private static String getSkyjamToken(Activity context){
        return PreferenceManager.getDefaultSharedPreferences(context.getBaseContext())
                .getString("OAuthToken", "");
    }

    private static Request.Builder defaultBuilder(Activity context){
        return new Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.googleUserAgent)
                .header("Authorization", "GoogleLogin auth=" + getSkyjamToken(context))
                .header("X-Device-ID", IdentityUtils.deviceId(context));
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


