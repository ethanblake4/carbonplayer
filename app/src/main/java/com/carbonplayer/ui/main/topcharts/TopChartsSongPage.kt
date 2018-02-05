package com.carbonplayer.ui.main.topcharts

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.carbonplayer.R
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.TopChartsSongAdapter
import kotlinx.android.synthetic.main.topcharts_recycler_layout.view.*
import timber.log.Timber

class TopChartsSongPage : Controller() {

    private lateinit var musicManager: MusicManager

    var songList: List<SkyjamTrack>? = null
        set(value) {
            field = value
            if( view != null && value != null) {
                setAdapter(view!!)
            }
        }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        musicManager = MusicManager(context as MainActivity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val view = inflater.inflate(R.layout.topcharts_recycler_layout, container, false)
        view.main_recycler.layoutManager = LinearLayoutManager(activity)
        //view.main_recycler.isNestedScrollingEnabled = false
        if(songList != null) setAdapter(view)

        return view

    }

    private fun setAdapter(v: View) {
        Timber.d("setting adapter")
        v.main_recycler.adapter = TopChartsSongAdapter(songList!!, {
            Timber.d("Track at position $it is " +
                    (songList as List<SkyjamTrack>)[it].let { it.storeId + ": " + it.title })
            musicManager.fromTracks(songList!!, it, false)
        })
    }

}