package com.carbonplayer.ui.main

import android.content.Context
import android.support.annotation.Keep
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bumptech.glide.Glide
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.api.ExploreTab
import com.carbonplayer.model.entity.api.StationCategory
import com.carbonplayer.model.entity.enums.ExploreTabType
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.main.adapters.TopChartsAlbumAdapter
import com.carbonplayer.ui.main.explore.BrowseStationsController
import com.carbonplayer.ui.main.explore.BrowseStationsPage
import com.carbonplayer.ui.main.explore.BrowseStationsTab
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.controller_explore.view.*
import timber.log.Timber

class ExploreController @Keep constructor() : Controller() {

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)

        if (CarbonPlayerApplication.instance.lastNewReleasesResponse == null) {
            Protocol.exploreTab(context, ExploreTabType.NEW_RELEASES)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->

                        CarbonPlayerApplication.instance.lastNewReleasesResponse = response
                        view?.let { applyNewReleases(it, response) }

                    }) { err ->
                        Timber.e(err)
                    }
        }

        if (CarbonPlayerApplication.instance.lastStationCategoryRoot == null) {
            Protocol.stationCategories(context)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        CarbonPlayerApplication.instance.lastStationCategoryRoot = response
                        view?.let { applyBrowseStations(it, response) }

                    }) { err ->
                        Timber.e(err)
                    }
        }


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val root = inflater.inflate(R.layout.controller_explore, container, false)

        root.exploreContainer.setPadding(0, IdentityUtils.getStatusBarHeight(resources),
                0, IdentityUtils.getNavbarHeight(resources) +
                (activity as MainActivity).bottomInset)

        root.exploreContainer.clipToPadding = false

        root.toolbar.setPadding(root.toolbar.paddingLeft, root.toolbar.paddingTop +
                IdentityUtils.getStatusBarHeight(resources),
                root.toolbar.paddingRight, root.toolbar.paddingBottom)

        root.explore_nr_recycler.layoutParams.height =
                (IdentityUtils.displayWidth2(inflater.context) / 2) +
                        MathUtils.dpToPx2(container.resources, 70)

        root.explore_nr_recycler.isNestedScrollingEnabled = false

        //view.toolbar.layoutParams.height += IdentityUtils.getStatusBarHeight(resources)
        (root.toolbar.layoutParams as AppBarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2
        (root.toolbar.layoutParams as AppBarLayout.LayoutParams).bottomMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2

        CarbonPlayerApplication.instance.lastNewReleasesResponse?.let {
            applyNewReleases(root, it)
        }

        CarbonPlayerApplication.instance.lastStationCategoryRoot?.let {
            applyBrowseStations(root, it)
        } ?: getChildRouter(root.browseStationsContainer).pushController(
                RouterTransaction.with(LoadingController())
        )

        root.app_bar.addOnOffsetChangedListener({ _, i ->
            (activity as MainActivity).scrollCb(i)
        })

        return root
    }

    override fun onAttach(view: View) {

        super.onAttach(view)

        CarbonPlayerApplication.instance.lastStationCategoryRoot?.let {
            applyBrowseStations(view, it)
        } ?: getChildRouter(view.browseStationsContainer).pushController(
                RouterTransaction.with(LoadingController())
        )
    }

    private fun applyNewReleases(view: View, newReleases: ExploreTab) {
        view.explore_nr_recycler.layoutManager = LinearLayoutManager(
                view.context, LinearLayoutManager.HORIZONTAL, false
        )
        view.explore_nr_recycler.adapter = TopChartsAlbumAdapter (
                newReleases.groups[0].entities.mapNotNull { it.album },
                activity as MainActivity,
                Glide.with(view)
        ).apply { isHorizontal = true }
    }

    private fun applyBrowseStations(view: View, categoryRoot: StationCategory) {

        Timber.d(categoryRoot.toString())

        getChildRouter(view.browseStationsContainer).apply {
            setRoot(
                    RouterTransaction.with(BrowseStationsController(categoryRoot, { station ->
                        selectStation(this, station)
                    }, { pager -> view.stationsTabs.setupWithViewPager(pager)}))
            )
        }
    }

    private fun selectStation(router: Router, station: StationCategory) {
        if(station.subcategories != null) {
            router.pushController(RouterTransaction.with(BrowseStationsTab(station, {
                selectStation(router, it)
            })))
        } else {
            router.pushController(RouterTransaction.with(LoadingController()))
            Protocol.stations(activity!!, station)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { response ->
                        router.replaceTopController(RouterTransaction.with(
                                BrowseStationsPage(response.take(60))
                        ))
                    }
        }
        view!!.browseStationsHeader.text = station.display_name
        view!!.browseStationsHeader.visibility = View.VISIBLE
        view!!.browseStationsBack.visibility = View.VISIBLE
        view!!.stationsTabs.visibility = View.INVISIBLE

        view!!.browseStationsBack.setOnClickListener {
            router.apply {
                Timber.d("backstack size is $backstackSize")
                if(backstackSize == 2) {
                    view!!.browseStationsBack.visibility = View.GONE
                    view!!.browseStationsHeader.visibility = View.GONE
                    view!!.stationsTabs.visibility = View.VISIBLE
                    popCurrentController()
                }
            }
        }
    }

}