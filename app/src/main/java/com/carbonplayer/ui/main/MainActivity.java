package com.carbonplayer.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.carbonplayer.R;
import com.carbonplayer.ui.intro.IntroActivity;

import flow.Flow;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());

                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

                if (isFirstStart) {
                    Intent i = new Intent(MainActivity.this, IntroActivity.class);
                    startActivity(i);

                    SharedPreferences.Editor e = getPrefs.edit();
                    e.putBoolean("firstStart", false);

                    e.apply();
                }
            }
        });

        t.start();

        setContentView(R.layout.activity_main);
    }

    @Override protected void attachBaseContext(Context baseContext) {
        baseContext = Flow.configure(baseContext, this).install();
        super.attachBaseContext(baseContext);
    }
}
