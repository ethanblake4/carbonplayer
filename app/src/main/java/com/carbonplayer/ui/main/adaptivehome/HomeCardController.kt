package com.carbonplayer.ui.main.adaptivehome

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.support.annotation.Keep
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto.FullBleedSection
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.utils.protocol.ProtoUtils
import com.carbonplayer.utils.ui.ColorUtils
import com.karumi.headerrecyclerview.HeaderSpanSizeLookup
import kotlinx.android.synthetic.main.controller_home_card.view.*
import timber.log.Timber



class HomeCardController (
        private val module: FullBleedModuleV1Proto.FullBleedModule
) : Controller() {

    @Keep
    constructor() : this (FullBleedModuleV1Proto.FullBleedModule.getDefaultInstance())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val root = inflater.inflate(R.layout.controller_home_card, container, false)

        when (module.singleSection.contentCase) {
             FullBleedSection.ContentCase.SQUAREPLAYABLECARDLIST -> {

                Timber.d("squareplayable")

                root.homeCardRecycler.apply {
                    layoutManager = GridLayoutManager(inflater.context, 2)
                    adapter = SquarePlayableCardAdapter(
                            activity as MainActivity
                    ).apply {
                        setItems(module.singleSection.squarePlayableCardList.cardsList)
                        header = Pair(module.moduleTitle, module.moduleTitleUnderlineColor)
                    }
                }
            }
            FullBleedSection.ContentCase.TALLPLAYABLECARDLIST -> {
                Timber.d("tallplayable")

                val vAdapter = TallPlayableCardAdapter(
                        activity as MainActivity
                ).apply {
                    setItems(module.singleSection.tallPlayableCardList.cardsList)
                    header = Pair(module.moduleTitle, module.moduleTitleUnderlineColor)
                }

                root.homeCardRecycler.apply {
                    layoutManager = GridLayoutManager(inflater.context, 2).apply {
                        spanSizeLookup = HeaderSpanSizeLookup(vAdapter, this)
                    }
                    adapter = vAdapter
                }
            }
            FullBleedSection.ContentCase.WIDEPLAYABLECARDLIST -> {
                Timber.d("wideplayable")

                val vAdapter = WidePlayableCardAdapter(
                        activity as MainActivity
                ).apply {
                    setItems(module.singleSection.widePlayableCardList.cardsList)
                    header = Pair(module.moduleTitle, module.moduleTitleUnderlineColor)
                }

                root.homeCardRecycler.apply {
                    layoutManager = LinearLayoutManager(activity)
                    adapter = vAdapter
                }
            }

        // Foreground drawables only supported on Marshmallow +
        }

        root.homeCardRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                root.homeCardBackdrop.translationY -= dy * 0.16f

                super.onScrolled(recyclerView, dx, dy)
            }
        })

        root.background = ColorDrawable(
                ProtoUtils.colorFrom(module.backgroundImageReference.representativeColor)
        )

        val grad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ColorUtils.modifyAlpha(ProtoUtils.colorFrom(
                        module.backgroundColor), 0.1f),
                        ColorUtils.modifyAlpha(ProtoUtils.colorFrom(
                        module.backgroundImageReference.representativeColor), 1.0f)))

        Glide.with(activity)
                .load(module.backgroundImageReference.url)
                .into(root.homeCardBackdrop)

        // Foreground drawables only supported on Marshmallow +
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            root.homeCardBackdrop.foreground = grad
        } else root.homeCardGradient?.background = grad

        return root
    }
}