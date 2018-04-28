package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.carbonplayer.R
import com.carbonplayer.model.entity.SuggestResponse
import kotlinx.android.synthetic.main.topcharts_song_layout.view.*

/**
 * Displays list of tracks w/ ranking
 */
internal class SuggestionsAdapter(
        private val dataset: List<SuggestResponse.SuggestResponseEntry>,
        private val clicked: (SuggestResponse.SuggestResponseEntry) -> Unit)
    : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

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
                        if(viewType == 1) R.layout.topcharts_song_layout
                        else android.R.layout.simple_list_item_1, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = dataset[position]

        if(holder.itemViewType == 1) {
            holder.itemView.trackName.text = t.entity?.title ?: t.suggestion_string ?: "Unknown"
            holder.itemView.trackRanking.text = (position + 1).toString()
            holder.itemView.artistName.text = t.entity?.subtitle ?: ""
        } else {
            holder.itemView.findViewById<TextView>(android.R.id.text1)
                    .text = t.suggestion_string
        }

    }

    override fun getItemCount(): Int = dataset.size
}
