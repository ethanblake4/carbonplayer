package com.carbonplayer.ui.main.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.recyclerview.widget.GridLayoutManager
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.SkyjamStationAdapter
import kotlinx.android.synthetic.main.topcharts_recycler_layout.view.*

class BrowseStationsPage (
        private val stations: List<SkyjamStation>
) : Controller() {

    @Keep constructor() : this (listOf())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val view = inflater.inflate(
                R.layout.browse_stations_recycler_layout, container, false)

        view.main_recycler.layoutManager = GridLayoutManager(inflater.context, 2)

        view.main_recycler.adapter = SkyjamStationAdapter(
                stations, activity as MainActivity, Glide.with(container)
        )

        view.main_recycler.isNestedScrollingEnabled = false

        return view
    }

}