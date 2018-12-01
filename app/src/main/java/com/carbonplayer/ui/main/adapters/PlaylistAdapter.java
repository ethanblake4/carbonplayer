package com.carbonplayer.ui.main.adapters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.RectF;
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

import androidx.annotation.ColorInt;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

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
        RealmResults<Track> tracks;
        List<String> artUrls;
        PaletteUtil.SwatchPair swatchPair;
        int size;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);

            layoutRoot.setOnClickListener(view ->
                    context.gotoPlaylist(playlist, thumb.getDrawable(),
                            PaletteUtil.INSTANCE.getDEFAULT_SWATCH_PAIR()));

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
        v.thumb.setImageBitmap(null);
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
        holder.detailText.setText(playlist.getEntries().size() + " songs");
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
                                    .setPaletteBuilderInterceptor(Palette.Builder::clearFilters)
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

            holder.artUrls = new ArrayList<>();

            List<String> trackIds = new ArrayList<>();

            for (PlaylistEntry p : playlist.getEntries()) {
                trackIds.add(p.getTrackId());
            }

            if(trackIds.isEmpty()) {
                holder.thumb.setImageResource(R.drawable.unknown_music_track);
                return;
            }

            String[] trackIdArray = trackIds.toArray(new String[0]);

            holder.tracks = realm.where(Track.class)
                    .in(Track.TRACK_ID, trackIdArray)
                    .or()
                    .in(Track.STORE_ID, trackIdArray)
                    .findAllAsync();

            if (!holder.tracks.isLoaded()) {
                holder.tracks.addChangeListener(xTracks -> {
                    for (Track t : xTracks) {
                        String a = t.getAlbumArtURL();
                        if (a != null) holder.artUrls.add(a);
                        if (holder.artUrls.size() == 4) break;
                    }
                    loadArts(holder.artUrls, holder.thumb);
                });
            } else {
                for (Track t : holder.tracks) {
                    String a = t.getAlbumArtURL();
                    if (a != null) holder.artUrls.add(a);
                    if (holder.artUrls.size() == 4) break;
                }

                loadArts(holder.artUrls, holder.thumb);
            }

            Timber.d("after iterate");
        }
    }

    private void loadArts(List<String> artUrls, ImageView view) {

        if(artUrls.isEmpty()) {
            view.setImageResource(R.drawable.unknown_music_track);
            return;
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
                                combineAndSet(resource, resource, resource, resource, view);
                            }
                        } else if (bit2.get() == null) {
                            bit2.set(resource);
                            if(numArts < 3) {
                                Bitmap bit1b = bit1.get();
                                combineAndSet(bit1b, resource, bit1b, resource, view);
                            }
                        } else if(bit3.get() == null) {
                            bit3.set(resource);
                            if(numArts < 4) {
                                Bitmap bit1b = bit1.get();
                                Bitmap bit2b = bit2.get();
                                combineAndSet(bit1b, bit2b, resource, bit2b, view);
                            }
                        } else {
                            combineAndSet(bit1.get(), bit2.get(), bit3.get(), resource, view);
                        }
                    }
                });
        }
    }

    private void combineAndSet(Bitmap b1, Bitmap b2, Bitmap b3, Bitmap b4, ImageView view) {
        Bitmap big = Bitmap.createBitmap(b1.getWidth(), b1.getWidth(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(big);

        float half = b1.getWidth()/2f;

        RectF rect1 = new RectF(0, 0, half, half);
        RectF rect2 = new RectF(half, 0, half*2, half);
        RectF rect3 = new RectF(0, half, half, half*2);
        RectF rect4 = new RectF(half, half, half*2, half*2);

        canvas.drawBitmap(b1, null, rect1, null);
        canvas.drawBitmap(b2, null, rect2, null);
        canvas.drawBitmap(b3, null, rect3, null);
        canvas.drawBitmap(b4, null, rect4, null);

        view.setImageBitmap(big);
        view.setAlpha(0.0f);
        view.animate().alpha(1.0f).start();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public String getSectionTitle(int position) {
        return dataset.get(position).getName().substring(0, 1).toUpperCase();
    }
}
