package com.carbonplayer.ui.main.explore

import android.support.annotation.Keep
import android.support.v4.view.ViewPager
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.contains
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.carbonplayer.R
import com.carbonplayer.model.entity.api.StationCategory
import com.carbonplayer.ui.widget.helpers.ObjectAtPositionInterface
import kotlinx.android.synthetic.main.controller_browse_stations.view.*
import timber.log.Timber

class BrowseStationsController (
        val categoryRoot: StationCategory,
        val stationCallback: (StationCategory) -> Unit,
        val pagerCallback: (ViewPager) -> Unit
) : Controller() {

    @Keep constructor() : this (StationCategory("", "", listOf()), {}, {})

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val root = inflater.inflate(R.layout.controller_browse_stations, container, false)

        root.browseStationsPager.adapter = object: RouterPagerAdapter(this), ObjectAtPositionInterface {

            private var objects = SparseArray<Any>()

            override fun configureRouter(router: Router, position: Int) {
                router.setRoot(RouterTransaction.with(BrowseStationsTab(
                        categoryRoot.subcategories?.get(position) ?:
                                StationCategory.DEFAULT, { station ->
                    Timber.d("click'd @ browseCtrl")
                    stationCallback(station)
                })))
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val `object` = super.instantiateItem(container, position)
                objects.put(position, `object`)
                return `object`
            }

            override fun getObjectAtPosition(position: Int): Any? {
                if(!objects.contains(position)) return null
                return objects.get(position)
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                objects.remove(position)
                super.destroyItem(container, position, `object`)
            }

            override fun getCount() = categoryRoot.subcategories?.size ?: 0

            override fun getPageTitle(position: Int): CharSequence? =
                    categoryRoot.subcategories?.get(position)?.display_name ?: ""

        }

        return root
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        pagerCallback(view.browseStationsPager)
    }

}