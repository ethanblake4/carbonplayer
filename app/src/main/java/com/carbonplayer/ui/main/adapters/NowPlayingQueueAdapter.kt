package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.R
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.ui.widget.helpers.ItemTouchHelperAdapter
import kotlinx.android.synthetic.main.song_item_queue.view.*
import java.util.*

/**
 * Album / playlist adapter
 */
internal class NowPlayingQueueAdapter(
        var dataset: List<ITrack>,
        private val clicked: (Pair<ITrack, Int>) -> Unit,
        private val handlePressed: (ViewHolder) -> Unit,
        private val itemRemoved: (Int) -> Unit,
        private val itemReordered: (Int, Int) -> Unit
)
    : RecyclerView.Adapter<NowPlayingQueueAdapter.ViewHolder>(), ItemTouchHelperAdapter {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var trackName: TextView = v.findViewById<View>(R.id.trackName) as TextView
        var pos: Int = 0

        init {
            v.findViewById<View>(R.id.songLayoutRoot).setOnClickListener { _ ->
                clicked(Pair(dataset[pos], pos))
            }
            v.song_item_handle.setOnTouchListener { _, ev ->
                if(ev.actionMasked == MotionEvent.ACTION_DOWN){
                    handlePressed(this)
                }
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(ContextThemeWrapper(parent.context, R.style.DarkRippleTheme))
                .inflate(R.layout.song_item_queue, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = dataset[position]
        holder.trackName.text = t.title
        holder.itemView.artistName.text = t.artist
        when (t) {
            is SkyjamTrack -> t.albumArtRef?.first()?.url?.let {
                Glide.with(holder.itemView)
                        .load(it)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(holder.itemView.trackThumb)
            }
            is Track -> t.albums?.first()?.albumArtRef?.let {
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
        holder.pos = position

    }

    override fun onItemDismiss(position: Int) {
        if(dataset is MutableList) {
            (dataset as MutableList).removeAt(position)
        } else {
            dataset = dataset.toMutableList().apply {
                removeAt(position)
            }
        }
        notifyItemRemoved(position)
        itemRemoved(position)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(dataset, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(dataset, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        itemReordered(fromPosition, toPosition)
    }

    override fun getItemCount(): Int = dataset.size
}
