package com.carbonplayer.ui.main.adapters;

import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.Playlist;
import com.carbonplayer.ui.main.MainActivity;
import com.carbonplayer.utils.general.MathUtils;
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.github.florent37.glidepalette.BitmapPalette;
import com.github.florent37.glidepalette.GlidePalette;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Displays albums in variable-size grid view
 */

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder>
        implements SectionTitleProvider {

    private List<Playlist> dataset;
    private MainActivity context;
    private int screenWidthPx;
    private RequestManager requestManager;

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.gridLayoutRoot) View layoutRoot;
        @BindView(R.id.gridLayoutContentRoot) View contentRoot;
        @BindView(R.id.imgthumb) ImageView thumb;
        @BindView(R.id.primaryText) TextView titleText;
        @BindView(R.id.detailText) TextView detailText;
        Playlist playlist;
        int size;
        int mainColor;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);


            layoutRoot.setOnClickListener(view -> {

                thumb.setTransitionName(playlist.getId() + "i");
                contentRoot.setTransitionName(playlist.getId() + "cr");
                titleText.setTransitionName(playlist.getId() + "t");
                detailText.setTransitionName(playlist.getId() + "d");

                //context.gotoAlbum(album, thumb, contentRoot, titleText.getCurrentTextColor(), mainColor, titleText, detailText);
            });

            ViewTreeObserver vto = thumb.getViewTreeObserver();
            vto.addOnPreDrawListener(() -> {
                size = thumb.getMeasuredWidth();
                ((FrameLayout.LayoutParams) contentRoot.getLayoutParams())
                        .setMargins(0, size, 0, 0);
                contentRoot.postInvalidate();
                return true;
            });

            titleText.setMaxWidth((screenWidthPx / 2) - (MathUtils.dpToPx(context, 50)));
            detailText.setMaxWidth((screenWidthPx / 2) - (MathUtils.dpToPx(context, 32)));
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PlaylistAdapter(List<Playlist> dataset, MainActivity context,
                           RequestManager requestManager) {
        this.dataset = dataset;
        this.context = context;
        this.requestManager = requestManager;
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidthPx = size.x;
    }

    @Override
    public void onViewRecycled(ViewHolder v) {
        requestManager.clear(v.thumb);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
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

        Playlist playlist = dataset.get(position);
        holder.titleText.setText(playlist.getName());
        holder.playlist = playlist;

        if (playlist.getAlbumArtURL() != null && !playlist.getAlbumArtURL().equals("")) {
            requestManager.load(playlist.getAlbumArtURL())
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .listener(
                            GlidePalette.with(playlist.getAlbumArtURL())
                                    .use(GlidePalette.Profile.VIBRANT)
                                    .intoBackground(holder.contentRoot)
                                    .intoTextColor(holder.titleText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                                    .intoTextColor(holder.detailText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                                    .intoCallBack(palette -> {
                                        if (palette != null) {
                                            Palette.Swatch vibra = palette.getVibrantSwatch();
                                            if (vibra != null)
                                                holder.mainColor = palette.getVibrantSwatch().getRgb();
                                            else holder.mainColor = Color.parseColor("#ff333333");
                                        }
                                    })
                                    .crossfade(true, 500)
                    )
                    .into(holder.thumb);

        } else {
            requestManager.clear(holder.thumb);
            holder.thumb.setImageResource(R.drawable.unknown_music_track);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public String getSectionTitle(int position) {
        return dataset.get(position).getName().substring(0, 1);
    }
}