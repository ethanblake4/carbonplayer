package com.carbonplayer.ui.main.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.carbonplayer.R
import com.carbonplayer.utils.general.IdentityUtils
import kotlinx.android.synthetic.main.activity_main.view.*
import timber.log.Timber


class LibraryController : Controller() {

    var adapter: RouterPagerAdapter? = null
    var curPage = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {

        /*Timber.d("Library - onCreateView. savedInstanceState= %s",
                if(savedInstanceState == null) "null" else "not null")*/

        val view = inflater.inflate(R.layout.activity_main, container, false)

        //view.toolbar.inflateMenu(R.menu.menu_main)

        view.toolbar.setPadding(view.toolbar.paddingLeft, view.toolbar.paddingTop +
                IdentityUtils.getStatusBarHeight(resources),
                view.toolbar.paddingRight, view.toolbar.paddingBottom)

        //view.toolbar.layoutParams.height += IdentityUtils.getStatusBarHeight(resources)
        //(view.app_bar.layoutParams as CoordinatorLayout.LayoutParams).topMargin +=
        //        IdentityUtils.getStatusBarHeight(resources) / 2
        /*(view.toolbar.layoutParams as AppBarLayout.LayoutParams).bottomMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2*/
        /*(view.tab_layout.layoutParams as AppBarLayout.LayoutParams).topMargin +=
                IdentityUtils.getStatusBarHeight(resources) / 2*/

        //view.statusBarCover.layoutParams.height = IdentityUtils.getStatusBarHeight(resources)

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
                        }
                    }
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

        /*view.libraryPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {

                Handler().postDelayed({

                    val recycler  = when(position) {
                        0 -> playlists.view?.main_recycler
                        1 -> artists.view?.main_recycler
                        2 -> albums.view?.main_recycler
                        else -> null
                    }

                    recycler?.let {
                        view.fastscroll.setRecyclerView(it)
                        view.fastscroll.visibility = View.VISIBLE
                        view.fastscroll.invalidate()
                    }
                }, 500)

            }

            override fun onPageScrollStateChanged(state: Int) { }

            override fun onPageScrolled(position: Int, positionOffset: Float, offsetPixels: Int) { }
        })*/

        view.libraryPager.currentItem = curPage
        //adapter!!.clearCache()

        return view
    }

    /*override fun onPause() {
        super.onPause()
        Timber.d("Library frag on pause state")

    }*/

    override fun onDestroyView(view: View) {
        curPage = view.libraryPager.currentItem
        super.onDestroyView(view)
        Timber.d("Library ctrl on destroy view")
    }

    /*override fun onSaveInstanceState(outState: Bundle?) {
        Timber.d("Library frag saving self state")
//        outState?.putParcelable("pager", view.libraryPager.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        Timber.d("Library frag - on resume")
        super.onResume()
        //view.libraryPager.adapter.notifyDataSetChanged()

    }*/
}