package com.carbonplayer.ui.main.adapters;

import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.Album;
import com.carbonplayer.ui.main.MainActivity;
import com.carbonplayer.utils.general.MathUtils;
import com.carbonplayer.utils.ui.PaletteUtil;
import com.github.florent37.glidepalette.GlidePalette;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Displays albums in variable-size grid view
 */

public class TopChartsAlbumAdapter extends RecyclerView.Adapter<TopChartsAlbumAdapter.ViewHolder> {

    private List<Album> mDataset;
    private MainActivity context;
    private int screenWidthPx;
    private RequestManager requestManager;

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.gridLayoutRoot) View layoutRoot;
        @BindView(R.id.gridLayoutContentRoot) View contentRoot;
        @BindView(R.id.imgthumb) ImageView thumb;
        @BindView(R.id.primaryText) TextView titleText;
        @BindView(R.id.detailText) TextView detailText;
        @BindView(R.id.imageButton) ImageButton menuButton;


        Album album;
        PaletteUtil.SwatchPair swatchPair;
        int size;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);


            layoutRoot.setOnClickListener(view -> {

                thumb.setTransitionName(album.getId() + "i");
                contentRoot.setTransitionName(album.getId() + "cr");
                titleText.setTransitionName(album.getId() + "t");
                detailText.setTransitionName(album.getId() + "d");

                context.gotoAlbum(album, thumb, contentRoot, swatchPair.getPrimary().getTitleTextColor(),
                        swatchPair.getPrimary().getRgb(), swatchPair.getSecondary().getBodyTextColor(),
                        titleText, detailText);
                //context.gotoAlbum2(album, (FrameLayout)layoutRoot);
            });

            menuButton.setOnClickListener(view -> {
                context.showAlbumPopup(view, album);
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
    public TopChartsAlbumAdapter(List<Album> myDataset, MainActivity context,
                                 RequestManager requestManager) {
        mDataset = myDataset;
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
    public TopChartsAlbumAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
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

        if (!a.getAlbumArtURL().equals("")) {
            requestManager.load(a.getAlbumArtURL())
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .listener(GlidePalette.with(a.getAlbumArtURL())
                            .use(GlidePalette.Profile.VIBRANT)
                            .intoCallBack(palette -> {
                                if (palette != null) {
                                    PaletteUtil.SwatchPair pair =
                                            PaletteUtil.getSwatches(context, palette);
                                    holder.swatchPair = pair;

                                    PaletteUtil.crossfadeBackground(holder.contentRoot, pair.getPrimary());
                                    PaletteUtil.crossfadeTitle(holder.titleText, pair.getPrimary());
                                    PaletteUtil.crossfadeSubtitle(holder.detailText, pair.getPrimary());
                                }
                            })
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
        return mDataset.size();
    }
}
