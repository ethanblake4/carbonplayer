package com.carbonplayer.ui.main.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluelinelabs.conductor.Controller
import com.carbonplayer.R
import com.carbonplayer.model.entity.api.StationCategory
import com.carbonplayer.ui.main.adapters.StationCategoryAdapter
import com.carbonplayer.utils.general.MathUtils
import kotlinx.android.synthetic.main.topcharts_recycler_layout.view.*
import timber.log.Timber

class BrowseStationsTab (
        private val category: StationCategory,
        val callback: (StationCategory) -> Unit
) : Controller() {

    @Keep constructor() : this (StationCategory.DEFAULT, {})

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val view = inflater.inflate(
                R.layout.browse_stations_recycler_layout, container, false)

        view.main_recycler.layoutManager = LinearLayoutManager(inflater.context)

        view.main_recycler.adapter = StationCategoryAdapter(
                category.subcategories ?: listOf()
        ) { (category, _) ->
            callback(category);
            Timber.d("Clicked @ tab")
        }

        Timber.d(category.subcategories?.size.toString())

        view.main_recycler.layoutParams.height = (category.subcategories?.size ?: 0) *
                MathUtils.dpToPx2(container.resources, 50)

        view.main_recycler.isNestedScrollingEnabled = false

        return view
    }

}