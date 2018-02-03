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
import com.carbonplayer.R
import com.carbonplayer.model.entity.TopChartsResponse
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

    var currentResponse: TopChartsResponse? = null

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)

        Protocol.getTopCharts(context, 0, 100)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    currentResponse = response
                    currentSongPage?.let { it.songList = response.chart.tracks }
                    currentAlbumPage?.let { it.albumList = response.chart.albums }
                    view?.let {
                        Glide.with(activity).load(response.header.header_image.url)
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
                            currentResponse?.let { currentSongPage!!.songList = it.chart.tracks }
                            router.setRoot(RouterTransaction.with(currentSongPage!!))
                        }
                        1 -> {
                            currentAlbumPage = TopChartsAlbumPage()
                            currentResponse?.let { currentAlbumPage!!.albumList = it.chart.albums }
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

        (v.toolbar2.layoutParams as CollapsingToolbarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2

        currentResponse?.let {
            Glide.with(activity).load(it.header.header_image.url)
                    .into(v.topcharts_header_image)
        }

        return v
    }



}