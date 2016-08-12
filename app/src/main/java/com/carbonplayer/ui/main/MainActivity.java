package com.carbonplayer.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.RealmString;
import com.carbonplayer.ui.intro.IntroActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.RealmResults;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private Subscription albumSubscription;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    @BindView(R.id.main_recycler) RecyclerView mainRecycler;

    private static int screenHeightPx;
    private static int screenWidthPx;
    private static int screenHeightDp;
    private static int screenWidthDp;
    private static int dpSize;

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

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Configuration configuration = getResources().getConfiguration();
        screenHeightPx = size.y;
        screenWidthPx = size.x;
        screenWidthDp = configuration.screenWidthDp;
        screenHeightDp = configuration.screenHeightDp;

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

    private class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
        private List<Album> mDataset;
        private MainActivity context;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            ImageView thumb;
            TextView titleText;
            TextView detailText;
            String songs;
            Album album;

            ViewHolder(View v) {
                super(v);
                thumb = (ImageView)    v.findViewById(R.id.imgthumb);
                titleText = (TextView) v.findViewById(R.id.primaryText);
                detailText = (TextView)v.findViewById(R.id.detailText);
                v.findViewById(R.id.gridLayoutRoot).setOnClickListener(view -> {
                    //Toast.makeText(MainActivity.this, songs, Toast.LENGTH_SHORT).show();
                    CarbonPlayerApplication.getInstance().currentAlbum = album;
                    Intent i = new Intent(context, AlbumActivity.class);
                    startActivity(i);
                });

                titleText.setMaxWidth((screenWidthPx/2)-(dpToPx(50)));
                detailText.setMaxWidth((screenWidthPx/2)-(dpToPx(32)));
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        AlbumAdapter(List<Album> myDataset, MainActivity context) {
            mDataset = myDataset;
            this.context = context;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public AlbumAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.grid_item_layout, parent, false);
            // set the view's size, margins, paddings and layout parameters

            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            Album a = mDataset.get(position);
            holder.titleText.setText(a.getTitle());
            holder.detailText.setText(a.getArtist());
            holder.album = a;

            StringBuilder sb = new StringBuilder();
            List<RealmString> songs = a.getSongIds();
            for(RealmString string: songs){
                sb.append(MusicLibrary.getInstance().getTrack(string.toString()).getTitle());
                sb.append(", ");
            }
            holder.songs = sb.toString();

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }

    private int dpToPx(int dp){
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }
}
