package com.carbonplayer.ui.main

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import kotlinx.android.synthetic.main.activity_main.view.*
import rx.Subscription

class AlbumPageController : Controller() {

    private var albumSubscription: Subscription? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var requestManager: RequestManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val view = inflater.inflate(R.layout.activity_main, container, false)
        view.main_recycler.hasFixedSize()

        layoutManager = GridLayoutManager(activity, 2)
        view.main_recycler.layoutManager = layoutManager

        requestManager = Glide.with(activity)


        albumSubscription?.unsubscribe()
        albumSubscription = MusicLibrary.getInstance().loadAlbums()
                .subscribe { albums ->
                    adapter = AlbumAdapterJ(albums, activity as MainActivity, requestManager)
                    view.main_recycler.adapter = adapter
                }

        return view
    }

}