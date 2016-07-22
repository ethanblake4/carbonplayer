package com.carbonplayer.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.RealmString;
import com.carbonplayer.ui.intro.IntroActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import flow.Flow;
import io.realm.RealmResults;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private Subscription albumSubscription;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    @BindView(R.id.main_recycler) RecyclerView mainRecycler;

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

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mainRecycler.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mainRecycler.setLayoutManager(mLayoutManager);


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
                        mAdapter = new AlbumAdapter(albums);
                        mainRecycler.setAdapter(mAdapter);
                    }
                });
    }

    private class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
        private List<Album> mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public ImageView thumb;
            public TextView titleText;
            public TextView detailText;
            public String songs;

            public ViewHolder(View v) {
                super(v);
                thumb = (ImageView)    v.findViewById(R.id.imgthumb);
                titleText = (TextView) v.findViewById(R.id.mainText);
                detailText = (TextView)v.findViewById(R.id.detailText);
                v.findViewById(R.id.listItemContraintLayout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(MainActivity.this, songs, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public AlbumAdapter(List<Album> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public AlbumAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_layout, parent, false);
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


    /*@Override protected void attachBaseContext(Context baseContext) {
        baseContext = Flow.configure(baseContext, this).install();
        super.attachBaseContext(baseContext);
    }*/
}
