package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.carbonplayer.R
import com.carbonplayer.model.entity.base.ITrack

/**
 * Album / playlist adapter
 */
internal class SongListAdapter(
        private val mDataset: List<ITrack>,
        private val clicked: (Pair<ITrack, Int>) -> Unit)
    : RecyclerView.Adapter<SongListAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var trackName: TextView = v.findViewById<View>(R.id.trackName) as TextView
        var trackNumber: TextView = v.findViewById<View>(R.id.trackNumber) as TextView
        lateinit var track: ITrack
        var pos: Int = 0

        init {
            v.findViewById<View>(R.id.songLayoutRoot).setOnClickListener { _ -> clicked(Pair(track, pos)) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.song_item_layout, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = mDataset[position]
        holder.trackName.text = t.title
        holder.trackNumber.text = t.trackNumber.toString()
        holder.track = t
        holder.pos = position

    }

    override fun getItemCount(): Int = mDataset.size

    companion object {
        const val SONG_HEIGHT_DP = 60
    }
}
