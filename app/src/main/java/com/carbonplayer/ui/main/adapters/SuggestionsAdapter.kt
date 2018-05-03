package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.R
import com.carbonplayer.model.entity.SuggestResponse
import com.carbonplayer.model.entity.utils.MediaTypeUtil
import kotlinx.android.synthetic.main.search_song_layout.view.*

/**
 * Displays list of tracks w/ ranking
 */
internal class SuggestionsAdapter(
        private val dataset: List<SuggestResponse.SuggestResponseEntry>,
        private val clicked: (SuggestResponse.SuggestResponseEntry) -> Unit,
        private val requestManager: RequestManager
)   : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        init {
            v.songLayoutRoot?.setOnClickListener { _ ->
                clicked(dataset[adapterPosition])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(dataset[position].entity != null) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(
                        if(viewType == 1) R.layout.search_song_layout
                        else android.R.layout.simple_list_item_1, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = dataset[position]

        if(holder.itemViewType == 1) {
            holder.itemView.trackName.text = t.entity?.title ?: t.suggestion_string ?: "Unknown"
            t.entity?.let { when (it.type) {
                MediaTypeUtil.TYPE_SONG -> {
                    it.track?.albumArtRef?.firstOrNull()?.let {
                        requestManager.load(it.url)
                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                                .transition(DrawableTransitionOptions.withCrossFade(200))
                                .into(holder.itemView.trackThumb)
                    }
                }
                MediaTypeUtil.TYPE_ARTIST -> {
                    (it.artist?.artistArtRef ?: it.artist?.artistArtRefs?.firstOrNull()?.url)?.let {
                        requestManager.load(it)
                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                                .transition(DrawableTransitionOptions.withCrossFade(200))
                                .into(holder.itemView.trackThumb)
                    }
                }
                MediaTypeUtil.TYPE_ALBUM -> {
                    it.album?.albumArtRef?.firstOrNull()?.let {
                        requestManager.load(it)
                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                                .transition(DrawableTransitionOptions.withCrossFade(200))
                                .into(holder.itemView.trackThumb)
                    }
                }
                else -> {}
            }}
            holder.itemView.artistName.text = t.entity?.subtitle ?: ""


        } else {
            holder.itemView.findViewById<TextView>(android.R.id.text1)
                    .text = t.suggestion_string
        }

    }

    override fun getItemCount(): Int = dataset.size
}
