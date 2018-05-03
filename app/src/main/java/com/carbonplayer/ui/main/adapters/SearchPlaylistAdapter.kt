package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.utils.general.MathUtils
import kotlinx.android.synthetic.main.grid_item_layout.view.*

/**
 * Displays list of tracks w/ ranking
 */
internal class SearchPlaylistAdapter(
        private val context: Activity,
        private val mDataset: List<SkyjamPlaylist>,
        private val clicked: (SkyjamPlaylist) -> Unit,
        private val menuClicked: (View, SkyjamPlaylist) -> Unit
) : RecyclerView.Adapter<SearchPlaylistAdapter.ViewHolder>() {

    val screenWidthPx: Int

    init {
        val display = context.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        screenWidthPx = size.x
    }

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        init {
            v.primaryText.maxWidth = screenWidthPx / 2 - MathUtils.dpToPx2(context.resources, 50)
            v.detailText.maxWidth = screenWidthPx / 2 - MathUtils.dpToPx2(context.resources, 32)
            /*v.songLayoutRoot.setOnClickListener { _ ->
                clicked(mDataset[adapterPosition])
            }
            v.songLayoutMenu.setOnClickListener {
                menuClicked(it, mDataset[adapterPosition])
            }*/
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.grid_item_layout, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = mDataset[position]
        holder.itemView.primaryText.text = t.name
        holder.itemView.detailText.text = "By ${t.ownerName}"
        //holder.id = SongID(t)
        t.albumArtRef?.first()?.let {
            Glide.with(context)
                    .load(it)
                    .into(holder.itemView.imgthumb)
        }
    }

    override fun getItemCount(): Int = mDataset.size
}
