package com.carbonplayer.ui.main.library

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.carbonplayer.R
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.ui.helpers.MusicManager
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.utils.carbonAnalytics
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.logEntityEvent
import com.carbonplayer.utils.ui.ColorUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.github.florent37.glidepalette.GlidePalette
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_songgroup.view.*
import kotlinx.android.synthetic.main.songgroup_details.view.*

class StationController(
        val station: SkyjamStation?,
        var textColor: Int,
        var mainColor: Int,
        var bodyColor: Int,
        var secondaryColor: Int,
        var secondaryTextColor: Int
) : Controller() {

    private var swatchPair: PaletteUtil.SwatchPair? = null

    private var fabOffset: Int = 0
    private var squareHeight: Int = 0

    private var expanded = false
    private var ogHeight = 0

    private lateinit var manager: MusicManager
    private lateinit var requestMgr: RequestManager

    init {
        station?.let { carbonAnalytics.logEntityEvent(FirebaseAnalytics.Event.VIEW_ITEM, it) }
    }

    constructor() : this(null, PaletteUtil.DEFAULT_SWATCH_PAIR)

    constructor(station: SkyjamStation?, swatchPair: PaletteUtil.SwatchPair) : this (
            station,
            swatchPair.primary.titleTextColor,
            swatchPair.primary.rgb,
            swatchPair.primary.bodyTextColor,
            swatchPair.secondary.rgb,
            swatchPair.secondary.bodyTextColor
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        val root = inflater.inflate(R.layout.activity_songgroup, container, false)

        manager = MusicManager(activity as MainActivity)

        root.main_backdrop.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.TWO_BY_ONE
        root.parallaxSquare.aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.TWO_BY_ONE

        root.primaryText.text = station?.bestName?.plus(" radio") ?: "Unknown station"
        root.secondaryText.text = station?.seed?.metadataSeed?.artist?.name ?:
                station?.seed?.metadataSeed?.album?.name
                ?: station?.seed?.metadataSeed?.playlist?.name ?: "Radio station"
        squareHeight = root.main_backdrop.height
        fabOffset = MathUtils.dpToPx(activity, 28)

        root.albumLayoutRoot.viewTreeObserver.addOnGlobalLayoutListener {
            root.play_fab.y = (root.main_backdrop.height - fabOffset).toFloat()
        }

        root.play_fab.setOnClickListener {
            station?.let { manager.radio(it) }
        }

        root.primaryText.setTextColor(textColor)
        root.secondaryText.setTextColor(textColor)
        root.constraintLayout6.background = ColorDrawable(mainColor)
        root.songgroup_grad.background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(mainColor, ColorUtils.modifyAlpha(mainColor, 200),
                        ColorUtils.modifyAlpha(mainColor, 0)))

        root.artistTracksHeader.visibility = View.VISIBLE

        root.downloadButton.imageTintList = ColorStateList.valueOf(textColor)
        root.overflowButton.imageTintList = ColorStateList.valueOf(textColor)
        root.expandDescriptionChevron.imageTintList = ColorStateList.valueOf(bodyColor)

        root.play_fab.backgroundTintList = ColorStateList.valueOf(secondaryColor)
        root.play_fab.imageTintList = ColorStateList.valueOf(secondaryTextColor)

        station?.description?.let {
            root.descriptionText.text = it
        }

        station?.imageUrls?.get(0)?.url?.let {
            Glide.with(activity!!).load(it)
                    .apply { if(swatchPair == PaletteUtil.DEFAULT_SWATCH_PAIR) {
                        listener(GlidePalette.with(it)
                                .use(0)
                                .intoCallBack({ palette -> if (palette != null) {
                                    val pair = PaletteUtil.getSwatches(activity!!, palette)
                                    mainColor = pair.primary.rgb
                                    bodyColor = pair.primary.bodyTextColor
                                    secondaryColor = pair.secondary.rgb
                                    textColor = pair.primary.titleTextColor
                                    secondaryTextColor = pair.secondary.bodyTextColor
                                }
                                }))
                    }}.into(root.main_backdrop)
        }

        return root

    }

}