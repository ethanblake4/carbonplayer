package com.carbonplayer.ui.main

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.annotation.ColorInt
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Album
import com.carbonplayer.ui.helpers.NowPlayingHelper
import com.carbonplayer.ui.intro.IntroActivity
import com.carbonplayer.ui.main.library.AlbumController
import com.carbonplayer.ui.main.library.LibraryController
import com.carbonplayer.ui.settings.Settings
import com.carbonplayer.ui.transition.SimpleScaleTransition
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.newIntent
import com.carbonplayer.utils.ui.VolumeObserver
import icepick.Icepick
import kotlinx.android.synthetic.main.controller_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    lateinit var npHelper: NowPlayingHelper
    val volumeObserver = VolumeObserver({ npHelper.maybeHandleVolumeEvent() })
    lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        applicationContext.contentResolver.registerContentObserver(
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

        foregroundToolbar.inflateMenu(R.menu.menu_main)

        foregroundToolbar.setPadding(foregroundToolbar.paddingLeft,
                foregroundToolbar.paddingTop + IdentityUtils.getStatusBarHeight(resources),
                    foregroundToolbar.paddingRight, foregroundToolbar.paddingBottom)

        foregroundToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_item_settings -> {
                    startActivity(newIntent<Settings>())
                    true
                }
                else -> false
            }

        }

        router = Conductor.attachRouter(this, main_controller_container, savedInstanceState)


        npHelper = NowPlayingHelper(this)

        bottomNavContainer.setPadding(0, 0, 0,
                IdentityUtils.getNavbarHeight(resources))
        bottom_nav.selectedItemId = R.id.action_home

        bottom_nav.setOnNavigationItemSelectedListener { item ->

            val initialFrag = when (item.itemId) {
                R.id.action_topcharts -> TopChartsController()
                R.id.action_home -> HomeController()
                R.id.action_library -> LibraryController()
                else -> LibraryController()
            }

            //initialFrag.exitTransition = Fade()

            main_controller_container.animate().alpha(0.0f).setDuration(100).start()

            Timber.d("Will add new fragment")

            Handler().postDelayed({

                Timber.d("Adding new fragment")

                router.setRoot(RouterTransaction.with(initialFrag))
                router.popToRoot()

                main_controller_container.translationY = 100.0f
                main_controller_container.animate().translationY(0.0f)
                        .alpha(1.0f).setDuration(200).start()
            }, 100)

            true
        }

        Timber.d("Adding HomeController")
        router.setRoot(RouterTransaction.with(HomeController()))
        //val initialFrag = HomeController()
        //initialFrag.exitTransition = Fade()

        /*fragmentManager.beginTransaction()
                .replace(R.id.main_controller_container, initialFrag)
                .commit()*/

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Icepick.saveInstanceState(this, outState)
        super.onSaveInstanceState(outState)
    }

    fun gotoAlbum(album: Album, image: ImageView, content: View, @ColorInt textColor: Int,
                  @ColorInt mainColor: Int, @ColorInt bodyColor: Int, text: TextView, text2: TextView) {

        val frag = AlbumController(album.id, textColor, mainColor, bodyColor)

        router.pushController(RouterTransaction.with(frag)
                .pushChangeHandler(SimpleScaleTransition(this))
                .popChangeHandler(SimpleScaleTransition(this)))


        /*fragmentManager.beginTransaction()
                .addSharedElement(image, image.transitionName)
                .addSharedElement(content, content.transitionName)
                .addSharedElement(text, text.transitionName)
                .addSharedElement(text2, text2.transitionName)
                .replace(R.id.main_controller_container, frag)
                .addToBackStack("base")
                .commit()*/
    }

    /*fun gotoAlbum2(album: Album, layoutRoot: FrameLayout) {
        //albumController = AlbumController(album.id)
        //albumController!!.makeAlbum(this, main_controller_container, layoutRoot)

        router.pushController(RouterTransaction.with(frag))

    }*/

    fun showAlbumPopup(view: View, album: Album) {

        val pop = PopupMenu(ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view)
        pop.inflate(R.menu.album_popup)

        pop.setOnMenuItemClickListener { item ->

            val trackList = MusicLibrary.getInstance().getAllAlbumTracks(album.id)

            when (item.itemId) {
                R.id.menu_play_next -> {
                    npHelper.insertNext(trackList)
                }
                R.id.menu_add_to_queue -> {
                    npHelper.insertAtEnd(trackList)
                }
                else -> {
                    Toast.makeText(this, "This action is not supported yet",
                            Toast.LENGTH_SHORT).show()
                }
            }

            return@setOnMenuItemClickListener true
        }
        pop.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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

        if(router.backstackSize > 0){
            router.popCurrentController()
        } else this.finish()
    }
}
