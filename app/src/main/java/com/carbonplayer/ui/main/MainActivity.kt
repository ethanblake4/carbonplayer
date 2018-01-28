package com.carbonplayer.ui.main

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.ui.helpers.NowPlayingHelper
import com.carbonplayer.ui.intro.IntroActivity
import com.carbonplayer.ui.main.library.AlbumController
import com.carbonplayer.ui.main.library.LibraryController
import com.carbonplayer.ui.settings.Settings
import com.carbonplayer.ui.transition.SimpleScaleTransition
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.newIntent
import com.carbonplayer.utils.ui.PaletteUtil
import com.carbonplayer.utils.ui.VolumeObserver
import icepick.Icepick
import kotlinx.android.synthetic.main.controller_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    lateinit var npHelper: NowPlayingHelper
    private lateinit var volumeObserver: VolumeObserver
    private lateinit var router: Router

    private var curAppbarOffset = 0
    private var lastAppbarText = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (CarbonPlayerApplication.instance.preferences.firstStart) {
            val i = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(i)
            Timber.d("Started activity")
        }

        setContentView(R.layout.controller_main)

        npHelper = NowPlayingHelper(this)

        volumeObserver = VolumeObserver({ npHelper.maybeHandleVolumeEvent() })

        applicationContext.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                volumeObserver)

        volumeControlStream = AudioManager.STREAM_MUSIC

        functionalAppbar.outlineProvider = null

        foregroundToolbar.setPadding(foregroundToolbar.paddingLeft,
                foregroundToolbar.paddingTop + IdentityUtils.getStatusBarHeight(resources),
                    foregroundToolbar.paddingRight, foregroundToolbar.paddingBottom)


        router = Conductor.attachRouter(this, main_controller_container, savedInstanceState)

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

            main_actionbar_text.text = when (item.itemId) {
                R.id.action_topcharts -> "Top Charts"
                R.id.action_home -> "Home"
                R.id.action_library -> "My Library"
                else -> "Carbon Player"
            }

            lastAppbarText = main_actionbar_text.text.toString()

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

        main_actionbar_more.setOnClickListener {
            PopupMenu(this, main_actionbar_more).apply{
                inflate(R.menu.menu_main)
                show()
                setOnMenuItemClickListener {
                    if(it.itemId == R.id.menu_item_settings)
                        startActivity(newIntent<Settings>())

                    true
                }
            }
        }

        Timber.d("Adding HomeController")
        router.setRoot(RouterTransaction.with(HomeController()))

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Icepick.saveInstanceState(this, outState)
        super.onSaveInstanceState(outState)
    }

    fun gotoAlbum(album: IAlbum, swatchPair: PaletteUtil.SwatchPair) {

        val frag = AlbumController(album, swatchPair)

        router.pushController(RouterTransaction.with(frag)
                .pushChangeHandler(SimpleScaleTransition(this))
                .popChangeHandler(SimpleScaleTransition(this)))

        lastAppbarText = main_actionbar_text.text.toString()
        main_actionbar_text.text = ""

    }

    fun gotoAlbum(album: SkyjamAlbum, swatchPair: PaletteUtil.SwatchPair) {

    }

    fun scrollCb(dy: Int) {
        Timber.d("scrollOffset: %d", dy)
        curAppbarOffset = dy
        functionalAppbar.postOnAnimation {
            functionalAppbar.translationY = curAppbarOffset.toFloat()
            functionalAppbar.invalidate()
        }
    }

    fun showAlbumPopup(view: View, album: IAlbum) {

        val pop = PopupMenu(ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view)
        pop.inflate(R.menu.album_popup)

        pop.setOnMenuItemClickListener { item ->

            val trackList = MusicLibrary.getAllAlbumTracks(album)

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

        if(router.backstackSize == 1) {
            //foregroundToolbar.navigationIcon = null
            main_actionbar_text.text = lastAppbarText
        }

    }
}
