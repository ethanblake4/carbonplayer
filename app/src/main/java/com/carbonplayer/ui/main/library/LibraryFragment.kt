package com.carbonplayer.ui.main.library

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v13.app.FragmentStatePagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.carbonplayer.R
import com.carbonplayer.utils.IdentityUtils
import kotlinx.android.synthetic.main.activity_main.view.*

class LibraryFragment : Fragment() {

    lateinit var adapter: FragmentStatePagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.activity_main, container, false)

        view.toolbar.setPadding(view.toolbar.paddingLeft, view.toolbar.paddingTop +
                IdentityUtils.getStatusBarHeight(resources),
                view.toolbar.paddingRight, view.toolbar.paddingBottom)

        //view.toolbar.layoutParams.height += IdentityUtils.getStatusBarHeight(resources)
        (view.toolbar.layoutParams as AppBarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2
        (view.toolbar.layoutParams as AppBarLayout.LayoutParams).bottomMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2

        adapter = object : FragmentStatePagerAdapter(fragmentManager) {
            override fun getItem(position: Int): Fragment {
                when (position) {
                    0 -> return PlaylistPageFragment()
                    1 -> return ArtistsPageFragment()
                    2 -> return AlbumPageFragment()
                }
                return Fragment()
            }

            override fun getCount(): Int {
                return 3
            }

        }

        savedInstanceState?.let {
            view.libraryPager
                    .onRestoreInstanceState(savedInstanceState.getParcelable("pager"))
        }

        view.libraryPager.adapter = adapter

        return view
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable("pager", view.libraryPager.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()

    }
}