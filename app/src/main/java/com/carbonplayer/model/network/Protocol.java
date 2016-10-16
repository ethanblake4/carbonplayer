package com.carbonplayer.model.network;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.ConfigEntry;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.exception.ResponseCodeException;
import com.carbonplayer.utils.Constants;
import com.carbonplayer.utils.IdentityUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedList;

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
public final class Protocol {

    private static final String SJ_URL = "https://mclients.googleapis.com/sj/v2.5/";
    private static final String STREAM_URL = "https://android.clients.google.com/music/mplay";
    private static final MediaType TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RESULTS = 250;

    public static Observable<LinkedList<ConfigEntry>> getConfig(@NonNull final Activity context){
        final OkHttpClient client = new OkHttpClient();
        final Uri.Builder getParams = new Uri.Builder()
                .appendQueryParameter("dv", "0")
                .appendQueryParameter("hl", IdentityUtils.localeCode());

        return Observable.create(subscriber -> {
            Request request = defaultBuilder(context.getBaseContext())
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

                Request request = defaultBuilder(context.getBaseContext())
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

    public static Observable<String> getStreamURL(@NonNull final Context context, String song_id){
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        byte[] _s1 = Base64.decode("VzeC4H4h+T2f0VI180nVX8x+Mb5HiTtGnKgH52Otj8ZCGDz9jRW"+
                "yHb6QXK0JskSiOgzQfwTY5xgLLSdUSreaLVMsVVWfxfa8Rw==", Base64.DEFAULT);
        byte[] _s2 = Base64.decode("ZAPnhUkYwQ6y5DdQxWThbvhJHN8msQ1rqJw0ggKdufQjelrKuiG" +
                "GJI30aswkgCWTDyHkTGK9ynlqTkJ5L4CiGGUabGeo8M6JTQ==", Base64.DEFAULT);
        StringBuilder keyBuilder = new StringBuilder();
        for (int i=0;i<_s1.length;i++) keyBuilder.append(_s1[i] ^ _s2[i]);

        String key = keyBuilder.toString();

        return Observable.create(subscriber -> {
            String salt = String.valueOf(new Date().getTime());
            String digest = "";
            try {
                MessageDigest m = MessageDigest.getInstance("SHA-1");
                m.update(salt.getBytes("UTF-8"));
                digest = new String(Base64.encode(m.digest(song_id.getBytes("UTF-8")), Base64.URL_SAFE));
            }catch(NoSuchAlgorithmException | UnsupportedEncodingException e){
                subscriber.onError(e);
                subscriber.onCompleted();
            }
            final Uri.Builder getParams = new Uri.Builder()
                    .appendQueryParameter("opt", "360")
                    .appendQueryParameter("net", "mob")
                    .appendQueryParameter("pt", "e")
                    .appendQueryParameter("slt", salt)
                    .appendQueryParameter("sig", digest);

            if(song_id.startsWith("T")) getParams.appendQueryParameter("mjck", song_id);
            else getParams.appendQueryParameter("songid", song_id);

            Request request = defaultBuilder(context)
                    .url(STREAM_URL + "?" + getParams.build().getEncodedQuery())
                    .build();
            try{
                Response r = client.newCall(request).execute();
                if(r.isRedirect()){
                    subscriber.onNext(r.body().string());
                    subscriber.onCompleted();
                } else subscriber.onError(new Exception());
            }catch(IOException e){
                subscriber.onError(e);
            }
        });
    }

    private static String getSkyjamToken(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString("OAuthToken", "");
    }

    private static Request.Builder defaultBuilder(Context context){
        return new Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.googleUserAgent)
                .header("Authorization", "GoogleLogin auth=" + getSkyjamToken(context))
                .header("X-Device-ID", IdentityUtils.deviceId(context));
    }




}


