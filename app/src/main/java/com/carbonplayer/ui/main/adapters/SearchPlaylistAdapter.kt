package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.carbonplayer.R
import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.utils.general.MathUtils
import kotlinx.android.synthetic.main.grid_item_layout.view.*

/**
 * Displays list of tracks w/ ranking
 */
internal class SearchPlaylistAdapter(
        private val context: Activity,
        private val mDataset: List<IPlaylist>,
        private val clicked: (IPlaylist) -> Unit,
        private val menuClicked: (View, IPlaylist) -> Unit
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
            v.gridLayoutRoot.setOnClickListener { _ ->
                clicked(mDataset[adapterPosition])
            }
            v.imageButton.setOnClickListener {
                menuClicked(it, mDataset[adapterPosition])
            }
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
    }

    override fun getItemCount(): Int = mDataset.size
}
