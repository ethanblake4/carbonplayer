package com.carbonplayer.ui.main

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.transition.Fade
import android.transition.TransitionInflater
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.Album
import com.carbonplayer.ui.helpers.BackstackSaveable
import com.carbonplayer.ui.helpers.NowPlayingHelper
import com.carbonplayer.ui.intro.IntroActivity
import com.carbonplayer.utils.BundleBuilder
import com.carbonplayer.utils.IdentityUtils
import icepick.Icepick
import kotlinx.android.synthetic.main.controller_main.*
import timber.log.Timber



class MainActivity : AppCompatActivity() {

    var libraryFrag: BackstackSaveable? = null
    lateinit var npHelper: NowPlayingHelper

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        volumeControlStream = AudioManager.STREAM_MUSIC

        if (CarbonPlayerApplication.instance.preferences.firstStart) {
            val i = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(i)
            Timber.d("Started activity")
            return
        }

        setContentView(R.layout.controller_main)

        npHelper = NowPlayingHelper(this)

        bottomNavContainer.setPadding(0, 0, 0, IdentityUtils.getNavbarHeight(resources))
        bottom_nav.selectedItemId = R.id.action_home

        bottom_nav.setOnNavigationItemSelectedListener { item ->

            var initialFrag = when(item.itemId) {
                R.id.action_topcharts -> TopChartsPageFragment()
                R.id.action_home -> HomeFragment()
                R.id.action_library -> AlbumPageFragment()
                else -> AlbumPageFragment()
            }

            if(initialFrag is AlbumPageFragment) libraryFrag = initialFrag
            else libraryFrag = null

            initialFrag.exitTransition = Fade()

            main_controller_container.animate().alpha(0.0f).setDuration(200).start()

            Handler().postDelayed({
                fragmentManager.beginTransaction().replace(R.id.main_controller_container, initialFrag)
                        .commit()
                main_controller_container.translationY = 100.0f
                main_controller_container.animate().setStartDelay(50).translationY(0.0f).alpha(1.0f).setDuration(200).start()
            }, 200)

            true
        }

        val initialFrag = HomeFragment()
        initialFrag.exitTransition = Fade()

        fragmentManager.beginTransaction().replace(R.id.main_controller_container, initialFrag)
                .commit()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Icepick.saveInstanceState(this, outState)
        super.onSaveInstanceState(outState)
    }

    fun gotoAlbum(album: Album, image: ImageView, content: View, textColor: Int, mainColor: Int, text: TextView, text2: TextView) {

        libraryFrag!!.saveStateForBackstack()

        val frag = AlbumFragment()
        frag.allowEnterTransitionOverlap = true
        frag.arguments = BundleBuilder().putString("album_id", album.id)
                .putInt("text_color", textColor).putInt("main_color", mainColor)
                .build()
        frag.sharedElementEnterTransition = TransitionInflater.from(this).inflateTransition(R.transition.album_click)
        frag.sharedElementReturnTransition = TransitionInflater.from(this).inflateTransition(R.transition.album_click)

        frag.enterTransition = Fade(Fade.IN)


        fragmentManager.beginTransaction()
                .addSharedElement(image, image.transitionName)
                .addSharedElement(content, content.transitionName)
                .addSharedElement(text, text.transitionName)
                .addSharedElement(text2, text2.transitionName)
                .replace(R.id.main_controller_container, frag)
                .addToBackStack("base")
                .commit()

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return npHelper.handleVolumeEvent(keyCode)
    }

    override fun onResume() {

        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        npHelper.onDestroy()
    }

    override fun onBackPressed() {
        Timber.d("backStackEntry count %d", fragmentManager.backStackEntryCount)
        if(fragmentManager.backStackEntryCount > 0) {
            Timber.d("popping")
            fragmentManager.popBackStack()
        } else this.finish()
    }
}
