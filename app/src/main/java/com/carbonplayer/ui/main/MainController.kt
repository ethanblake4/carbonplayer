package com.carbonplayer.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.carbonplayer.R
import kotlinx.android.synthetic.main.controller_main.view.*

/**
 * Created by ethanelshyeb on 8/8/17.
 */
class MainController: Controller() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_main, container, false)

        getChildRouter(view.main_controller_container).run {
            if(!hasRootController())
                setRoot(RouterTransaction.with(AlbumPageController()))
        }

        view.bottom_nav.selectedItemId = R.id.action_home
        return view
    }

}