package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.artist_list_item.view.*

/**
 * Album / playlist adapter
 */
internal class LinearArtistAdapter(
        private val dataset: List<SkyjamArtist>,
        private val clicked: (Pair<SkyjamArtist, Int>) -> Unit)
    : RecyclerView.Adapter<LinearArtistAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        var name: TextView = v.artistName
        var thumb: CircleImageView = v.artistImage

        var pos: Int = 0

        init {
            v.findViewById<View>(R.id.artistLinearRoot)
                    .setOnClickListener { _ -> clicked(Pair(dataset[pos], pos)) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.artist_list_item, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = dataset[position]

        holder.name.text = t.name

        Glide.with(holder.itemView)
                .load(t.artistArtRef)
                .into(holder.thumb)

        holder.pos = position

    }

    override fun getItemCount(): Int = dataset.size
}
