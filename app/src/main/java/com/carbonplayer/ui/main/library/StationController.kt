package com.carbonplayer.ui.main.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.carbonplayer.R
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.utils.ui.PaletteUtil

class StationController(
        val station: SkyjamStation?,
        val swatchPair: PaletteUtil.SwatchPair
) : Controller() {

    constructor() : this(null, PaletteUtil.DEFAULT_SWATCH_PAIR)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {


        return inflater.inflate(R.layout.activity_songgroup, container, false)


    }

}