package com.carbonplayer.ui.main.library

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.carbonplayer.R
import com.carbonplayer.ui.main.adapters.LibraryFragmentPagerAdapter
import com.carbonplayer.utils.general.IdentityUtils
import kotlinx.android.synthetic.main.activity_main.view.*
import timber.log.Timber



class LibraryFragment : Fragment() {

    var adapter: LibraryFragmentPagerAdapter? = null
    var curPage = 0
    var currentPlFrag: PlaylistPageFragment? = null
    var currentArFrag: ArtistsPageFragment? = null
    var currentAlFrag: AlbumPageFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        Timber.d("Library - onCreateView. savedInstanceState= %s",
                if(savedInstanceState == null) "null" else "not null")

        val view = inflater.inflate(R.layout.activity_main, container, false)

        view.toolbar.inflateMenu(R.menu.menu_main)

        view.toolbar.setPadding(view.toolbar.paddingLeft, view.toolbar.paddingTop +
                IdentityUtils.getStatusBarHeight(resources),
                view.toolbar.paddingRight, view.toolbar.paddingBottom)

        //view.toolbar.layoutParams.height += IdentityUtils.getStatusBarHeight(resources)
        //(view.app_bar.layoutParams as CoordinatorLayout.LayoutParams).topMargin +=
        //        IdentityUtils.getStatusBarHeight(resources) / 2
        /*(view.toolbar.layoutParams as AppBarLayout.LayoutParams).bottomMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2*/
        (view.tab_layout.layoutParams as AppBarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2

        view.statusBarCover.layoutParams.height = IdentityUtils.getStatusBarHeight(resources)

        if(adapter == null) {
            adapter = object : LibraryFragmentPagerAdapter(fragmentManager) {
                override fun getItem(position: Int): Fragment {
                    when (position) {
                        0 -> {
                            Timber.d("Adapter getItem@0 = PlaylistPage")
                            currentPlFrag = currentPlFrag ?: PlaylistPageFragment()
                            return currentPlFrag!!
                        }
                        1 -> {
                            Timber.d("Adapter getItem@1 = ArtistsPage")
                            currentArFrag = currentArFrag ?: ArtistsPageFragment()
                            return currentArFrag!!
                        }
                        2 -> {
                            Timber.d("Adapter getItem@3 = AlbumPage")
                            currentAlFrag = currentAlFrag ?: AlbumPageFragment()
                            return currentAlFrag!!
                        }
                    }
                    return Fragment()
                }

                override fun getPageTitle(position: Int): CharSequence =
                    when(position) {
                        0 -> "Playlists"
                        1 -> "Artists"
                        2 -> "Albums"
                        else -> ""
                    }


                override fun getItemPosition(`object`: Any): Int {
                    return POSITION_NONE
                }

                override fun getCount(): Int {
                    return 3
                }

            }
        }

        Timber.d("curpager=%d", curPage)

        view.libraryPager.adapter = adapter
        view.tab_layout.setupWithViewPager(view.libraryPager)

        view.libraryPager.currentItem = curPage
        adapter!!.clearCache()

        return view
    }

    override fun onPause() {
        super.onPause()
        Timber.d("Library frag on pause state")

    }

    override fun onDestroyView() {
        curPage = view.libraryPager.currentItem
        super.onDestroyView()
        Timber.d("Library frag on destroy view")
        try {
            fragmentManager.beginTransaction().apply {
                currentPlFrag?.let { remove(it) }
                currentAlFrag?.let { remove(it) }
                currentArFrag?.let { remove(it) }
            }.commit()
        } catch (e: IllegalStateException) {
            Timber.w(e, "IllegalStateException committing fragment removal in LibraryFragment," +
                    "likely the base activity is being destroyed")
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Timber.d("Library frag saving self state")
//        outState?.putParcelable("pager", view.libraryPager.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        Timber.d("Library frag - on resume")
        super.onResume()
        //view.libraryPager.adapter.notifyDataSetChanged()

    }
}