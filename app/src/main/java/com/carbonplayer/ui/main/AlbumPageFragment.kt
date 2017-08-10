package com.carbonplayer.ui.main

import android.app.Fragment
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.utils.IdentityUtils
import icepick.Icepick
import icepick.State
import kotlinx.android.synthetic.main.activity_main.view.*
import rx.Subscription
import timber.log.Timber

class AlbumPageFragment : Fragment() {

    private var albumSubscription: Subscription? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var requestManager: RequestManager
    var recyclerState: Parcelable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.activity_main, container, false)
        view.main_recycler.hasFixedSize()

        view.toolbar.setPadding(view.toolbar.paddingLeft, view.toolbar.paddingTop + IdentityUtils.getStatusBarHeight(resources),
                view.toolbar.paddingRight, view.toolbar.paddingBottom)

        //view.toolbar.layoutParams.height += IdentityUtils.getStatusBarHeight(resources)
        (view.toolbar.layoutParams as AppBarLayout.LayoutParams).topMargin += IdentityUtils.getStatusBarHeight(resources) / 2
        (view.toolbar.layoutParams as AppBarLayout.LayoutParams).bottomMargin += IdentityUtils.getStatusBarHeight(resources) / 2

        layoutManager = GridLayoutManager(activity, 2)
        view.main_recycler.layoutManager = layoutManager
        view.main_recycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        requestManager = Glide.with(activity)

        albumSubscription?.unsubscribe()
        albumSubscription = MusicLibrary.getInstance().loadAlbums()
                .subscribe { albums ->
                    adapter = AlbumAdapterJ(albums, activity as MainActivity, requestManager)
                    view.main_recycler.adapter = adapter
                    recyclerState?.let {
                        view.main_recycler.layoutManager.onRestoreInstanceState(it)
                    }
                }

        return view
    }

    fun saveStateForBackstack() {
        recyclerState = view.main_recycler.layoutManager.onSaveInstanceState()
    }
}