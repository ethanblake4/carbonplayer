package com.carbonplayer.ui.intro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.network.GoogleLogin;
import com.carbonplayer.model.network.Protocol;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.webview.GoogleLoginJSInterfaceObject;
import com.carbonplayer.ui.intro.fragments.IntroPageOneFragment;
import com.carbonplayer.ui.intro.fragments.IntroPageThreeFragment;
import com.carbonplayer.ui.intro.fragments.IntroPageTwoFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Controller for intro screens, also logs in and gets Play Music library
 */
public class IntroActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private static final int NUM_PAGES = 3;

    @BindView(R.id.introPager) ViewPager mPager;
    @BindView(R.id.next) ImageButton nextButton;

    private boolean enableSwitching;

    private WebView web;

    private String username;
    private String password;

    private Dialog auth_dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);
        ButterKnife.bind(this);

        // Instantiate a ViewPager and a PagerAdapter.
        PagerAdapter mPagerAdapter = new IntroAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(this);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(1);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPager.getCurrentItem() > 0) nextButton.setAlpha(0.0f);
        if (mPager.getCurrentItem() < 2) enableSwitching = false;
    }

    @Override
    public void onBackPressed() {
        //thwart plans
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position == 0) {
            nextButton.setAlpha(1.0f - positionOffset);
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        if (position == 1 && !enableSwitching && positionOffsetPixels >= 50) {
            mPager.setCurrentItem(1, true);
            mPager.setScrollX(Math.min(mPager.getScrollX(), width + 150));
        } else if (position == 1 && enableSwitching && positionOffsetPixels <= width - 50) {

            mPager.setCurrentItem(2, true);
            mPager.setScrollX(Math.max(mPager.getScrollX(), (width * 2) - 150));
        } else if (position == 2 && enableSwitching && positionOffsetPixels >= 50) {
            mPager.setCurrentItem(2, true);
            mPager.setScrollX(Math.min(mPager.getScrollX(), (width * 2) + 150));
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    @Override
    public void onPageSelected(int position) {
        if (position == 1) nextButton.setAlpha(0.0f);

    }

    @SuppressLint("SetJavaScriptEnabled")
    public void beginOAuth2Authentication() {

        auth_dialog = new Dialog(IntroActivity.this);
        auth_dialog.setContentView(R.layout.auth_dialog);

        web = (WebView) auth_dialog.findViewById(R.id.webv);

        //Setup WebView
        web.getSettings().setJavaScriptEnabled(true);
        web.addJavascriptInterface(new GoogleLoginJSInterfaceObject(this), "Android");
        web.getSettings().setDisplayZoomControls(false);
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setLoadWithOverviewMode(true);

        web.loadUrl("https://accounts.google.com/AddSession?sacu=1");
        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        injectCovertly("js/jquery.js");
                        injectCovertly("js/setUp.js");
                        injectJavascript("armForNoAccount()");
                    }
                }, 5);
            }
        });

        auth_dialog.show();
        auth_dialog.setTitle(getString(R.string.intro_signin));
        auth_dialog.setCancelable(true);
    }

    private void injectCovertly(String pathToFile) {
        try {
            InputStream input = getApplicationContext().getAssets().open(pathToFile);
            byte[] buffer = new byte[input.available()];
            //noinspection ResultOfMethodCallIgnored
            input.read(buffer);
            input.close();

            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);

            injectJavascript("(function() {var parent=document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type='text/javascript';" +
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void injectJavascript(String javascript) {
        if (web != null) {
            web.evaluateJavascript(javascript, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {}
            });
        }
    }

    public void callbackWithJson(String json) {
        try {
            JSONObject j = new JSONObject(json);
            if (j.getString("candidateType").equals("field")) {
                String fieldId = j.getString("fieldId");

                if (fieldId.equals("Email")) username = j.getString("value");
                if (fieldId.equals("Passwd")) password = j.getString("value");

                if (username != null && password != null) {
                    auth_dialog.dismiss();

                    enableSwitching = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPager.setCurrentItem(3);
                        }
                    });

                    web = null;
                    auth_dialog = null;
                    Log.d("JsonCallback", "user/pass retrieved");

                    if (!username.contains("@")) username = username + "@gmail.com";

                    new LoginTask().execute(this);

                }
            }
        } catch (JSONException j) {
            j.printStackTrace();
        }
    }

    private static class IntroAdapter extends FragmentStatePagerAdapter {

        IntroAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) return new IntroPageOneFragment();
            if (position == 1) return new IntroPageTwoFragment();
            return new IntroPageThreeFragment();
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

    }

    private String getUsername() {
        return username;
    }

    private String getPassword() {
        return password;
    }

    private void getLibrary() {
        new GetLibraryTask().execute(this);
    }

    private void end(){
        this.finish();
    }

    private class LoginTask extends AsyncTask<IntroActivity, Void, Boolean> {

        protected Boolean doInBackground(IntroActivity... caller) {
            return GoogleLogin.login(caller[0], caller[0].getUsername(), caller[0].getPassword());
        }

        protected void onProgressUpdate(Void... values) {}

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                ((TextView) findViewById(R.id.introSlide3Desc)).setText(R.string.intro_slide3_desc2);
                getLibrary();
            }
        }
    }

    private class GetLibraryTask extends AsyncTask<Activity, Void, TrackResult> {

        private static final int SUCCESS = 0;
        private static final int ERR_NOT_NAUTILUS = 1;
        private static final int ERR_RESPONSECODE = 2;
        private static final int ERR_IOEXCEPTION = 3;

        protected TrackResult doInBackground(Activity... caller) {

            try {
                return doConfig(caller[0]);
            } catch (Protocol.Call.ResponseCodeException e) {
                e.printStackTrace();
                return new TrackResult(null, ERR_RESPONSECODE);
            } catch (IOException e) {
                e.printStackTrace();
                return new TrackResult(null, ERR_IOEXCEPTION);
            }

        }

        private TrackResult doConfig(Activity caller) throws IOException, Protocol.Call.ResponseCodeException {

            Protocol.ConfigCall config = new Protocol().new ConfigCall();
            JSONObject json = config.execute(caller);

            try {
                JSONObject nautilus = json.getJSONObject("data").getJSONArray("entries").getJSONObject(62);
                Log.d("doConfig", nautilus.toString());
                if (nautilus.getString("value").equals("false")) {
                    return new TrackResult(null, ERR_NOT_NAUTILUS);
                } else {
                    return getSongs(caller);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return new TrackResult(null, ERR_IOEXCEPTION);
            }

        }

        private TrackResult getSongs(Activity caller) throws IOException, Protocol.Call.ResponseCodeException {

            Protocol.ListTracksCall listTracks = new Protocol().new ListTracksCall();
            JSONObject json;
            json = listTracks.execute(caller, null, 250);

            Log.d("getSongs", json.toString());

            ArrayList<MusicTrack> tracks = new ArrayList<>();

            try {
                while (json.has("nextPageToken")) {
                    Timber.d("exec with nextPageToken=%s",json.getString("nextPageToken"));
                    JSONArray itemArray = json.getJSONObject("data").getJSONArray("items");
                    for(int i = 0; i<itemArray.length();i++) {
                        tracks.add(new MusicTrack(itemArray.getJSONObject(i)));

                    }
                    Timber.d(tracks.get(tracks.size()-1).toString());
                    Protocol.ListTracksCall sequentialTracks = new Protocol().new ListTracksCall();
                    json = sequentialTracks.execute(caller, json.getString("nextPageToken"), 250);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return new TrackResult(null, ERR_IOEXCEPTION);
            }

            return new TrackResult(tracks, SUCCESS);

        }

        protected void onProgressUpdate(Void... values) {}

        protected void onPostExecute(TrackResult result) {
            super.onPostExecute(result);

            switch(result.getSuccessCode()){
                case SUCCESS:
                    Timber.d("copying tracks to Realm");
                    MusicLibrary.getInstance().saveTracksAsync(result.getTracks());
                    Timber.d("tracks saved");
                    SharedPreferences getPrefs = PreferenceManager
                            .getDefaultSharedPreferences(getBaseContext());
                    getPrefs.edit().putBoolean("firstStart", true).apply();
                    end();
                    break;
                case ERR_NOT_NAUTILUS:
                    ((TextView) findViewById(R.id.introSettingup)).setText(R.string.intro_slide3_error);
                    ((TextView) findViewById(R.id.introSlide3Desc)).setText(R.string.intro_slide3_no_nautilus);
                    findViewById(R.id.slide3Spinner).setVisibility(View.GONE);
                    findViewById(R.id.nautilusFailedOKButton).setVisibility(View.VISIBLE);
                    findViewById(R.id.nautilusFailedOKButton).setEnabled(true);
                    break;
                case ERR_IOEXCEPTION:
                    ((TextView) findViewById(R.id.introSettingup)).setText(R.string.intro_slide3_error);
                    ((TextView) findViewById(R.id.introSlide3Desc)).setText(R.string.intro_slide3_issue);
                    findViewById(R.id.slide3Spinner).setVisibility(View.GONE);
                    findViewById(R.id.nautilusFailedOKButton).setVisibility(View.VISIBLE);
                    findViewById(R.id.nautilusFailedOKButton).setEnabled(true);
            }
        }
    }

    private class TrackResult {
        private ArrayList<MusicTrack> list;
        private int successCode;

        TrackResult(ArrayList<MusicTrack> list, int successCode) {
            this.list = list;
            this.successCode = successCode;
        }

        int getSuccessCode() {
            return successCode;
        }

        ArrayList<MusicTrack> getTracks() {
            return list;
        }
    }


}


