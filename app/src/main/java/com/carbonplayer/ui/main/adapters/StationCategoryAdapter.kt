package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.carbonplayer.R
import com.carbonplayer.model.entity.api.StationCategory
import kotlinx.android.synthetic.main.station_category.view.*
import timber.log.Timber

/**
 * Album / playlist adapter
 */
internal class StationCategoryAdapter(
        private val dataset: List<StationCategory>,
        private val clicked: (Pair<StationCategory, Int>) -> Unit)
    : RecyclerView.Adapter<StationCategoryAdapter.ViewHolder>() {

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.station_category, parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.stationName.text = dataset[position].display_name
        holder.itemView.stationCategoryLayoutRoot.setOnClickListener {
            Timber.d("Clicked")
            clicked(Pair(dataset[position], position))
        }
    }

    override fun getItemCount(): Int = dataset.size
}
