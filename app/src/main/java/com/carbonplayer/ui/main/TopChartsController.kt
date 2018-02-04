package com.carbonplayer.ui.main

import android.content.Context
import android.support.design.widget.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.main.topcharts.TopChartsAlbumPage
import com.carbonplayer.ui.main.topcharts.TopChartsSongPage
import com.carbonplayer.utils.addToAutoDispose
import com.carbonplayer.utils.general.IdentityUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.controller_topcharts.view.*
import timber.log.Timber

class TopChartsController : Controller() {

    var currentSongPage: TopChartsSongPage? = null
    var currentAlbumPage: TopChartsAlbumPage? = null

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)

        getCharts(context)

        Protocol.getTopChartsGenres(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {

                    (activity as MainActivity).callbackWithTopChartsGenres(it.genres, {
                        Timber.d("selected $it")

                        if(it == DEFAULT_CHART) getCharts(context)
                        else applyCachedChart(context, it)
                    })
                }.addToAutoDispose()
    }

    fun getCharts(context: Context) {

        Protocol.getTopCharts(context, 0, CHART_ENTRIES)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->

                    CarbonPlayerApplication.instance.topchartsResponseMap[DEFAULT_CHART] = response

                    currentSongPage?.let { it.songList = response.chart.tracks }
                    currentAlbumPage?.let { it.albumList = response.chart.albums }
                    view?.let {
                        Glide.with(activity).load(response.header.header_image.url)
                                .transition(DrawableTransitionOptions.withCrossFade(200))
                                .into(it.topcharts_header_image)
                    }

                    view?.topChartsSwipeRefresh?.isRefreshing = false
                }, { err ->
                    Timber.e(err)
                }).addToAutoDispose()
    }

    fun applyCachedChart(context: Context, id: String) {

        CarbonPlayerApplication.instance.topchartsResponseMap[id]?.let { response ->
            currentSongPage?.let { it.songList = response.chart.tracks }
            currentAlbumPage?.let { it.albumList = response.chart.albums }
            CarbonPlayerApplication.instance.topchartsResponseMap[id] = response

            view?.let {
                Glide.with(activity).load(response.header.header_image.url)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(it.topcharts_header_image)
            }
        }

        getChartsFor(context, id)
    }

    fun getChartsFor(context: Context, id: String) {
        Protocol.getTopChartsFor(context, id, 0, CHART_ENTRIES)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    currentSongPage?.let { it.songList = response.chart.tracks }
                    currentAlbumPage?.let { it.albumList = response.chart.albums }
                    CarbonPlayerApplication.instance.topchartsResponseMap[id] = response

                    view?.let {
                        Glide.with(activity).load(response.header.header_image.url)
                                .transition(DrawableTransitionOptions.withCrossFade(200))
                                .into(it.topcharts_header_image)
                    }
                }, { err ->
                    Timber.e(err)
                }).addToAutoDispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val v = inflater.inflate(R.layout.controller_topcharts, container, false)

        v.topChartsPager.adapter = object : RouterPagerAdapter(this) {

            override fun configureRouter(router: Router, position: Int) {
                if(!router.hasRootController()) {
                    when(position) {
                        0 -> {
                            currentSongPage = TopChartsSongPage()
                            CarbonPlayerApplication.instance.topchartsResponseMap[DEFAULT_CHART]?.let {
                                currentSongPage!!.songList = it.chart.tracks
                            }
                            router.setRoot(RouterTransaction.with(currentSongPage!!))
                        }
                        1 -> {
                            currentAlbumPage = TopChartsAlbumPage()
                            CarbonPlayerApplication.instance.topchartsResponseMap[DEFAULT_CHART]?.let {
                                currentAlbumPage!!.albumList = it.chart.albums
                            }
                            router.setRoot(RouterTransaction.with(currentAlbumPage!!))
                        }
                    }
                }
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when (position + if (resources == null) 123 else 0) {
                    0 -> resources!!.getString(R.string.topcharts_tab_songs)
                    1 -> resources!!.getString(R.string.topcharts_tab_albums)
                    else -> super.getPageTitle(position)
                }
            }

            override fun getCount(): Int = 2

        }

        v.topcharts_tabs.setupWithViewPager(v.topChartsPager)

        v.topChartsSwipeRefresh.isRefreshing = true

        (v.toolbar2.layoutParams as CollapsingToolbarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources)

        CarbonPlayerApplication.instance.topchartsResponseMap[DEFAULT_CHART]?.let {
            Glide.with(activity).load(it.header.header_image.url)
                    .into(v.topcharts_header_image)
        }

        return v
    }

    companion object {
        const val DEFAULT_CHART = "DEFAULT"
        const val CHART_ENTRIES = 100
    }



}