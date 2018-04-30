package com.carbonplayer.ui.main.adapters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.carbonplayer.R;
import com.carbonplayer.model.entity.Playlist;
import com.carbonplayer.model.entity.PlaylistEntry;
import com.carbonplayer.model.entity.Track;
import com.carbonplayer.ui.main.MainActivity;
import com.carbonplayer.ui.widget.fastscroll.SectionTitleProvider;
import com.carbonplayer.utils.general.MathUtils;
import com.carbonplayer.utils.ui.PaletteUtil;
import com.github.florent37.glidepalette.GlidePalette;
import com.google.common.util.concurrent.Atomics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;

/**
 * Displays albums in variable-size grid view
 */

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder>
        implements SectionTitleProvider {

    private List<Playlist> dataset;
    private MainActivity context;
    private Realm realm;
    private int screenWidthPx;
    private RequestManager requestManager;

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.gridLayoutRoot) View layoutRoot;
        @BindView(R.id.gridLayoutContentRoot) View contentRoot;
        @BindView(R.id.imgthumb) ImageView thumb;
        @BindView(R.id.primaryText) TextView titleText;
        @BindView(R.id.detailText) TextView detailText;
        Playlist playlist;
        PaletteUtil.SwatchPair swatchPair;
        int size;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);


            layoutRoot.setOnClickListener(view -> {

                thumb.setTransitionName(playlist.getId() + "i");
                contentRoot.setTransitionName(playlist.getId() + "cr");
                titleText.setTransitionName(playlist.getId() + "t");
                detailText.setTransitionName(playlist.getId() + "d");


            });

            /*ViewTreeObserver vto = thumb.getViewTreeObserver();
            vto.addOnPreDrawListener(() -> {
                size = thumb.getMeasuredWidth();
                ((FrameLayout.LayoutParams) contentRoot.getLayoutParams())
                        .setMargins(0, size, 0, 0);
                contentRoot.postInvalidate();
                return true;
            });*/

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
        this.realm = Realm.getDefaultInstance();
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

        if (playlist.getAlbumArtRef() != null && playlist.getAlbumArtRef().size() > 0
                && playlist.getAlbumArtRef().first() != null
                && !playlist.getAlbumArtRef().first().getUrl().equals("")) {
            requestManager.load(playlist.getAlbumArtRef().first().getUrl())
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .listener(
                            GlidePalette.with(playlist.getAlbumArtRef().first().getUrl())
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
                                    .crossfade(true, 500)
                    )
                    .into(holder.thumb);

        } else {
            List<String> artUrls = new ArrayList<>();
            for(PlaylistEntry p: playlist.getEntries()) {
                Track t;
                t = p.getTrack();
                if(t == null) {
                    t = realm.where(Track.class)
                            .equalTo(Track.STORE_ID, p.getTrackId())
                            .or().equalTo(Track.TRACK_ID, p.getTrackId())
                            .findFirst();
                }
                if(t==null) continue;
                if(t.getAlbumArtURL() == null) continue;
                if(!artUrls.contains(t.getAlbumArtURL())) artUrls.add(t.getAlbumArtURL());
                if(artUrls.size() == 4) break;

            }

            int numArts = Math.min(4, artUrls.size());

            final AtomicReference<Bitmap> bit1 = Atomics.newReference();
            final AtomicReference<Bitmap> bit2 = Atomics.newReference();
            final AtomicReference<Bitmap> bit3 = Atomics.newReference();

            for(String url: artUrls.subList(0, numArts)) {
                requestManager
                        .asBitmap()
                        .load(url)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                if(bit1.get() == null) {
                                    bit1.set(resource);
                                    if(numArts < 2) {
                                        combineBitmaps(resource, resource, resource, resource, holder.thumb);
                                    }
                                } else if (bit2.get() == null) {
                                    bit2.set(resource);
                                    if(numArts < 3) {
                                        Bitmap bit1b = bit1.get();
                                        combineBitmaps(bit1b, resource, bit1b, resource, holder.thumb);
                                    }
                                } else if(bit3.get() == null) {
                                    bit3.set(resource);
                                    if(numArts < 4) {
                                        Bitmap bit1b = bit1.get();
                                        Bitmap bit2b = bit2.get();
                                        combineBitmaps(bit1b, bit2b, resource, bit2b, holder.thumb);
                                    }
                                } else {
                                    combineBitmaps(bit1.get(), bit2.get(), bit3.get(), resource, holder.thumb);
                                }
                            }
                        });

            }
        }
    }

    public void combineBitmaps(Bitmap b1, Bitmap b2, Bitmap b3, Bitmap b4, ImageView view) {
        Bitmap big = Bitmap.createBitmap(b1.getWidth() * 2, b2.getHeight() * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(big);
        canvas.drawBitmap(b1, 0, 0, null);
        canvas.drawBitmap(b2, b1.getWidth(), 0, null);
        canvas.drawBitmap(b3, 0, b1.getHeight(), null);
        canvas.drawBitmap(b4, b1.getWidth(), b1.getHeight(), null);
        //new DrawableCrossFadeTransition()
        view.setImageBitmap(big);
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
