package com.carbonplayer.ui.main.library

import android.content.Context
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
import com.carbonplayer.ui.main.adapters.ArtistAdapter
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.single_recycler_layout.view.*

class ArtistsPageController : Controller() {

    private var artistSubscription: Disposable? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var requestManager: RequestManager
    var recyclerState: Parcelable? = null

    var subscribed = false;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        //Timber.d("Artists - onCreateView. savedInstanceState= %s",
        //        if(savedInstanceState == null) "null" else "not null")

        val view = inflater.inflate(R.layout.single_recycler_layout, container, false)
        view.main_recycler.hasFixedSize()

        layoutManager = GridLayoutManager(activity, 2)

        view.main_recycler.layoutManager = layoutManager

        requestManager = Glide.with(activity)

        //recyclerState = savedInstanceState?.getParcelable("recycler")

        view.main_recycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                //(activity as MainActivity).scrollCb(dy)
            }
        })

        if(activity != null && !subscribed) resubscribe(view)

        return view
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)

        if(view != null && !subscribed) resubscribe(view!!)
    }

    fun resubscribe(view: View) {
        artistSubscription?.dispose()
        artistSubscription = MusicLibrary.loadArtists()
                .subscribe { artists ->
                    adapter = ArtistAdapter(artists, activity as MainActivity, requestManager)
                    view.main_recycler.adapter = adapter
                    view.fastscroll.setRecyclerView(view.main_recycler)
                    recyclerState?.let {
                        view.main_recycler.layoutManager.onRestoreInstanceState(it)
                    }
                }
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