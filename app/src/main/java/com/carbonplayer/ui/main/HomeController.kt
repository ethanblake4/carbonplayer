package com.carbonplayer.ui.main

import android.support.design.widget.AppBarLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.exception.ServerRejectionException
import com.carbonplayer.model.entity.proto.identifiers.AlbumReleaseIdV1Proto
import com.carbonplayer.model.entity.proto.identifiers.ArtistIdV1Proto
import com.carbonplayer.model.entity.proto.identifiers.PlayableItemIdV1Proto
import com.carbonplayer.model.entity.proto.identifiers.PlayableItemIdV1Proto.PlayableItemId.TypeCase.*
import com.carbonplayer.model.entity.proto.identifiers.RadioSeedIdV1Proto.RadioSeedId.TypeCase.*
import com.carbonplayer.model.entity.proto.identifiers.RadioSeedIdV1Proto.RadioSeedId.TypeCase.ARTIST
import com.carbonplayer.model.entity.proto.identifiers.RadioSeedIdV1Proto.RadioSeedId.TypeCase.TYPE_NOT_SET
import com.carbonplayer.model.entity.proto.identifiers.TrackIdV1Proto
import com.carbonplayer.model.entity.proto.innerjam.ContentPageV1Proto.ContentPage.PageTypeCase
import com.carbonplayer.model.entity.proto.innerjam.InnerJamApiV1Proto.GetHomeResponse
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto.FullBleedSection
import com.carbonplayer.model.entity.radio.RadioSeed
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.main.adaptivehome.FullBleedListAdapter
import com.carbonplayer.utils.addToAutoDispose
import com.carbonplayer.utils.general.IdentityUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.adaptivehome.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class HomeController : Controller() {

    lateinit var adapter: FullBleedListAdapter
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var requestManager: RequestManager
    private var currentScrollCallback: Action? = null

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

        view.main_recycler.setPadding(0, 0, 0,
                IdentityUtils.getNavbarHeight(resources) +
                        (activity as MainActivity).bottomInset)

        view.swipeRefreshLayout.setOnRefreshListener {
            Timber.d("Will refresh")
            refresh()
        }

        requestManager = Glide.with(activity!!)

        CarbonPlayerApplication.instance.homeLastResponse?.let {
            Timber.d("Has cached gethomeresponse")
            processHomeRequest(it, view)
            it
        } ?: refresh(view)

        return view
    }

    private fun refresh(v: View = view!!) {

        v.swipeRefreshLayout.isRefreshing = true

        Protocol.listenNow(activity!!, CarbonPlayerApplication.instance.homePdContextToken)
                .retry({ tries, err ->
                    if (err !is ServerRejectionException) return@retry tries < 2
                    if (err.rejectionReason !=
                            ServerRejectionException.RejectionReason.DEVICE_NOT_AUTHORIZED)
                        return@retry false
                    else tries < 3
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ home ->
                    if(home.previousContentState !=
                            GetHomeResponse.PreviousContentState.ALL_CONTENT_UP_TO_DATE) {
                        CarbonPlayerApplication.instance.homeLastResponse = home
                        processHomeRequest(home)
                    }
                    v.swipeRefreshLayout.isRefreshing = false
                }, { err ->
                    Timber.e(err)
                    v.adaptiveHomeCoordinator?.let {
                        Snackbar.make(it, "Error " +
                                "", Snackbar.LENGTH_SHORT)
                    }
                }).addToAutoDispose()

    }

    private fun processHomeRequest(home: GetHomeResponse, v: View = view!!) {
        CarbonPlayerApplication.instance.homePdContextToken = home.distilledContextToken

        val homeContentPage = home.homeContentPage
        when (homeContentPage.pageTypeCase) {
            PageTypeCase.HOMEPAGE -> {
                val homePage = homeContentPage.homePage

                v.main_recycler.adapter = FullBleedListAdapter(
                        homePage.fullBleedModuleList.modulesList.filter { m ->
                            m.singleSection.contentCase.let {
                                it == FullBleedSection.ContentCase.TALLPLAYABLECARDLIST ||
                                        it == FullBleedSection.ContentCase.WIDEPLAYABLECARDLIST ||
                                        it == FullBleedSection.ContentCase.SQUAREPLAYABLECARDLIST
                            }
                        },
                        { mod: FullBleedModuleV1Proto.FullBleedModule ->
                            Toast.makeText(activity, "This action is not supported yet",
                                    Toast.LENGTH_LONG).show()
                            Timber.d("Clicked module")
                        }, { mod, cb ->
                            playSingleSection(mod.singleSection)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe { cb.run() }
                        }, requestManager, v.main_recycler)
                        .apply {
                            currentScrollCallback = this.scrollCallback
                        }

            }
            else -> {
                v.adaptiveHomeCoordinator.let {
                    Snackbar.make(it, "Error processing server response", Snackbar.LENGTH_SHORT)
                }
            }
        }
    }

    private fun playSingleSection(singleSection: FullBleedSection): Completable {
        return when (singleSection.contentCase) {
            FullBleedSection.ContentCase.SQUAREPLAYABLECARDLIST -> {
                playItem(singleSection.squarePlayableCardList.cardsList[0].itemId)
            }
            FullBleedSection.ContentCase.WIDEPLAYABLECARDLIST -> {
                playItem(singleSection.widePlayableCardList.cardsList[0].itemId)
            }
            FullBleedSection.ContentCase.TALLPLAYABLECARDLIST -> {
                playItem(singleSection.tallPlayableCardList.cardsList[0].itemId)
            }
            else -> errorPlaying()
        }
    }

    private fun playItem(itemId: PlayableItemIdV1Proto.PlayableItemId): Completable {
        val helper = (activity as MainActivity).npHelper

        when(itemId.typeCase) {
            RADIOSEED -> {
                when(itemId.radioSeed.typeCase) {
                    TRACK -> {
                        return when(itemId.radioSeed.track.typeCase) {
                            TrackIdV1Proto.TrackId.TypeCase.CATALOG -> {
                                helper.startRadio(RadioSeed.TYPE_SJ_TRACK,
                                        itemId.radioSeed.track.catalog.metajamCompactKey)
                            }
                            TrackIdV1Proto.TrackId.TypeCase.UPLOADED -> {
                                helper.startRadio(RadioSeed.TYPE_LIBRARY_TRACK,
                                        itemId.radioSeed.track.uploaded.lockerId)
                            }
                            else -> errorPlaying()
                        }
                    }
                    ALBUMRELEASE -> {
                        return when(itemId.radioSeed.albumRelease.typeCase) {
                            AlbumReleaseIdV1Proto.AlbumReleaseId.TypeCase.CATALOG -> {
                                helper.startRadio(RadioSeed.TYPE_ALBUM,
                                        itemId.radioSeed.albumRelease.catalog.metajamCompactKey)
                            }
                            else -> errorPlaying()
                        }
                    }
                    CURATED -> {
                        return helper.startRadio(RadioSeed.TYPE_CURATED_STATION,
                                itemId.radioSeed.curated.metajamCompactKey)
                    }
                    ARTIST -> {
                        return when(itemId.radioSeed.artist.typeCase) {
                            ArtistIdV1Proto.ArtistId.TypeCase.CATALOG -> {
                                helper.startRadio(RadioSeed.TYPE_ARTIST,
                                        itemId.radioSeed.artist.catalog.metajamCompactKey)
                            }
                            else -> errorPlaying()
                        }
                    }
                    GENRE -> {
                        return helper.startRadio(RadioSeed.TYPE_GENRE,
                                itemId.radioSeed.genre.id)
                    }
                    LOCKERPLAYLIST -> {
                        return helper.startRadio(RadioSeed.TYPE_PLAYLIST,
                                itemId.radioSeed.lockerPlaylist.playlistToken)
                    }

                    FEELINGLUCKY -> return errorPlaying()
                    TYPE_NOT_SET -> return errorPlaying()
                    null -> return errorPlaying()
                }
            }
            AUDIO -> return errorPlaying()
            AUDIOLIST -> return errorPlaying()
            PlayableItemIdV1Proto.PlayableItemId.TypeCase.ARTIST -> return errorPlaying()
            PlayableItemIdV1Proto.PlayableItemId.TypeCase.TYPE_NOT_SET -> return errorPlaying()
            else -> return errorPlaying()
        }
    }

    private fun errorPlaying(): Completable {
        Toast.makeText(activity, "Playing this item is not supported yet", Toast.LENGTH_LONG)
                .show()
        Timber.e("Could not play")
        return Completable.timer(1, TimeUnit.MILLISECONDS)
    }
}