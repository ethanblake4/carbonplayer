package com.carbonplayer.ui.main

import android.support.design.widget.AppBarLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.exception.ServerRejectionException
import com.carbonplayer.model.entity.proto.innerjam.ContentPageV1Proto.ContentPage.PageTypeCase
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto.GetHomeResponse
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto.FullBleedSection
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.main.adaptivehome.FullBleedListAdapter
import com.carbonplayer.utils.addToAutoDispose
import com.carbonplayer.utils.general.IdentityUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.adaptivehome.view.*
import timber.log.Timber

class HomeController : Controller() {

    lateinit var adapter: FullBleedListAdapter
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var requestManager: RequestManager
    var currentScrollCallback: Action? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val view = inflater.inflate(R.layout.adaptivehome, container, false)
        view.main_recycler.hasFixedSize()

        view.toolbar.setPadding(view.toolbar.paddingLeft, view.toolbar.paddingTop +
                IdentityUtils.getStatusBarHeight(resources),
                view.toolbar.paddingRight, view.toolbar.paddingBottom)

        //view.toolbar.layoutParams.height += IdentityUtils.getStatusBarHeight(resources)
        (view.toolbar.layoutParams as AppBarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2
        (view.toolbar.layoutParams as AppBarLayout.LayoutParams).bottomMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2

        view.app_bar.addOnOffsetChangedListener({ _, i ->
            (activity as MainActivity).scrollCb(i)
            currentScrollCallback?.run()
        })

        layoutManager = LinearLayoutManager(activity)

        view.main_recycler.layoutManager = layoutManager

        view.main_recycler.recycledViewPool.setMaxRecycledViews(0, 2)

        view.swipeRefreshLayout.setOnRefreshListener {
            Timber.d("Will refresh")
            refresh()
        }

        CarbonPlayerApplication.instance.homeLastResponse?.let {
            processHomeRequest(it)
        }

        requestManager = Glide.with(activity)
        refresh()

        return view
    }

    fun refresh() {
        Protocol.listenNow(activity!!, CarbonPlayerApplication.instance.homePdContextToken)
                .retry({ tries, err ->
                    if (err !is ServerRejectionException) return@retry false
                    if (err.rejectionReason !=
                            ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
                        return@retry false
                    else tries < 3
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ home ->
                    CarbonPlayerApplication.instance.homeLastResponse = home
                    processHomeRequest(home)
                }, { err ->
                    Timber.e(err)
                    view?.adaptiveHomeCoordinator?.let {
                        Snackbar.make(it, "Error " +
                                "", Snackbar.LENGTH_SHORT)
                    }
                }).addToAutoDispose()

    }

    fun processHomeRequest(home: GetHomeResponse) {
        CarbonPlayerApplication.instance.homePdContextToken = home.distilledContextToken

        val homeContentPage = home.homeContentPage
        when (homeContentPage.pageTypeCase) {
            PageTypeCase.HOMEPAGE -> {
                val homePage = homeContentPage.homePage

                view?.main_recycler?.adapter = FullBleedListAdapter(
                        homePage.fullBleedModuleList.modulesList.filter { m ->
                            m.singleSection.contentCase.let {
                                it == FullBleedSection.ContentCase.TALLPLAYABLECARDLIST ||
                                        it == FullBleedSection.ContentCase.WIDEPLAYABLECARDLIST ||
                                        it == FullBleedSection.ContentCase.SQUAREPLAYABLECARDLIST
                            }
                        },
                        { mod: FullBleedModuleV1Proto.FullBleedModule ->
                            Timber.d(mod.toString())
                        },
                        requestManager, view!!.main_recycler).apply { currentScrollCallback = this.scrollCallback }

                view?.swipeRefreshLayout?.isRefreshing = false
            }
            else -> {
                view?.adaptiveHomeCoordinator?.let {
                    Snackbar.make(it, "Error processing server response", Snackbar.LENGTH_SHORT)
                }
            }
        }
    }

    fun playSingleSection(singleSection: FullBleedSection) {
        when (singleSection.contentCase) {
            FullBleedSection.ContentCase.SQUAREPLAYABLECARDLIST -> {
                //singleSection.squarePlayableCardList.cardsList[0].
            }
        }
    }
}