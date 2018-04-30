package com.carbonplayer.ui.main.topcharts

import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.TopChartsAlbumAdapter
import kotlinx.android.synthetic.main.topcharts_recycler_layout.view.*
import timber.log.Timber

class TopChartsAlbumPage : Controller() {

    var albumList: List<SkyjamAlbum>? = null
        set(value) {
            field = value
            if( view != null && value != null) {
                setAdapter(view!!)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val view = inflater.inflate(R.layout.topcharts_recycler_layout, container, false)
        view.main_recycler.layoutManager = GridLayoutManager(activity, 2)
        if(albumList != null) setAdapter(view)

        return view

    }

    private fun setAdapter(v: View) {
        Timber.d("setting adapter")
        v.main_recycler.adapter = TopChartsAlbumAdapter(albumList!!, activity as MainActivity,
                Glide.with(activity as MainActivity))
    }

}