package com.carbonplayer.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import com.bluelinelabs.conductor.Controller
import com.carbonplayer.R

class LoadingController @Keep constructor(): Controller() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.controller_loading, container, false)
    }
}