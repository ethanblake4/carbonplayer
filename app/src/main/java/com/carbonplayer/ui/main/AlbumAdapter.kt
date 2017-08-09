package com.carbonplayer.ui.main

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Album
import com.carbonplayer.utils.MathUtils
import com.github.florent37.glidepalette.BitmapPalette
import com.github.florent37.glidepalette.GlidePalette

import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.utils.ColorUtils
import kotlinx.android.synthetic.main.grid_item_layout.view.*
import timber.log.Timber

/**
 * Displays albums in variable-size grid view
 */

internal class AlbumAdapter (
        private val mDataset: List<Album>,
        private val context: MainActivity,
        private val requestManager: RequestManager)
    : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    private val screenWidthPx: Int

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var songs: String? = null
        var album: Album? = null
        var size: Int = 0

        init {

            v.gridLayoutRoot.setOnClickListener { view ->
                //Toast.makeText(MainActivity.this, songs, Toast.LENGTH_SHORT).show();
                CarbonPlayerApplication.instance.currentAlbum = album
                val i = Intent(context, AlbumActivity::class.java)

                val options = ActivityOptions
                        .makeSceneTransitionAnimation(context,
                                Pair.create<View, String>(v.imgthumb, "imgthumb"),
                                Pair.create<View, String>(v.gridLayoutContentRoot, "albumdetails"),
                                Pair.create<View, String>(v.primaryText, "albumName"),
                                Pair.create<View, String>(v.detailText, "artistName"))

                context.startActivity(i, options.toBundle())
            }

            v.imgthumb.viewTreeObserver.addOnPreDrawListener {
                size = v.imgthumb.measuredWidth
                true
            }

            v.primaryText.maxWidth = screenWidthPx / 2 - MathUtils.dpToPx(context, 50)
            v.detailText.maxWidth = screenWidthPx / 2 - MathUtils.dpToPx(context, 32)
        }
    }

    init {
        val display = context.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        screenWidthPx = size.x
    }

    override fun onViewRecycled(v: ViewHolder?) {
        v?.let { requestManager.clear(it.itemView.imgthumb) }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): AlbumAdapter.ViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.grid_item_layout, parent, false)
        // set the view's size, margins, paddings and layout parameters

        val vh = ViewHolder(v)
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        @ColorInt val defaultColor = Color.parseColor("#292929")
        @ColorInt val defaultTextColor = Color.parseColor("#cccccc")
        holder.itemView.gridLayoutRoot.setBackgroundColor(defaultColor)
        holder.itemView.primaryText.setTextColor(defaultTextColor)
        holder.itemView.detailText.setTextColor(defaultTextColor)

        val a = mDataset[position]
        holder.itemView.primaryText.text = a.title
        holder.itemView.detailText.text = a.artist
        holder.album = a



        if (a.albumArtURL != "") {

            requestManager.load(a.albumArtURL)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .listener(GlidePalette.with(a.albumArtURL)
                            .use(0)
                            .intoBackground(holder.itemView.gridLayoutRoot)
                            .intoTextColor(holder.itemView.primaryText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                            .intoTextColor(holder.itemView.detailText, BitmapPalette.Swatch.BODY_TEXT_COLOR)
                            .crossfade(true, 500))
                    .into(holder.itemView.imgthumb)

        } else {
            requestManager.clear(holder.itemView.imgthumb)
            holder.itemView.imgthumb.setImageResource(R.drawable.unknown_music_track)
        }

        val sb = StringBuilder()
        val songs = a.songIds
        for (string in songs) {
            sb.append(MusicLibrary.getInstance().getTrack(string.toString()).title)
            sb.append(", ")
        }
        holder.songs = sb.toString()

    }

    override fun getItemCount(): Int = mDataset.size
}