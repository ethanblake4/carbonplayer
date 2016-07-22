package com.carbonplayer.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.ui.intro.IntroActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import flow.Flow;
import io.realm.RealmResults;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private Subscription albumSubscription;
    @BindView(R.id.mainText) TextView mainText;

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

        ButterKnife.bind(this);

        sectionSelected();
    }

    private void sectionSelected() {
        if (albumSubscription != null) {
            albumSubscription.unsubscribe();
        }
        albumSubscription = MusicLibrary.getInstance().loadAlbums()
                .subscribe(new Action1<RealmResults<Album>>() {
                    @Override
                    public void call(RealmResults<Album> albums) {
                        for(Album album : albums) {
                            mainText.append(album.getId());
                            mainText.append(" : ");
                            mainText.append(album.getTitle());
                            mainText.append("\n");
                        }
                    }
                });
    }

    /*@Override protected void attachBaseContext(Context baseContext) {
        baseContext = Flow.configure(baseContext, this).install();
        super.attachBaseContext(baseContext);
    }*/
}
