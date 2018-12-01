package com.carbonplayer.ui.main.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.carbonplayer.R
import com.carbonplayer.model.entity.Artist
import com.carbonplayer.model.entity.base.IArtist
import com.carbonplayer.ui.widget.fastscroll.SectionTitleProvider
import com.carbonplayer.utils.ui.PaletteUtil
import com.github.florent37.glidepalette.GlidePalette
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.artist_list_item.view.*

/**
 * Album / playlist adapter
 */
internal class LinearArtistAdapter(
        private val context: Context,
        private val dataset: List<IArtist>,
        private val clicked: (Pair<IArtist, PaletteUtil.SwatchPair?>) -> Unit,
        private val lightTheme: Boolean = false
)   : RecyclerView.Adapter<LinearArtistAdapter.ViewHolder>(), SectionTitleProvider {

    override fun getSectionTitle(position: Int): String =
            dataset[position].name.take(1).toUpperCase()

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        var name: TextView = v.artistName
        var thumb: CircleImageView = v.artistImage
        var swatchPair: PaletteUtil.SwatchPair? = null

        var pos: Int = 0

        init {
            v.findViewById<View>(R.id.artistLinearRoot)
                    .setOnClickListener { _ -> clicked(Pair(dataset[pos], swatchPair)) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(if(lightTheme) R.layout.artist_list_item_light else R.layout.artist_list_item,
                        parent, false)

        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = dataset[position]

        holder.name.text = t.name

        val artRef = t.artistArtRef ?: (t as? Artist)?.artistArtRefs?.firstOrNull()?.url

        Glide.with(holder.itemView)
                .load(artRef)
                .listener(GlidePalette.with(artRef).intoCallBack { it?.let { palette ->
                    holder.swatchPair = PaletteUtil.getSwatches(context, palette)
                }})
                .into(holder.thumb)


        holder.pos = position

    }

    override fun getItemCount(): Int = dataset.size
}
