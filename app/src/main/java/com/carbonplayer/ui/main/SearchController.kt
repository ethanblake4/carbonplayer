package com.carbonplayer.ui.main

import android.content.Context
import android.support.annotation.Keep
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.R
import com.carbonplayer.model.entity.SearchResponse
import com.carbonplayer.model.entity.utils.MediaTypeUtil
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.main.adapters.LinearArtistAdapter
import com.carbonplayer.ui.main.adapters.TopChartsAlbumAdapter
import com.carbonplayer.ui.main.adapters.TopChartsSongAdapter
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.controller_search.view.*
import kotlinx.android.synthetic.main.search_cluster.view.*
import timber.log.Timber

class SearchController(
        val searchTerm: String
) : Controller() {

    @Keep
    constructor() : this("")

    lateinit var requestManager: RequestManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val view = inflater.inflate(R.layout.controller_search, container, false)
        view.searchHeader.setPadding(0,
                view.searchHeader.paddingTop +
                        IdentityUtils.getStatusBarHeight(resources) +
                        MathUtils.dpToPx2(resources, 56), 0, 0)

        view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight,
                view.paddingBottom + IdentityUtils.getNavbarHeight(view.resources))

        view.searchHeader.text =
                resources?.getString(R.string.search_header, searchTerm) ?:
                "Results for \"$searchTerm\""

        requestManager = Glide.with(view)

        runSearch(view, searchTerm)

        return view
    }

    fun runSearch(view: View, term: String) {
        Protocol.search(activity!!, term)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ results ->
                    view.searchLoader.visibility = View.GONE
                    if(results == null) {
                        err()
                        Timber.d("Null results")
                        return@subscribe
                    }
                    if(results.suggestedQuery != null) {
                        view.suggestedContainer.visibility = View.VISIBLE
                        view.suggestedQueryText.text = results.suggestedQuery
                        view.suggestedQueryText.setOnClickListener {
                            runSearch(view, results.suggestedQuery)
                        }
                    }

                    val pr = results.clusterDetail?.filter {
                        it.entries != null && it.entries.isNotEmpty()
                    }?.mapNotNull { dt ->
                        dt.cluster?.let {
                            it.type to dt
                        }
                    }?.toMap() ?: mapOf()

                    Timber.d(results.clusterOrder.toString())

                    val buildView = { cDetail: SearchResponse.ClusterDetail ->
                        val v = activity!!.layoutInflater.inflate(R.layout.search_cluster,
                                view.searchResults, false)
                        v.clusterTitle.text =
                                if(cDetail.cluster?.category == 2)
                                    view.resources.getString(R.string.media_type_best_match) else
                                    cDetail.displayName ?: cDetail.cluster?.type?.let {
                                        MediaTypeUtil.getMediaTypeString(view.resources, it) + "s"
                                    } ?: resources?.getString(R.string.media_type_unknown) ?: "Unknown"
                        v.clusterEntries.layoutManager = when(cDetail.cluster?.type) {
                            MediaTypeUtil.TYPE_SONG -> LinearLayoutManager(activity)
                            MediaTypeUtil.TYPE_ALBUM -> GridLayoutManager(activity, 2)
                            MediaTypeUtil.TYPE_ARTIST -> LinearLayoutManager(activity)
                            MediaTypeUtil.TYPE_PLAYLIST -> GridLayoutManager(activity, 2)
                            MediaTypeUtil.TYPE_GENRE -> GridLayoutManager(activity, 2)
                            MediaTypeUtil.TYPE_SITUATION -> GridLayoutManager(activity, 2)
                            MediaTypeUtil.TYPE_STATION -> GridLayoutManager(activity, 2)
                            MediaTypeUtil.TYPE_PODCAST -> GridLayoutManager(activity, 2)
                            MediaTypeUtil.TYPE_VIDEO -> GridLayoutManager(activity, 2)
                            else -> GridLayoutManager(activity, 2)
                        }
                        cDetail.entries?.let { entries ->
                            v.clusterEntries.adapter = when (cDetail.cluster?.type) {
                                MediaTypeUtil.TYPE_SONG -> TopChartsSongAdapter(
                                        entries.mapNotNull { it.track }.take(5), {}
                                )
                                MediaTypeUtil.TYPE_ALBUM -> TopChartsAlbumAdapter(
                                        entries.mapNotNull { it.album }.take(4),
                                        activity as MainActivity,
                                        requestManager
                                )
                                MediaTypeUtil.TYPE_ARTIST -> LinearArtistAdapter(
                                        entries.mapNotNull { it.artist }.take(5),
                                        {}
                                )
                                else -> null
                            }
                        }
                        view.searchResults.addView(v)
                    }

                    results.clusterOrder?.mapNotNull { pr[it] }?.forEach (buildView) ?:
                            pr.forEach { buildView(it.value) }


                }, { err ->
                    Timber.e(err)
                    err()
                    view.searchLoader.visibility = View.GONE
                })
    }
    fun err() {
        Toast.makeText(activity, R.string.error_search, Toast.LENGTH_LONG).show()
    }



    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)

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
