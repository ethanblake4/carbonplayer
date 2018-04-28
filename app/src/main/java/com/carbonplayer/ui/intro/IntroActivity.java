package com.carbonplayer.ui.intro;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.webview.GoogleLoginJSInterfaceObject;
import com.carbonplayer.ui.intro.fragments.IntroPageOneFragment;
import com.carbonplayer.ui.intro.fragments.IntroPageThreeFragment;
import com.carbonplayer.ui.intro.fragments.IntroPageTwoFragment;
import com.carbonplayer.utils.general.IdentityUtils;
import com.carbonplayer.utils.general.JavascriptUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

/**
 * Controller for intro screens, also logs in and gets Play Music library
 */
public class IntroActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private IntroPresenter presenter;

    private static final int NUM_PAGES = 3;

    @BindView(R.id.introPager)
    ViewPager mPager;

    @BindView(R.id.next)
    ImageButton nextButton;

    @State
    boolean enableSwitching;

    @State
    int currentPage = 0;

    @State
    int savedPageState = 0;

    WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Icepick.restoreInstanceState(this, savedInstanceState);

        presenter = new IntroPresenter(this);

        setContentView(R.layout.activity_intro);

        ButterKnife.bind(this);

        mPager = findViewById(R.id.introPager);
        nextButton = findViewById(R.id.next);

        if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInMultiWindowMode()) ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            ConstraintLayout.LayoutParams lp =
                    ((ConstraintLayout.LayoutParams) nextButton.getLayoutParams());
            lp.bottomMargin = lp.bottomMargin + IdentityUtils.getNavbarHeight(getResources());

            nextButton.setLayoutParams(lp);
        }

        // Instantiate a ViewPager and a PagerAdapter.
        PagerAdapter mPagerAdapter = new IntroAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(this);
        mPager.setCurrentItem(currentPage);

        nextButton.setOnClickListener(v -> mPager.setCurrentItem(1));
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        savedPageState = currentPage;
        Icepick.saveInstanceState(this, outState);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        currentPage = position;

        int width = IdentityUtils.displayWidth(this);

        if (position == 0) nextButton.setAlpha(1.0f - positionOffset);
        if (position == 1 && !enableSwitching && positionOffsetPixels >= 50) {
            mPager.setCurrentItem(1, true);
            mPager.setScrollX(Math.min(mPager.getScrollX(), width - (width * savedPageState) + 150));
        } else if (position == 1 && enableSwitching && positionOffsetPixels <= width - 50) {
            mPager.setCurrentItem(2, true);
            mPager.setScrollX(Math.max(mPager.getScrollX(), ((width * 2) - (width * savedPageState)) - 150));
        } else if (position == 2 && enableSwitching && positionOffsetPixels >= 50) {
            mPager.setCurrentItem(2, true);
            mPager.setScrollX(Math.min(mPager.getScrollX(), ((width * 2) - (width * savedPageState)) + 150));
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageSelected(int position) {
        if (position >= 1) nextButton.setAlpha(0.0f);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void beginAuthentication() {

        Dialog authDialog = new Dialog(IntroActivity.this);

        if (CarbonPlayerApplication.Companion.getInstance().getUseWebAuthDialog()) {
            authDialog.setContentView(R.layout.auth_dialog);

            presenter.setAuthDialog(authDialog);

            web = authDialog.findViewById(R.id.webv);

            //Setup WebView
            web.getSettings().setJavaScriptEnabled(true);
            web.addJavascriptInterface(new GoogleLoginJSInterfaceObject(this), "Android");
            web.getSettings().setDisplayZoomControls(false);
            web.getSettings().setUseWideViewPort(true);
            web.getSettings().setLoadWithOverviewMode(true);

            web.loadUrl("https://accounts.google.com/AddSession?sacu=1");
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Timber.d("Page finished");
                    super.onPageFinished(view, url);
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        JavascriptUtils.injectCovertly(IntroActivity.this, web, "js/jquery.js");
                        JavascriptUtils.injectCovertly(IntroActivity.this, web, "js/setUp.js");
                    }, 5);
                }
            });
        } else {
            authDialog.setContentView(R.layout.auth_dialog_std);
            presenter.setAuthDialog(authDialog);
            authDialog.findViewById(R.id.sign_in_dialog_button).setOnClickListener(v -> {
                String user = ((EditText) authDialog.findViewById(R.id.sign_in_username_email)).getText().toString();
                String pass = ((EditText) authDialog.findViewById(R.id.sign_in_dialog_password)).getText().toString();
                presenter.tryLogin(user, pass);
            });

        }

        authDialog.show();
        authDialog.setTitle(getString(R.string.intro_signin));
        authDialog.setCancelable(true);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    public void callbackWithUsername(String username) {
        presenter.callbackWithUsername(username);
    }
    public void callbackWithPassword(String password) {
        presenter.callbackWithPassword(password);
    }

    void makeLibraryError(@StringRes int desc) {
        Timber.e(getString(desc));
        ((TextView) findViewById(R.id.introSettingup)).setText(R.string.intro_slide3_error);
        ((TextView) findViewById(R.id.introSlide3Desc)).setText(desc);
        findViewById(R.id.slide3Spinner).setVisibility(View.GONE);
        findViewById(R.id.nautilusFailedOKButton).setVisibility(View.VISIBLE);
        findViewById(R.id.nautilusFailedOKButton).setEnabled(true);
    }

    void endSuccessfully() {
        Timber.d("End IntroActivity successfully");
        CarbonPlayerApplication.Companion.getInstance().getPreferences().firstStart = false;
        CarbonPlayerApplication.Companion.getInstance().getPreferences().save();
        this.finish();
    }

    void slide3Progress(boolean playlist, int p) {
        ((TextView) findViewById(R.id.introSlide3Desc)).setText(playlist ?
                getString(R.string.intro_slide3_desc3) :
                getString(R.string.intro_slide3_desc2, p));
    }

}


