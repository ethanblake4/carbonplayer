package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.carbonplayer.R
import com.carbonplayer.model.entity.SearchResponse
import com.carbonplayer.model.entity.utils.MediaTypeUtil
import kotlinx.android.synthetic.main.search_cluster.view.*
import timber.log.Timber

/**
 * Displays list of tracks w/ ranking
 */
internal class SearchClusterAdapter(
        private val dataset: List<SearchResponse.ClusterDetail>
): RecyclerView.Adapter<SearchClusterAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {

        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.search_cluster, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cluster = dataset[position]

        holder.itemView.clusterTitle.text = cluster.displayName ?: cluster.cluster?.type?.let {
            MediaTypeUtil.getMediaTypeString(holder.itemView.context.resources, it) + "s"
        } ?: holder.itemView.context.getString(R.string.media_type_unknown)
    }

    override fun getItemCount(): Int {
        Timber.d("Number of items: ${dataset.size}")
        return dataset.size
    }
}
