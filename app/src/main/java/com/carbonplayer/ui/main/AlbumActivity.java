package com.carbonplayer.ui.main;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.RealmString;
import com.carbonplayer.ui.widget.ParallaxScrimageView;
import com.carbonplayer.ui.widget.SquareView;
import com.carbonplayer.utils.MathUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.RealmList;

/**
 * Displays an album
 */
public class AlbumActivity extends AppCompatActivity {

    @BindView(R.id.albumLayoutRoot) FrameLayout layoutRoot;
    @BindView(R.id.primaryText) TextView primaryText;
    @BindView(R.id.secondaryText) TextView secondaryText;
    @BindView(R.id.main_backdrop) ParallaxScrimageView albumart;
    @BindView(R.id.songgroup_recycler) RecyclerView songList;
    @BindView(R.id.songgroup_scrollview) ScrollView scrollView;
    @BindView(R.id.play_fab) FloatingActionButton fab;
    @BindView(R.id.parallaxSquare) SquareView square;

    private int fabOffset;
    private int squareHeight;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Album mAlbum;
    private ArrayList<MusicTrack> tracks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songgroup);
        ButterKnife.bind(this);

        mAlbum = CarbonPlayerApplication.getInstance().currentAlbum;

        scrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        layoutRoot.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

        squareHeight = albumart.getHeight();
        fabOffset = MathUtils.dpToPx(this, 23);

        primaryText.setText(mAlbum.getArtist());
        secondaryText.setText(mAlbum.getTitle());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(AlbumActivity.this){
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        songList.setLayoutManager(mLayoutManager);

        RealmList<RealmString> trackIds = mAlbum.getSongIds();
        for(RealmString trackId : trackIds){
            tracks.add(MusicLibrary.getInstance().getTrack(trackId.getValue()));
        }

        mAdapter = new SongListAdapter(tracks, this);
        songList.setAdapter(mAdapter);

    }

    private ViewTreeObserver.OnScrollChangedListener scrollListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            int scrollY = scrollView.getScrollY();
            albumart.setOffset(-scrollY);
            fab.setY(albumart.getHeight() - scrollY - fabOffset);
        }
    };

    private ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            fab.setY(squareHeight+fab.getY()-fabOffset);
        }
    };
}
