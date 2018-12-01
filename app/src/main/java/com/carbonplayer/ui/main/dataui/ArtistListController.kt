package com.carbonplayer.ui.main.dataui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluelinelabs.conductor.Controller
import com.carbonplayer.R
import com.carbonplayer.model.entity.base.IArtist
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.LinearArtistAdapter
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.controller_single_recycler.view.*

class ArtistListController(
        private val artistList: List<IArtist>,
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

        root.main_recycler.layoutManager = LinearLayoutManager(activity)
        root.main_recycler.adapter = LinearArtistAdapter(
                activity!!,
                artistList,
                { (artist, swPair) -> (activity as MainActivity).gotoArtist(
                        artist, swPair ?: PaletteUtil.DEFAULT_SWATCH_PAIR
                ) }
        )

        root.main_recycler.setPadding(0, 0, 0,
                IdentityUtils.getNavbarHeight(resources) +
                        (activity as MainActivity).bottomInset)

        return root

    }

}