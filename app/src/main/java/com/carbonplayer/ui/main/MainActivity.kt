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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.Artist
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.skyjam.TopChartsGenres
import com.carbonplayer.ui.helpers.NowPlayingHelper
import com.carbonplayer.ui.intro.IntroActivity
import com.carbonplayer.ui.main.library.AlbumController
import com.carbonplayer.ui.main.library.ArtistController
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

        // If this is the first-time start of the app,
        // launch the IntroActivity
        if (CarbonPlayerApplication.instance.preferences.firstStart) {
            val i = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(i)
            Timber.d("Started IntroActivity")
        }

        // Setup view
        setContentView(R.layout.controller_main)

        // Setup now playing view
        npHelper = NowPlayingHelper(this)

        /** Observe system volume changes and update UI **/
        volumeObserver = VolumeObserver({ npHelper.maybeHandleVolumeEvent() })
        applicationContext.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                volumeObserver)

        // Adjusting volume should adjust media volume
        volumeControlStream = AudioManager.STREAM_MUSIC

        // We don't want shadows be drawn under the overlay appbar
        functionalAppbar.outlineProvider = null

        // Since carbon uses translucent window decoration, we need to add padding for
        // the toolbar so it doesn't get drawn under the system status bar
        foregroundToolbar.setPadding(foregroundToolbar.paddingLeft,
                foregroundToolbar.paddingTop + IdentityUtils.getStatusBarHeight(resources),
                    foregroundToolbar.paddingRight, foregroundToolbar.paddingBottom)


        // Setup Conductor
        router = Conductor.attachRouter(this, main_controller_container, savedInstanceState)

        // Again: add padding for nav bar to bottom nav because of translucent window decor
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

             when (item.itemId) {
                R.id.action_topcharts -> {
                    // If top charts is selected, enable the genre spinner
                    main_actionbar_text.visibility = View.GONE
                    topChartsSpinner.visibility = View.VISIBLE
                    topChartsSpinner.adapter = ArrayAdapter(
                            this,
                            R.layout.topcharts_spinner_item,
                            listOf("Top Charts")
                    )
                }
                R.id.action_home -> {
                    main_actionbar_text.visibility = View.VISIBLE
                    topChartsSpinner.visibility = View.GONE
                    main_actionbar_text.text ="Home"
                }
                R.id.action_library -> {
                    main_actionbar_text.visibility = View.VISIBLE
                    topChartsSpinner.visibility = View.GONE
                    main_actionbar_text.text ="My Library"
                }
                else -> {
                    main_actionbar_text.visibility = View.VISIBLE
                    topChartsSpinner.visibility = View.GONE
                    main_actionbar_text.text = "Carbon Player"
                }
            }

            lastAppbarText = main_actionbar_text.text.toString()

            // Animate the controller container out
            main_controller_container.animate().alpha(0.0f).setDuration(100).start()

            Timber.d("Will add new fragment")

            Handler().postDelayed({

                Timber.d("Adding new fragment")

                // Replace content of the controller container with the new controller
                router.setRoot(RouterTransaction.with(initialFrag))
                router.popToRoot()

                // Animate the controller container back in
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

    // When the genre list is retrieved from the network
    fun callbackWithTopChartsGenres(genres: List<TopChartsGenres.Genre>, callback: (String) -> Unit) {
        topChartsSpinner.adapter = ArrayAdapter(
                this,
                R.layout.topcharts_spinner_item,
                mutableListOf("Top Charts").apply { addAll(genres.map { it.title }) }
        )

        topChartsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Should not happen
                Timber.d("Nothing selected")
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Callback to the TopChartsController so it can update its content
                if(position == 0) callback(TopChartsController.DEFAULT_CHART)
                else callback(genres[position - 1].id)
            }
        }
    }

    // When an album is selected
    fun gotoAlbum(album: IAlbum, swatchPair: PaletteUtil.SwatchPair) {

        val frag = AlbumController(album, swatchPair)

        router.pushController(RouterTransaction.with(frag)
                .pushChangeHandler(SimpleScaleTransition(this))
                .popChangeHandler(SimpleScaleTransition(this)))

        lastAppbarText = main_actionbar_text.text.toString()
        main_actionbar_text.text = ""

    }

    // When an artist is selected
    fun gotoArtist(artist: Artist, swatchPair: PaletteUtil.SwatchPair) {
        val frag = ArtistController(artist, swatchPair)

        router.pushController(RouterTransaction.with(frag)
                .pushChangeHandler(SimpleScaleTransition(this))
                .popChangeHandler(SimpleScaleTransition(this)))

        lastAppbarText = main_actionbar_text.text.toString()
        main_actionbar_text.text = ""
    }

    fun scrollCb(dy: Int) {
        curAppbarOffset = dy
        functionalAppbar.postOnAnimation {
            functionalAppbar.translationY = curAppbarOffset.toFloat()
            functionalAppbar.invalidate()
        }
    }

    /** When an album's menu button is selected **/
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
                R.id.menu_share -> {
                    val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain";
                    val shareBody = "https://play.google.com/music/m/${album.albumId}?signup_if_needed=1"
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, "Share via"));
                    
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

            main_actionbar_text.text = lastAppbarText
        }

    }
}
