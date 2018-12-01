package com.carbonplayer.ui.main.dataui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.recyclerview.widget.GridLayoutManager
import com.bluelinelabs.conductor.Controller
import com.carbonplayer.R
import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.SearchPlaylistAdapter
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.controller_single_recycler.view.*

class PlaylistListController(
        val playlistList: List<IPlaylist>,
        val swatchPair: PaletteUtil.SwatchPair
) : Controller() {

    @Keep @Suppress("unused")
    constructor() : this(listOf(), PaletteUtil.DEFAULT_SWATCH_PAIR)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

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

        root.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, i ->
            (activity as MainActivity).scrollCb(i)
        })

        root.fastscroll.setRecyclerView(root.main_recycler)

        root.main_recycler.layoutManager = GridLayoutManager(activity, 2)
        root.main_recycler.adapter = SearchPlaylistAdapter(
                activity as MainActivity,
                playlistList,
                { playlist -> (activity as MainActivity).gotoPlaylist(playlist, null,
                            PaletteUtil.DEFAULT_SWATCH_PAIR)
                },
                { v, playlist -> (activity as MainActivity).showPlaylistPopup(v, playlist) }
        )

        root.main_recycler.setPadding(0, 0, 0,
                IdentityUtils.getNavbarHeight(resources) +
                        (activity as MainActivity).bottomInset)

        return root

    }

}