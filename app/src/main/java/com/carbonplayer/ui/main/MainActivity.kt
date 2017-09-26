package com.carbonplayer.ui.main

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.transition.Fade
import android.transition.TransitionInflater
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.ui.helpers.NowPlayingHelper
import com.carbonplayer.ui.intro.IntroActivity
import com.carbonplayer.ui.main.library.AlbumFragment
import com.carbonplayer.ui.main.library.LibraryFragment
import com.carbonplayer.utils.BundleBuilder
import com.carbonplayer.utils.IdentityUtils
import com.carbonplayer.utils.VolumeObserver
import icepick.Icepick
import kotlinx.android.synthetic.main.controller_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    lateinit var npHelper: NowPlayingHelper
    val volumeObserver = VolumeObserver({ npHelper.maybeHandleVolumeEvent() })

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        applicationContext.contentResolver.registerContentObserver (
                android.provider.Settings.System.CONTENT_URI, true,
                volumeObserver)

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
                R.id.action_library -> LibraryFragment()
                else -> LibraryFragment()
            }

            initialFrag.exitTransition = Fade()

            main_controller_container.animate().alpha(0.0f).setDuration(200).start()

            Handler().postDelayed({
                val transaction = fragmentManager.beginTransaction()
                        .replace(R.id.main_controller_container, initialFrag)

                if(Build.VERSION.SDK_INT >= 24) {
                    transaction.commitNow()
                } else transaction.commit()

                main_controller_container.translationY = 100.0f
                main_controller_container.animate().setStartDelay(50).translationY(0.0f)
                        .alpha(1.0f).setDuration(200).start()
            }, 200)

            true
        }

        val initialFrag = HomeFragment()
        initialFrag.exitTransition = Fade()

        fragmentManager.beginTransaction()
                .replace(R.id.main_controller_container, initialFrag)
                .commit()

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Icepick.saveInstanceState(this, outState)
        super.onSaveInstanceState(outState)
    }

    fun gotoAlbum(album: Album, image: ImageView, content: View, textColor: Int,
                  mainColor: Int, text: TextView, text2: TextView) {

        val frag = AlbumFragment()
        frag.allowEnterTransitionOverlap = true
        frag.arguments = BundleBuilder().putString("album_id", album.id)
                .putInt("text_color", textColor).putInt("main_color", mainColor)
                .build()
        frag.sharedElementEnterTransition = TransitionInflater.from(this)
                .inflateTransition(R.transition.album_click)
        frag.sharedElementReturnTransition = TransitionInflater.from(this)
                .inflateTransition(R.transition.album_click)

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

    fun showAlbumPopup(view: View, trackList: List<MusicTrack>) {
        val pop = PopupMenu(this, view)
        pop.inflate(R.menu.album_popup)
        pop.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.menu_play_next -> {
                    npHelper.trackQueue.insertNext(trackList)
                }
                R.id.menu_add_to_queue -> {
                    npHelper.trackQueue.insertAtEnd(trackList)
                }
                else -> {
                    Toast.makeText(this, "This action is not supported yet",
                            Toast.LENGTH_SHORT).show()
                }
            }

            return@setOnMenuItemClickListener true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed() // Fixes issue with HTC One
            return true
        }
        return npHelper.handleVolumeEvent(keyCode)
    }

    override fun onResume() {

        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        npHelper.onDestroy()
        applicationContext.contentResolver.unregisterContentObserver(volumeObserver)
    }

    override fun onBackPressed() {
        Timber.d("backStackEntry count %d", fragmentManager.backStackEntryCount)
        if(fragmentManager.backStackEntryCount > 0) {
            Timber.d("popping")
            fragmentManager.popBackStack()
        } else this.finish()
    }
}
