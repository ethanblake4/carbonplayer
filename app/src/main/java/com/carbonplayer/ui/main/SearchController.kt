package com.carbonplayer.ui.main

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
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.adapters.*
import com.carbonplayer.ui.main.dataui.AlbumListController
import com.carbonplayer.ui.main.dataui.SongListController
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.ui.PaletteUtil
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
    lateinit var musicManager: MusicManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        musicManager = MusicManager(activity as MainActivity)

        val view = inflater.inflate(R.layout.controller_search, container, false)
        view.searchHeader.setPadding(0,
                view.searchHeader.paddingTop +
                        IdentityUtils.getStatusBarHeight(resources) +
                        MathUtils.dpToPx2(resources, 56), 0, 0)

        view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight,
                view.paddingBottom + IdentityUtils.getNavbarHeight(view.resources) +
                        (activity as MainActivity).bottomInset)

        view.searchHeader.text =
                resources?.getString(R.string.search_header, searchTerm) ?:
                "Results for \"$searchTerm\""

        requestManager = Glide.with(view)

        runSearch(view, searchTerm)

        return view
    }

    private fun runSearch(view: View, term: String) {
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
                            clearForNext(view)
                            view.suggestedContainer.visibility = View.GONE
                            view.searchLoader.visibility = View.VISIBLE
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
                                MediaTypeUtil.TYPE_SONG -> SearchSongAdapter(
                                        entries.mapNotNull { it.track }.take(5),
                                        { track, pos ->
                                            musicManager.fromTracks(entries.mapNotNull { it.track },
                                                    pos)
                                        },
                                        { v, track -> (activity as MainActivity)
                                                .showTrackPopup(v, track)
                                        }
                                )
                                MediaTypeUtil.TYPE_ALBUM -> TopChartsAlbumAdapter(
                                        entries.mapNotNull { it.album }.take(4),
                                        activity as MainActivity,
                                        requestManager
                                )
                                MediaTypeUtil.TYPE_ARTIST -> LinearArtistAdapter(
                                        activity!!,
                                        entries.mapNotNull { it.artist }.take(5),
                                        { (artist, swPair) ->
                                            (activity as MainActivity).gotoArtist(artist,
                                                    swPair ?: PaletteUtil.DEFAULT_SWATCH_PAIR)
                                        }
                                )
                                MediaTypeUtil.TYPE_STATION -> SkyjamStationAdapter(
                                        entries.mapNotNull { it.station }.take(4),
                                        activity as MainActivity,
                                        requestManager
                                )
                                MediaTypeUtil.TYPE_PLAYLIST -> SearchPlaylistAdapter(
                                        activity!!,
                                        entries.mapNotNull { it.playlist }.take(4),
                                        { playlist -> },
                                        { v, playlist -> }
                                )
                                else -> null
                            }
                        }

                        v.see_all.setOnClickListener {
                            cDetail.entries?.let { entries -> when (cDetail.cluster?.type) {
                                MediaTypeUtil.TYPE_SONG -> {
                                    (activity as MainActivity).goto(SongListController(
                                            entries.mapNotNull { it.track },
                                            PaletteUtil.DEFAULT_SWATCH_PAIR
                                    ))
                                }
                                MediaTypeUtil.TYPE_ALBUM -> {
                                    (activity as MainActivity).goto(AlbumListController(
                                            entries.mapNotNull { it.album },
                                            PaletteUtil.DEFAULT_SWATCH_PAIR
                                    ))
                                }
                            }}
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

    private fun clearForNext(view: View) {
        (0..view.searchResults.childCount).forEach { i ->
            view.searchResults.getChildAt(i)?.let {
                Timber.d("View at $i is id = ${it.id}")
                if(it.id == View.NO_ID) {
                    view.searchResults.removeViewAt(i)
                }
            }
        }
    }

    fun err() {
        Toast.makeText(activity, R.string.error_search, Toast.LENGTH_LONG).show()
    }
}
