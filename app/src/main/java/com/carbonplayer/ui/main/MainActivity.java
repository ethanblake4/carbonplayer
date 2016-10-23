package com.carbonplayer.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.ui.intro.IntroActivity;
import com.carbonplayer.utils.URLSigning;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscription;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private Subscription albumSubscription;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    @BindView(R.id.main_recycler) RecyclerView mainRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  Declare a new thread to do a preference check
        Thread t = new Thread(() -> {
            SharedPreferences getPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());

            boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

            if (isFirstStart) {
                Intent i = new Intent(MainActivity.this, IntroActivity.class);
                startActivity(i);
            }
        });

        t.start();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mainRecycler.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new GridLayoutManager(MainActivity.this, 2);
        mainRecycler.setLayoutManager(mLayoutManager);

        sectionSelected();
    }

    private void sectionSelected() {
        if (albumSubscription != null) {
            albumSubscription.unsubscribe();
        }
        final MainActivity context = this;
        albumSubscription = MusicLibrary.getInstance().loadAlbums()
                .subscribe(albums -> {
                    mAdapter = new AlbumAdapter(albums, context);
                    mainRecycler.setAdapter(mAdapter);
                });
    }

    private int dpToPx(int dp){
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }
}
