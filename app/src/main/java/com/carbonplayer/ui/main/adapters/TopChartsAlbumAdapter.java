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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.carbonplayer.CarbonPlayerApplication;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.base.IAlbum;
import com.carbonplayer.ui.main.MainActivity;
import com.carbonplayer.utils.general.MathUtils;
import com.carbonplayer.utils.ui.ColorUtils;
import com.carbonplayer.utils.ui.PaletteUtil;
import com.github.florent37.glidepalette.GlidePalette;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Displays albums in variable-size grid view
 */

public class TopChartsAlbumAdapter extends RecyclerView.Adapter<TopChartsAlbumAdapter.ViewHolder> {

    private List<IAlbum> mDataset;
    private MainActivity context;
    private int screenWidthPx;
    private RequestManager requestManager;

    public boolean isHorizontal = false;

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.gridLayoutRoot) View layoutRoot;
        @BindView(R.id.gridLayoutContentRoot) View contentRoot;
        @BindView(R.id.imgthumb) ImageView thumb;
        @BindView(R.id.primaryText) TextView titleText;
        @BindView(R.id.detailText) TextView detailText;
        @BindView(R.id.imageButton) ImageButton menuButton;

        IAlbum album;
        PaletteUtil.SwatchPair swatchPair;
        int size;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);


            layoutRoot.setOnClickListener(view -> {

                thumb.setTransitionName(album.getAlbumId() + "i");
                contentRoot.setTransitionName(album.getAlbumId() + "cr");
                titleText.setTransitionName(album.getAlbumId() + "t");
                detailText.setTransitionName(album.getAlbumId() + "d");

                context.gotoAlbum(album, swatchPair);
                //context.gotoAlbum2(album, (FrameLayout)layoutRoot);
            });

            menuButton.setOnClickListener(view -> context.showAlbumPopup(view, album));

            titleText.setMaxWidth((screenWidthPx / 2) - (MathUtils.dpToPx(context, 50)));
            detailText.setMaxWidth((screenWidthPx / 2) - (MathUtils.dpToPx(context, 32)));
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public TopChartsAlbumAdapter(List<IAlbum> myDataset, MainActivity context,
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
                .inflate(isHorizontal ? R.layout.grid_item_layout_horizontal :
                        R.layout.grid_item_layout, parent, false);
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

        IAlbum a = mDataset.get(position);
        holder.titleText.setText(a.getName());
        holder.detailText.setText(a.getAlbumArtist());
        holder.album = a;

        if (!a.getAlbumArtRef().equals("")) {
            requestManager.load(a.getAlbumArtRef())
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .listener(GlidePalette.with(a.getAlbumArtRef())
                            .use(GlidePalette.Profile.VIBRANT)
                            .setPaletteBuilderInterceptor(Palette.Builder::clearFilters)
                            .intoCallBack(palette -> {
                                if (palette != null) {
                                    PaletteUtil.SwatchPair pair =
                                            PaletteUtil.getSwatches(context, palette);
                                    holder.swatchPair = pair;

                                    PaletteUtil.crossfadeBackground(holder.contentRoot, pair.getPrimary());
                                    PaletteUtil.crossfadeTitle(holder.titleText, pair.getPrimary());
                                    PaletteUtil.crossfadeSubtitle(holder.detailText, pair.getPrimary());

                                    if(ColorUtils.isDark(holder.swatchPair.getPrimary().getBodyTextColor())) {
                                        holder.menuButton.setImageTintList(
                                                CarbonPlayerApplication.instance.getDarkCSL());
                                    }
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
