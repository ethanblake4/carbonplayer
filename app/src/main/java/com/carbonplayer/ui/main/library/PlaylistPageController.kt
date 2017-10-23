package com.carbonplayer.ui.main.library

import android.os.Parcelable
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
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.ui.main.adapters.PlaylistAdapter
import kotlinx.android.synthetic.main.single_recycler_layout.view.*
import rx.Subscription

class PlaylistPageController : Controller() {
    private var playlistSubscription: Subscription? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var requestManager: RequestManager
    var recyclerState: Parcelable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        /*Timber.d("Playlists - onCreateView. savedInstanceState= %s",
                if(savedInstanceState == null) "null" else "not null"*/

        val view = inflater.inflate(R.layout.single_recycler_layout, container, false)
        view.main_recycler.hasFixedSize()

        layoutManager = GridLayoutManager(activity, 2)

        view.main_recycler.layoutManager = layoutManager
        //attachToHandle?.setRecyclerView(view.main_recycler)
        //attachToHandle?.visibility = View.VISIBLE

        requestManager = Glide.with(activity)

        //recyclerState = savedInstanceState?.getParcelable("recycler")

        playlistSubscription?.unsubscribe()
        playlistSubscription = MusicLibrary.getInstance().loadPlaylists()
                .subscribe { playlists ->
                    adapter = PlaylistAdapter(playlists, activity as MainActivity, requestManager)
                    view.main_recycler.adapter = adapter
                    view.fastscroll.setRecyclerView(view.main_recycler)
                    recyclerState?.let {
                        view.main_recycler.layoutManager.onRestoreInstanceState(it)
                    }
                }

        return view
    }

    /*override fun onResume() {
        view.main_recycler.adapter.notifyDataSetChanged()
        super.onResume()

    }*/

    /*override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("recycler", view.main_recycler.layoutManager.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }*/
}