package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.SongID
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import kotlinx.android.synthetic.main.topcharts_song_layout.view.*

/**
 * Displays list of tracks w/ ranking
 */
internal class TopChartsSongAdapter(
        private val mDataset: List<SkyjamTrack>,
        private val clicked: (SongID) -> Unit)
    : RecyclerView.Adapter<TopChartsSongAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        //dlateinit var id: SongID

        init {
            //v.songLayoutRoot.setOnClickListener { _ -> clicked(id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.topcharts_song_layout, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = mDataset[position]
        holder.itemView.trackName.text = t.title
        holder.itemView.trackRanking.text = (position + 1).toString()
        holder.itemView.artistName.text = t.artist
        //holder.id = SongID(t)

        t.albumArtRef?.first()?.url?.let {
            Glide.with(holder.itemView)
                    .load(it)
                    .into(holder.itemView.trackThumb)
        }



    }

    override fun getItemCount(): Int = mDataset.size
}
