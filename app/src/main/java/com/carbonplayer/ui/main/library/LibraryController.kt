package com.carbonplayer.ui.main.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.carbonplayer.R
import com.carbonplayer.ui.main.MainActivity
import com.carbonplayer.utils.general.IdentityUtils
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_main.view.*
import timber.log.Timber


class LibraryController : Controller() {

    var adapter: RouterPagerAdapter? = null
    var curPage = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {


        val view = inflater.inflate(R.layout.activity_main, container, false)

        (view.tab_layout.layoutParams as AppBarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2

        view.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { a, i ->
            (activity as MainActivity).scrollCb(i)

        })

        if(adapter == null) {
            adapter = object : RouterPagerAdapter(this) {

                override fun configureRouter(router: Router, position: Int) {
                    if(!router.hasRootController()) {
                        when (position) {
                            0 -> {
                                val playlists = PlaylistPageController()
                                //playlists.attachToHandle = view.fastscroll
                                router.setRoot(RouterTransaction.with(playlists))
                            }
                            1 -> {
                                val artists = ArtistsPageController()
                                router.setRoot(RouterTransaction.with(artists))
                            }
                            2 -> {
                                val albums = AlbumPageController()
                                router.setRoot(RouterTransaction.with(albums))
                            }
                            3 -> {
                                val songs = SongsPageController()
                                router.setRoot(RouterTransaction.with(songs))
                            }
                        }
                    }
                }

                override fun getPageTitle(position: Int): CharSequence =
                    when(position) {
                        0 -> "Playlists"
                        1 -> "Artists"
                        2 -> "Albums"
                        3 -> "Songs"
                        else -> ""
                    }


                override fun getItemPosition(`object`: Any): Int {
                    return POSITION_NONE
                }

                override fun getCount(): Int {
                    return 4
                }

            }
        }

        Timber.d("curpager=%d", curPage)

        (view.libraryPager as ViewPager).adapter = adapter
        view.tab_layout.setupWithViewPager((view.libraryPager as ViewPager))

        (view.libraryPager as ViewPager).currentItem = curPage

        return view
    }


    override fun onDestroyView(view: View) {

        curPage = (view.libraryPager as ViewPager).currentItem
        super.onDestroyView(view)
        Timber.d("Library ctrl on destroy view")
    }
}