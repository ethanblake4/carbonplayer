package com.carbonplayer.ui.main.adaptivehome

import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.carbonplayer.R
import com.carbonplayer.model.entity.proto.innerjam.renderers.SquarePlayableCardV1Proto
import com.carbonplayer.model.entity.proto.innerjam.visuals.AttributedTextV1Proto
import com.carbonplayer.model.entity.proto.innerjam.visuals.ColorV1Proto
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.utils.layoutInflater
import com.carbonplayer.utils.protocol.ProtoUtils
import com.karumi.headerrecyclerview.HeaderRecyclerViewAdapter
import kotlinx.android.synthetic.main.full_bleed_header.view.*
import kotlinx.android.synthetic.main.grid_item_layout.view.*


internal class SquarePlayableCardAdapter (
        val context: MainActivity,
        val callback: (SquarePlayableCardV1Proto.SquarePlayableCardDescriptor) -> Unit,
        val menuCallback: (View, SquarePlayableCardV1Proto.SquarePlayableCardDescriptor) -> Unit
) : HeaderRecyclerViewAdapter<
        RecyclerView.ViewHolder,
        Pair<AttributedTextV1Proto.AttributedText, ColorV1Proto.Color>,
        SquarePlayableCardV1Proto.SquarePlayableCardDescriptor,
        Unit>() {

    private var glide = Glide.with(context)

    internal inner class HeaderViewHolder (
            itemView: View
    ) : RecyclerView.ViewHolder(itemView)

    internal inner class CardViewHolder (
            itemView: View
    ) : RecyclerView.ViewHolder(itemView)

    override fun onCreateHeaderViewHolder(parent: ViewGroup?, viewType: Int) = if(parent == null) {
        super.onCreateHeaderViewHolder(parent, viewType)
    } else {
        val v = parent.layoutInflater.inflate(R.layout.full_bleed_header, parent, false)
        HeaderViewHolder(v)
    }!!

    override fun onCreateItemViewHolder(parent: ViewGroup?, viewType: Int) = if(parent == null) {
        super.onCreateHeaderViewHolder(parent, viewType)
    } else {
        val v = parent.layoutInflater.inflate(R.layout.grid_item_layout, parent, false)
        CardViewHolder(v)
    }!!

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {

        if(holder == null) return

        val card = getItem(position)

        holder.itemView.gridLayoutRoot.contentDescription = card.allYText
        holder.itemView.gridLayoutContentRoot.background = ColorDrawable(
                ProtoUtils.colorFrom(card.imageReference.representativeColor)
        )

        holder.itemView.primaryText.text = card.numberedDescriptionSection.titleSection.title.text
        holder.itemView.detailText.text = card.numberedDescriptionSection.titleSection.subtitle.text

        holder.itemView.imageButton.setOnClickListener { v ->
            menuCallback(v, getItem(position))
        }

        glide.load(card.imageReference.url)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(holder.itemView.imgthumb)

        holder.itemView.gridLayoutRoot.setOnClickListener {
            callback(getItem(position))
        }

    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if(holder == null) return

        holder.itemView.cardTitle.text = header.first.text
        holder.itemView.cardTitle.setTextColor(ProtoUtils.colorFrom(header.first.color))

        holder.itemView.moduleTitleUnderline.setBackgroundColor(
                ProtoUtils.colorFrom(header.second))
    }

    override fun onItemViewRecycled(holder: RecyclerView.ViewHolder?) {
        if(holder == null) return

        glide.clear(holder.itemView.imgthumb)
    }
}