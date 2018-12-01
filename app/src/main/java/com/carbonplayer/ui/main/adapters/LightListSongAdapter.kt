package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.R
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import kotlinx.android.synthetic.main.search_song_layout.view.*

/**
 * Displays list of tracks w/ ranking
 */
internal class LightListSongAdapter(
        private val mDataset: List<ITrack>,
        private val clicked: (ITrack, Int) -> Unit,
        private val menuClicked: (View, ITrack) -> Unit
) : RecyclerView.Adapter<LightListSongAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        init {
            v.songLayoutRoot.setOnClickListener { _ ->
                clicked(mDataset[adapterPosition], adapterPosition)
            }
            v.songLayoutMenu.setOnClickListener {
                menuClicked(it, mDataset[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.light_list_song, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = mDataset[position]
        holder.itemView.trackName.text = t.title
        holder.itemView.artistName.text = t.artist
        //holder.id = SongID(t)

        when (t) {
            is SkyjamTrack -> t.albumArtRef?.firstOrNull()?.url?.let {
                Glide.with(holder.itemView)
                        .load(it)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(holder.itemView.trackThumb)
            }
            is Track -> t.albums?.firstOrNull()?.albumArtRef?.let {
                Glide.with(holder.itemView)
                        .load(it)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(holder.itemView.trackThumb)
            }
            is ParcelableTrack -> t.albumArtURL?.let {
                Glide.with(holder.itemView)
                        .load(it)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(holder.itemView.trackThumb)
            }
        }
    }

    override fun getItemCount(): Int = mDataset.size

    companion object {
        const val SONG_HEIGHT_DP = 57
    }
}
