package com.carbonplayer.ui.main;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.base.ITrack;
import com.carbonplayer.ui.helpers.NowPlayingHelper;
import com.carbonplayer.ui.main.adapters.SongListAdapter;
import com.carbonplayer.ui.transition.DetailSharedElementEnterCallback;
import com.carbonplayer.ui.widget.ParallaxScrimageView;
import com.carbonplayer.ui.widget.SquareView;
import com.carbonplayer.utils.general.IdentityUtils;
import com.carbonplayer.utils.general.MathUtils;
import com.carbonplayer.utils.ui.ColorUtils;
import com.github.florent37.glidepalette.BitmapPalette;
import com.github.florent37.glidepalette.GlidePalette;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kotlin.Unit;
import timber.log.Timber;

/**
 * Displays an album
 */
public class AlbumActivity extends AppCompatActivity {

    @BindView(R.id.albumLayoutRoot) FrameLayout layoutRoot;
    @BindView(R.id.constraintLayout6) ConstraintLayout constraintLayoutRoot;
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
    private List<ITrack> tracks;

    private NowPlayingHelper nowPlayingHelper;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songgroup);

        ButterKnife.bind(this);

        nowPlayingHelper = new NowPlayingHelper(this);

        mAlbum = CarbonPlayerApplication.Companion.getInstance().getCurrentAlbum();

        Timber.d("album %s", mAlbum.getAlbumId());

        scrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        layoutRoot.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

        squareHeight = albumart.getHeight();
        fabOffset = MathUtils.dpToPx(this, 28);

        int preImageWidth = IdentityUtils.displayWidthDp(this) - 4;

        DetailSharedElementEnterCallback handler = new DetailSharedElementEnterCallback(this);
        handler.addTextViewSizeResource(primaryText,
                R.dimen.small_text_size, R.dimen.large_text_size);
        handler.addTextViewSizeResource(secondaryText,
                R.dimen.small_text_2, R.dimen.large_text_2);

        //noinspection SuspiciousNameCombination
        Glide.with(this).load(mAlbum.getAlbumArtRef())
                .apply(RequestOptions.overrideOf(preImageWidth, preImageWidth).diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate())
                .listener(
                        GlidePalette.with(mAlbum.getAlbumArtRef())
                                .use(GlidePalette.Profile.VIBRANT)
                                .intoBackground(constraintLayoutRoot)
                                .intoTextColor(primaryText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                                .intoTextColor(secondaryText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                                .intoCallBack(palette -> {
                                    if (Color.red(ColorUtils.contrastColor(palette.getVibrantColor(Color.DKGRAY))) > 200) {
                                        Timber.d("red>200");
                                        fab.setBackgroundTintList(ColorStateList.valueOf(palette.getLightVibrantColor(Color.WHITE)));
                                        //nowPlayingHelper.getDetailsView().setBackgroundColor(palette.getLightVibrantColor(Color.WHITE));
                                    } else {
                                        Timber.d("not");
                                        ColorStateList s = ColorStateList.valueOf(palette.getDarkVibrantColor(Color.DKGRAY));
                                        ColorStateList t = ColorStateList.valueOf(palette.getVibrantColor(Color.WHITE));
                                        Timber.d(s.toString());
                                        Timber.d(t.toString());
                                        fab.setBackgroundTintList(s);
                                        fab.setImageTintList(t);
                                        //nowPlayingHelper.getDetailsView().setBackgroundColor(palette.getDarkVibrantColor(Color.DKGRAY));
                                    }
                                })
                )
                .into(albumart);

        fab.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(() -> {
            fab.setVisibility(View.VISIBLE);
            ScaleAnimation anim = new ScaleAnimation(0, 1, 0, 1, fab.getPivotX(), albumart.getHeight() - fabOffset);
            anim.setFillAfter(true);
            anim.setDuration(310);
            anim.setInterpolator(new FastOutSlowInInterpolator());
            fab.startAnimation(anim);
        }, 600);


        /*new Handler().postDelayed(() ->
                Glide.with(AlbumActivity.this).load(mAlbum.getAlbumArtRef())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .into(albumart), 1000);*/

        secondaryText.setText(mAlbum.getArtists().first().getName());
        primaryText.setText(mAlbum.getName());

        songList.setNestedScrollingEnabled(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(AlbumActivity.this) {
            /*@Override
            public boolean canScrollVertically() { return false; }*/
        };
        songList.setLayoutManager(mLayoutManager);

        tracks = MusicLibrary.INSTANCE.getAllAlbumTracks(mAlbum);

        ViewGroup.LayoutParams params = songList.getLayoutParams();
        params.height = tracks.size() * MathUtils.dpToPx(this, 67);

        mAdapter = new SongListAdapter(tracks, songID -> {
            //nowPlayingHelper.makePlayingScreen(albumart.getDrawable());
            nowPlayingHelper.newQueue(tracks);
            return Unit.INSTANCE;
        });
        songList.setAdapter(mAdapter);

    }

    @Override
    public void onBackPressed() {
        fab.hide();
        super.onBackPressed();
    }

    @OnClick(R.id.play_fab)
    void playFABClicked() {
//        ..nowPlayingHelper.makePlayingScreen(albumart.getDrawable());
        nowPlayingHelper.newQueue(tracks);
    }

    public void setTransformedTextPosition(int transform) {
        secondaryText.layout(secondaryText.getLeft(), secondaryText.getTop() + transform,
                secondaryText.getRight(), secondaryText.getBottom() + transform);
    }

    private ViewTreeObserver.OnScrollChangedListener scrollListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            int scrollY = scrollView.getScrollY();
            albumart.setOffset(-scrollY);
            //fab.setY(albumart.getHeight() - scrollY - fabOffset);
        }
    };

    private ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            fab.setY(albumart.getHeight() - fabOffset);
        }
    };

    /*@Override
    public void onResume(){
        super.onResume();
        Glide.with(AlbumActivity.this).load(mAlbum.getAlbumArtRef())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(albumart);
    }

    @Override
    public void onStop(){
        super.onStop();
        Glide.clear(albumart);
    }*/
}
