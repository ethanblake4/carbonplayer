package com.carbonplayer.ui.main;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.MusicLibrary;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.model.entity.RealmString;
import com.carbonplayer.utils.MathUtils;
import com.github.florent37.glidepalette.BitmapPalette;
import com.github.florent37.glidepalette.GlidePalette;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Displays albums in variable-size grid view
 */

class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private List<Album> mDataset;
    private MainActivity context;
    private int screenWidthPx;

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.gridLayoutRoot) View layoutRoot;
        @BindView(R.id.gridLayoutContentRoot) View contentRoot;
        @BindView(R.id.imgthumb) ImageView thumb;
        @BindView(R.id.primaryText) TextView titleText;
        @BindView(R.id.detailText) TextView detailText;
        String songs;
        Album album;
        int size;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);

            layoutRoot.setOnClickListener(view -> {
                //Toast.makeText(MainActivity.this, songs, Toast.LENGTH_SHORT).show();
                CarbonPlayerApplication.getInstance().currentAlbum = album;
                Intent i = new Intent(context, AlbumActivity.class);

                ActivityOptions options = ActivityOptions
                        .makeSceneTransitionAnimation(context,
                                Pair.create(thumb, "imgthumb"),
                                Pair.create(contentRoot, "albumdetails"),
                                Pair.create(titleText, "albumName"),
                                Pair.create(detailText, "artistName"));
                context.startActivity(i, options.toBundle());
            });

            ViewTreeObserver vto = thumb.getViewTreeObserver();
            vto.addOnPreDrawListener(() -> {
                size = thumb.getMeasuredWidth();
                return true;
            });

            titleText.setMaxWidth((screenWidthPx/2)-(MathUtils.dpToPx(context, 50)));
            detailText.setMaxWidth((screenWidthPx/2)-(MathUtils.dpToPx(context, 32)));
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    AlbumAdapter(List<Album> myDataset, MainActivity context) {
        mDataset = myDataset;
        this.context = context;
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidthPx = size.x;
    }

    @Override
    public void onViewRecycled(ViewHolder v){
        Glide.clear(v.thumb);
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
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        @ColorInt int defaultColor = Color.parseColor("#292929");
        @ColorInt int defaultTextColor = Color.parseColor("#cccccc");
        holder.contentRoot.setBackgroundColor(defaultColor);
        holder.titleText.setTextColor(defaultTextColor);
        holder.detailText.setTextColor(defaultTextColor);

        Album a = mDataset.get(position);
        holder.titleText.setText(a.getTitle());
        holder.detailText.setText(a.getArtist());
        holder.album = a;

        if(a.getAlbumArtURL() != null && !a.getAlbumArtURL().equals("")) {
            Glide.with(context).load(a.getAlbumArtURL())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(
                            GlidePalette.with(a.getAlbumArtURL())
                                    .use(GlidePalette.Profile.VIBRANT)
                                    .intoBackground(holder.contentRoot)
                                    .intoTextColor(holder.titleText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                                    .intoTextColor(holder.detailText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                                    .crossfade(true, 500)
                    )
                    .into(holder.thumb);

        } else {
            Glide.clear(holder.thumb);
            holder.thumb.setImageResource(R.drawable.unknown_music_track);
        }

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