package com.carbonplayer.ui.main.dataui

import android.os.Bundle
import android.support.annotation.Keep
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.carbonplayer.R
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.Track
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.SearchSongAdapter
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.ui.PaletteUtil
import kotlinx.android.synthetic.main.controller_single_recycler.view.*

class SongListController(
        val songList: List<ITrack>,
        val swatchPair: PaletteUtil.SwatchPair
) : Controller() {

    lateinit var musicManager: MusicManager

    @Keep @Suppress("unused")
    constructor(bundle: Bundle) : this(
            (bundle.getParcelableArray("tracks") as Array<ParcelableTrack>).toList(),
            PaletteUtil.DEFAULT_SWATCH_PAIR)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        musicManager = MusicManager(activity as MainActivity)

        val root = inflater.inflate(R.layout.controller_single_recycler, container,
                false)

        root.toolbar.setPadding(root.toolbar.paddingLeft, root.toolbar.paddingTop +
                IdentityUtils.getStatusBarHeight(resources),
                root.toolbar.paddingRight, root.toolbar.paddingBottom)

        //view.toolbar.layoutParams.height += IdentityUtils.getStatusBarHeight(resources)
        (root.toolbar.layoutParams as AppBarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2
        (root.toolbar.layoutParams as AppBarLayout.LayoutParams).bottomMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2

        root.app_bar.addOnOffsetChangedListener({ _, i ->
            (activity as MainActivity).scrollCb(i)
        })

        root.fastscroll.setRecyclerView(root.main_recycler)

        root.main_recycler.layoutManager = LinearLayoutManager(activity)
        root.main_recycler.adapter = SearchSongAdapter(
                songList,
                { pos -> musicManager.fromTracks(songList, pos, songList.first() is Track) },
                { v, track -> (activity as MainActivity).showTrackPopup(v, track) }

        )

        return root

    }

}