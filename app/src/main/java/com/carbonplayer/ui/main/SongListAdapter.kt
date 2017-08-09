package com.carbonplayer.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import com.carbonplayer.R
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.entity.SongID
import java.util.function.Consumer

import timber.log.Timber

/**
 * Album / playlist adapter
 */
internal class SongListAdapter(
        private val mDataset: List<MusicTrack>,
        private val clicked: (SongID) -> Unit)
    : RecyclerView.Adapter<SongListAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var trackName: TextView = v.findViewById(R.id.trackName) as TextView
        var trackNumber: TextView = v.findViewById(R.id.trackNumber) as TextView
        lateinit var id: SongID

        init {
            v.findViewById(R.id.songLayoutRoot).setOnClickListener { _ -> clicked(id)}
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): SongListAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.song_item_layout, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SongListAdapter.ViewHolder, position: Int) {
        val t = mDataset[position]
        holder.trackName.text = t.title
        if (t.trackNumber != null) holder.trackNumber.text = t.trackNumber!!.toString()
        holder.id = SongID(t)

    }

    override fun getItemCount(): Int = mDataset.size
}
