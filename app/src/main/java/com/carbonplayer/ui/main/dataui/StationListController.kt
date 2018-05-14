package com.carbonplayer.ui.main.dataui

import android.support.annotation.Keep
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.SkyjamStationAdapter
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.ui.PaletteUtil
import kotlinx.android.synthetic.main.controller_single_recycler.view.*

class StationListController(
        val stationList: List<SkyjamStation>,
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

        root.app_bar.addOnOffsetChangedListener({ _, i ->
            (activity as MainActivity).scrollCb(i)
        })

        root.fastscroll.setRecyclerView(root.main_recycler)

        root.main_recycler.layoutManager = GridLayoutManager(activity, 2)
        root.main_recycler.adapter = SkyjamStationAdapter(
                stationList,
                activity as MainActivity,
                Glide.with(activity!!)
        )

        root.main_recycler.setPadding(0, 0, 0,
                IdentityUtils.getNavbarHeight(resources) +
                        (activity as MainActivity).bottomInset)

        return root

    }

}