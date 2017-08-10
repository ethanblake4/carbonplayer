package com.carbonplayer.ui.main

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.transition.*
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.Album
import com.carbonplayer.ui.intro.IntroActivity
import com.carbonplayer.utils.BundleBuilder
import com.carbonplayer.utils.IdentityUtils
import icepick.Icepick
import icepick.State

import timber.log.Timber
import kotlinx.android.synthetic.main.controller_main.*

class MainActivity : AppCompatActivity() {

    lateinit var pageFrag: AlbumPageFragment

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (CarbonPlayerApplication.instance.preferences.firstStart) {
            val i = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(i)
            Timber.d("Started activity")
            return
        }

        setContentView(R.layout.controller_main)

        bottomNavContainer.setPadding(0, 0, 0, IdentityUtils.getNavbarHeight(resources))
        bottom_nav.selectedItemId = R.id.action_home
        pageFrag = AlbumPageFragment()
        pageFrag.exitTransition = Fade()

        fragmentManager.beginTransaction().add(R.id.main_controller_container, pageFrag).commit()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Icepick.saveInstanceState(this, outState)
        super.onSaveInstanceState(outState)
    }

    fun gotoAlbum(album: Album, image: ImageView, content: View, textColor: Int, mainColor: Int, text: TextView) {

        pageFrag.saveStateForBackstack()

        val frag = AlbumFragment()
        frag.allowEnterTransitionOverlap = true
        frag.arguments = BundleBuilder().putString("album_id", album.id)
                .putInt("text_color", textColor).putInt("main_color", mainColor)
                .build()
        frag.sharedElementEnterTransition = TransitionInflater.from(this).inflateTransition(R.transition.album_click)

        frag.enterTransition = Fade(Fade.IN)

        fragmentManager.beginTransaction()
                .addSharedElement(image, image.transitionName)
                .addSharedElement(content, content.transitionName)
                .addSharedElement(text, text.transitionName)
                .replace(R.id.main_controller_container, frag)
                .addToBackStack(null)
                .commit()

    }

    override fun onBackPressed() {
        if(fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else super.onBackPressed()
    }
}
