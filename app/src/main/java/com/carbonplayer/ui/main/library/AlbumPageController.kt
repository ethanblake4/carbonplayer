package com.carbonplayer.ui.main.library

//import kotlinx.android.synthetic.main.activity_main.view.*
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
import com.carbonplayer.ui.main.adapters.AlbumAdapterJ
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.single_recycler_layout.view.*
import timber.log.Timber

/* Displays a list of albums */
class AlbumPageController : Controller() {

    private var albumSubscription: Disposable? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var requestManager: RequestManager
    var recyclerState: Parcelable? = null

    var subscribed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        //Timber.d("Album - onCreateView. savedInstanceState= %s", if(savedInstanceState == null) "null" else "not null")

        val view = inflater.inflate(R.layout.single_recycler_layout, container, false)
        view.main_recycler.hasFixedSize()

        layoutManager = GridLayoutManager(activity, 2)

        view.main_recycler.layoutManager = layoutManager

        requestManager = Glide.with(activity)

        /*recyclerState =
                (savedInstanceState?.getParcelable("recycler") ?: recyclerState)*/

        view.main_recycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                //(activity as MainActivity).scrollCb(dy)
            }
        })

        if(activity != null && !subscribed) resubscribe(view)

        return view
    }

    /*override fun onPause() {
        super.onPause()
        Timber.d("album: onPause")
    }*/

    override fun onDestroyView(view: View) {
        recyclerState = view.main_recycler.layoutManager.onSaveInstanceState()
        super.onDestroyView(view)
        Timber.d("album: destroyview")
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)

        if(view != null && !subscribed) resubscribe(view!!)

    }

    fun resubscribe(view: View) {

        albumSubscription?.dispose()
        albumSubscription = MusicLibrary.loadAlbums()
                .subscribe { albums ->
                    adapter = AlbumAdapterJ(albums, activity as MainActivity, requestManager)
                    view.main_recycler.adapter = adapter
                    view.fastscroll.setRecyclerView(view.main_recycler)
                    recyclerState?.let {
                        view.main_recycler.layoutManager.onRestoreInstanceState(it)
                    }
                }
        subscribed = true
    }

    /*override fun onResume() {
        //view.main_recycler.adapter.notifyDataSetChanged()
        super.onResume()

    }*/

    /*override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("Album - onSaveInstanceState = saving self state")
        outState.putParcelable("recycler", view.main_recycler.layoutManager.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }*/
}