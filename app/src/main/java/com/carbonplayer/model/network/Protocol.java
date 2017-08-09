package com.carbonplayer.model.network;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.model.entity.ConfigEntry;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.entity.exception.ResponseCodeException;
import com.carbonplayer.model.entity.exception.ServerRejectionException;
import com.carbonplayer.utils.Gservices;
import com.carbonplayer.utils.IdentityUtils;
import com.carbonplayer.utils.URLSigning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;


import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Single;
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
        final OkHttpClient client = CarbonPlayerApplication.Companion.getInstance().getOkHttpClient();
        final Uri.Builder getParams = new Uri.Builder()
                .appendQueryParameter("dv", CarbonPlayerApplication.Companion.getInstance().getGoogleBuildNumber())
                .appendQueryParameter("tier", "aa")
                .appendQueryParameter("hl", IdentityUtils.localeCode());

        return Observable.create(subscriber -> {
            Request request = defaultBuilder(context.getBaseContext())
                    .url(SJ_URL + "config?" + getParams.build().getEncodedQuery())
                    .build();
            try {
                Response r = client.newCall(request).execute();
                if (!r.isSuccessful()) subscriber.onError(new ResponseCodeException());

                String sR = r.body().string();
                Timber.d(sR);
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
        final OkHttpClient client = CarbonPlayerApplication.Companion.getInstance().getOkHttpClient();
        final Uri.Builder getParams = new Uri.Builder()
                .appendQueryParameter("dv", CarbonPlayerApplication.Companion.getInstance().getGoogleBuildNumber())
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
                    String response = r.body().string();
                    JSONObject j = new JSONObject(response);
                    //Timber.d(response);

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

    public static Single<String> getStreamURL(@NonNull final Context context, String song_id){
        ArrayList<okhttp3.Protocol> protocols = new ArrayList<>();
        protocols.add(okhttp3.Protocol.HTTP_1_1);
        final OkHttpClient client = CarbonPlayerApplication.Companion.getInstance().getOkHttpClient(
                new OkHttpClient().newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .protocols(protocols));
        return Single.create(subscriber -> {
            String salt = String.valueOf(new Date().getTime());
            String digest = "";
            try {
                digest = URLSigning.sign(song_id, salt);
            } catch(NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e){
                subscriber.onError(e);
            }
            final Uri.Builder getParams = new Uri.Builder();
            try {
                if (song_id.startsWith("T") || song_id.startsWith("D")) getParams.appendQueryParameter("mjck", song_id);
                else getParams.appendQueryParameter("songid", song_id);
            } catch(Exception e){
                subscriber.onError(e);
            }

            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            Timber.d("androidId: %s", androidId);

            //Timber.d("newAndroidID: %s", String.valueOf(Gservices.getLong(context.getContentResolver(), "android_id", 0)));

            getParams
                .appendQueryParameter("targetkbps", "180")
                .appendQueryParameter("audio_formats", "mp3")
                .appendQueryParameter("dv", CarbonPlayerApplication.Companion.getInstance().getGoogleBuildNumber())
                .appendQueryParameter("p", IdentityUtils.getDeviceIsSmartphone(context) ? "1" : "0")
                .appendQueryParameter("opt", getStreamQualityHeader(context))
                .appendQueryParameter("net", getNetHeader(context))
                .appendQueryParameter("pt", "e")
                .appendQueryParameter("adaptive", "true")
                //.appendQueryParameter("dt", "pc")
                .appendQueryParameter("slt", salt)
                .appendQueryParameter("sig", digest)
                .appendQueryParameter("hl", IdentityUtils.localeCode())
                .appendQueryParameter("tier", "aa");


            String encQuery = getParams.build().getEncodedQuery();
            Timber.d(encQuery);
            //encQuery = encQuery/*.substring(0, encQuery.indexOf("%"))*/ + "&hl=" + IdentityUtils.localeCode() + "&tier=aa";
            Timber.d(encQuery);
            Request request = bearerBuilder(context)
                    .url(STREAM_URL + "?" + encQuery)
                    .build();
            try{
                Response r = client.newCall(request).execute();
                if(r.isRedirect()){
                    subscriber.onSuccess(r.headers().get("Location"));
                } else {
                    if(r.code() == 401 || r.code() == 402 || r.code() == 403){
                        String rejectionReason = r.header("X-Rejected-Reason");
                        if(rejectionReason != null){
                            try {
                                ServerRejectionException.RejectionReason rejectionReasonEnum =
                                        ServerRejectionException.RejectionReason.valueOf(rejectionReason.toUpperCase());
                                Timber.e(new ServerRejectionException(rejectionReasonEnum), "getStreamURL: serverRejected");
                                switch(rejectionReasonEnum) {
                                    case DEVICE_NOT_AUTHORIZED:
                                        GoogleLogin.retryGoogleAuth(context);
                                    case ANOTHER_STREAM_BEING_PLAYED:
                                    case STREAM_RATE_LIMIT_REACHED:
                                    case TRACK_NOT_IN_SUBSCRIPTION:
                                    case WOODSTOCK_SESSION_TOKEN_INVALID:
                                    case WOODSTOCK_ENTRY_ID_INVALID:
                                    case WOODSTOCK_ENTRY_ID_EXPIRED:
                                    case WOODSTOCK_ENTRY_ID_TOO_EARLY:
                                    case DEVICE_VERSION_BLACKLISTED:
                                        subscriber.onError(new ServerRejectionException(rejectionReasonEnum));
                                }
                            } catch (IllegalArgumentException e){
                                try {
                                    GoogleLogin.retryGoogleAuthSync(context);
                                } catch (Exception s) {
                                    Timber.e(e, "Exception retrying Google Auth");
                                }
                                subscriber.onError(new ServerRejectionException(ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED));
                            }
                        } else {
                            try {
                                GoogleLogin.retryGoogleAuthSync(context);
                            } catch (Exception e) {
                                Timber.e(e, "Exception retrying Google Auth");
                            }
                            subscriber.onError(new ServerRejectionException(ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED));
                        }
                    } else if (r.code() >= 200 && r.code() < 300){
                        subscriber.onError(new ResponseCodeException(String.format(Locale.getDefault(),"Unexpected response code %d", r.code())));
                    }
                    subscriber.onError(new Exception(r.body().string()));
                }
            }catch(IOException e){
                subscriber.onError(e);
            }
        });
    }

    private static String getNetHeader(Context context){
        switch (IdentityUtils.networkType(context)){
            case WIFI: return "wifi";
            case ETHER: return "ether";
            case MOBILE: return "mob";
            default: return "";
        }
    }

    private static String getStreamQualityHeader(Context context){
        StreamQuality streamQuality;
        switch (IdentityUtils.networkType(context)){
            case WIFI:
            case ETHER:
                streamQuality = CarbonPlayerApplication.Companion.getInstance().getPreferences().preferredStreamQualityWifi;
                break;
            case MOBILE:
            default:
                streamQuality = CarbonPlayerApplication.Companion.getInstance().getPreferences().preferredStreamQualityMobile;
                break;
        }
        if(streamQuality == null)
            streamQuality = StreamQuality.MEDIUM;
        switch (streamQuality) {
            case HIGH: return "hi";
            case MEDIUM: return "med";
            case LOW: return "low";
        }
        return "";
    }

    private static String getSkyjamToken(Context context) {
        return CarbonPlayerApplication.Companion.getInstance().getPreferences().OAuthToken;
    }

    private static String getBearerToken(Context context){
        return CarbonPlayerApplication.Companion.getInstance().getPreferences().BearerAuth;
    }

    private static Request.Builder defaultBuilder(Context context){
        return new Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.Companion.getInstance().getGoogleUserAgent())
                .header("Authorization", "GoogleLogin auth=" + getSkyjamToken(context))
                .header("X-Device-ID", IdentityUtils.deviceId(context))
                .header("X-Device-Logging-ID", IdentityUtils.getLoggingID(context));
    }

    private static Request.Builder bearerBuilder(Context context){
        Timber.d("Bearer token: %s", getBearerToken(context));

        String deviceId = String.valueOf(Gservices.getLong(context.getContentResolver(), "android_id", 0));

        if(deviceId.equals(String.valueOf(0))){
            deviceId = IdentityUtils.deviceId(context);
        }

        return new Request.Builder()
                .header("User-Agent", CarbonPlayerApplication.Companion.getInstance().getGoogleUserAgent())
                .header("Authorization", "Bearer " + getBearerToken(context))
                .header("X-Device-ID", deviceId)
                .header("X-Device-Logging-ID", IdentityUtils.getLoggingID(context));
                //.header("X-Device-ID", IdentityUtils.deviceId(context));
    }




}


