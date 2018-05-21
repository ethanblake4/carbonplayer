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
import android.widget.Toast
import com.bluelinelabs.conductor.Controller
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.Playlist
import com.carbonplayer.model.entity.proto.identifiers.AudioListIdV1Proto.AudioListId
import com.carbonplayer.model.entity.proto.identifiers.PlayableItemIdV1Proto.PlayableItemId
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto
import com.carbonplayer.model.entity.proto.innerjam.renderers.FullBleedModuleV1Proto.FullBleedSection
import com.carbonplayer.model.entity.proto.metadata.PlaybackMetadataV1Proto
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamPlaylist
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.utils.carbonAnalytics
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.logUnsupportedEvent
import com.carbonplayer.utils.protocol.ProtoUtils
import com.carbonplayer.utils.ui.ColorUtils
import com.carbonplayer.utils.ui.PaletteUtil
import com.karumi.headerrecyclerview.HeaderSpanSizeLookup
import io.realm.Realm
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
                 val vAdapter = SquarePlayableCardAdapter(
                         activity as MainActivity,
                         { card ->
                             gotoItem(card.itemId,
                                 card.titleSection.title.text,
                                 card.titleSection.subtitle.text,
                                 listOf(card.imageReference.url),
                                 card.playbackMetadata)
                         },
                         { view, card ->
                             (activity as MainActivity).showContextMenuPopup(
                                     view,
                                     card.contextMenu,
                                     card.titleSection.title.text,
                                     card.titleSection.subtitle.text
                             )
                         }
                 ).apply {
                     setItems(module.singleSection.squarePlayableCardList.cardsList)
                     header = Pair(module.moduleTitle, module.moduleTitleUnderlineColor)
                 }

                 root.homeCardRecycler.apply {
                     layoutManager = GridLayoutManager(inflater.context, 2).apply {
                         spanSizeLookup = HeaderSpanSizeLookup(vAdapter, this)
                     }
                     adapter = vAdapter
                 }
            }

            FullBleedSection.ContentCase.TALLPLAYABLECARDLIST -> {
                Timber.d("tallplayable")

                val vAdapter = TallPlayableCardAdapter(
                        activity as MainActivity,
                        { card -> gotoItem(card.itemId, card.titleSection.title.text,
                                card.titleSection.subtitle.text,
                                listOf(card.imageReference.url),
                                card.playbackMetadata)},
                        { view, card ->
                            (activity as MainActivity).showContextMenuPopup(
                                    view,
                                    card.contextMenu,
                                    card.titleSection.title.text,
                                    card.titleSection.subtitle.text
                            )
                        }
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
                        activity as MainActivity,
                        { card -> gotoItem(card.itemId,
                                card.title.text,
                                card.description.text,
                                listOf(card.imageReference.url),
                                card.playbackMetadata)
                        }
                ).apply {
                    setItems(module.singleSection.widePlayableCardList.cardsList)
                    header = Pair(module.moduleTitle, module.moduleTitleUnderlineColor)
                }
                root.homeCardRecycler.apply {
                    layoutManager = LinearLayoutManager(activity)
                    adapter = vAdapter
                }
            }
        }

        root.homeCardRecycler.setPadding(0, 0, 0,
                IdentityUtils.getNavbarHeight(resources) +
                        (activity as MainActivity).bottomInset)

        root.homeCardRecycler.clipToPadding = false

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

        Glide.with(container)
                .load(module.backgroundImageReference.url)
                .into(root.homeCardBackdrop)

        // Foreground drawables only supported on Marshmallow +
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            root.homeCardBackdrop.foreground = grad
        } else root.homeCardGradient?.background = grad

        return root
    }

    private fun gotoItem(item: PlayableItemId, title: String, subtitle: String,
                         art: List<String>, metadata: PlaybackMetadataV1Proto.PlaybackMetadata) {

        val mainActivity = (activity as MainActivity)
        val realm = Realm.getDefaultInstance()

        when(item.typeCase) {
            PlayableItemId.TypeCase.AUDIOLIST -> item.audioList.run { when(typeCase) {
                AudioListId.TypeCase.LOCKERPLAYLIST -> {
                    realm.where(Playlist::class.java)
                            .equalTo(Playlist.SHARE_TOKEN, lockerPlaylist.playlistToken)
                            .findFirst()?.let {
                                mainActivity.gotoPlaylist(
                                        it,
                                        null,
                                        PaletteUtil.DEFAULT_SWATCH_PAIR
                                )
                            }
                }
                AudioListId.TypeCase.SHAREDPLAYLIST -> {
                    mainActivity.gotoPlaylist(SkyjamPlaylist(item, title), null,
                            PaletteUtil.DEFAULT_SWATCH_PAIR)
                }
                AudioListId.TypeCase.ALBUMRELEASE -> {
                    mainActivity.gotoAlbum(
                            SkyjamAlbum(item, title, subtitle, art.first()),
                            PaletteUtil.DEFAULT_SWATCH_PAIR
                    )
                }
                else -> {
                    carbonAnalytics.logUnsupportedEvent()
                    Toast.makeText(mainActivity, "This item is not supported yet",
                            Toast.LENGTH_LONG).show()
                }
            } }
            else -> {
                carbonAnalytics.logUnsupportedEvent()
                Toast.makeText(mainActivity, "This item is not supported yet",
                        Toast.LENGTH_LONG).show()
            }
        }
    }
}