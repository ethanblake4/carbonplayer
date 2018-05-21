package com.carbonplayer.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.*
import com.bumptech.glide.Glide
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.MusicLibrary
import com.carbonplayer.model.entity.*
import com.carbonplayer.model.entity.api.TopChartsGenres
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.IArtist
import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.MediaType
import com.carbonplayer.model.entity.enums.RadioFeedReason
import com.carbonplayer.model.entity.proto.identifiers.ArtistIdV1Proto
import com.carbonplayer.model.entity.proto.innerjam.elements.ActionListV1Proto
import com.carbonplayer.model.entity.proto.innerjam.elements.ActionListV1Proto.ActionList.Action.TypeCase.*
import com.carbonplayer.model.entity.proto.innerjam.renderers.ContextMenuV1Proto
import com.carbonplayer.model.entity.radio.RadioSeed
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.carbonplayer.model.entity.skyjam.SkyjamAlbum
import com.carbonplayer.model.entity.skyjam.SkyjamArtist
import com.carbonplayer.model.entity.skyjam.SkyjamTrack
import com.carbonplayer.model.network.Protocol
import com.carbonplayer.ui.helpers.NowPlayingHelper
import com.carbonplayer.ui.intro.IntroActivity
import com.carbonplayer.ui.main.adapters.SuggestionsAdapter
import com.carbonplayer.ui.main.library.*
import com.carbonplayer.ui.settings.Settings
import com.carbonplayer.ui.transition.SimpleScaleTransition
import com.carbonplayer.ui.widget.helpers.BottomNavigationHelper
import com.carbonplayer.utils.addToAutoDispose
import com.carbonplayer.utils.carbonAnalytics
import com.carbonplayer.utils.general.IdentityUtils
import com.carbonplayer.utils.general.MathUtils
import com.carbonplayer.utils.logEntityEvent
import com.carbonplayer.utils.newIntent
import com.carbonplayer.utils.ui.PaletteUtil
import com.carbonplayer.utils.ui.VolumeObserver
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import icepick.Icepick
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.controller_main.*
import kotlinx.android.synthetic.main.nowplaying.*
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import timber.log.Timber
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    lateinit var npHelper: NowPlayingHelper
    private lateinit var volumeObserver: VolumeObserver
    private lateinit var router: Router

    private var curAppbarOffset = 0
    private var lastAppbarText = ""
    private lateinit var suggestionsAdapter: SuggestionsAdapter

    private var setupSpinner = false

    /* Publishes new search text */
    private val suggestSubject = PublishSubject.create<String>()

    /* Offset for views that get covered by NPUI */
    val bottomInset: Int get() = MathUtils.dpToPx2(resources,
            if(nowplaying_frame.visibility == View.VISIBLE)
                NowPlayingHelper.HEIGHT_DP else 0)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // If this is the first-time start of the app,
        // launch the IntroActivity
        if (CarbonPlayerApplication.instance.preferences.firstStart) {
            val i = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(i)
            Timber.d("Started IntroActivity")
        } else {
            if (CarbonPlayerApplication.instance.preferences.isCarbonTester)
                Crashlytics.setString("mtoken",
                        CarbonPlayerApplication.instance.preferences.masterToken)
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

        searchToolbar.setPadding(searchToolbar.paddingLeft,
                searchToolbar.paddingTop + IdentityUtils.getStatusBarHeight(resources),
                searchToolbar.paddingRight, searchToolbar.paddingBottom)

        // Setup Conductor
        router = Conductor.attachRouter(
                this, main_controller_container, savedInstanceState)

        // Again: add padding for nav bar to bottom nav because of translucent window decor
        bottomNavContainer.setPadding(0, 0, 0,
                IdentityUtils.getNavbarHeight(resources))
        bottom_nav.selectedItemId = R.id.action_home

        BottomNavigationHelper.disableShiftMode(bottom_nav)

        bottom_nav.setOnNavigationItemSelectedListener { item ->

            val initialFrag = when (item.itemId) {
                R.id.action_topcharts -> TopChartsController()
                R.id.action_home -> HomeController()
                R.id.action_library -> LibraryController()
                R.id.action_explore -> ExploreController()
                else -> LibraryController()
            }

             when (item.itemId) {
                R.id.action_topcharts -> {
                    // If top charts is selected, enable the genre spinner
                    main_actionbar_text.visibility = View.GONE
                    topChartsSpinner.visibility = View.VISIBLE
                    if(!setupSpinner) {
                        topChartsSpinner.adapter = ArrayAdapter(
                                this,
                                R.layout.topcharts_spinner_item,
                                listOf(getString(R.string.top_charts))
                        )
                        setupSpinner = true
                    }
                }
                R.id.action_home -> {
                    main_actionbar_text.visibility = View.VISIBLE
                    topChartsSpinner.visibility = View.GONE
                    main_actionbar_text.setText(R.string.home)
                }
                R.id.action_library -> {
                    main_actionbar_text.visibility = View.VISIBLE
                    topChartsSpinner.visibility = View.GONE
                    main_actionbar_text.setText(R.string.my_library)
                }
                R.id.action_explore -> {
                    main_actionbar_text.visibility = View.VISIBLE
                    topChartsSpinner.visibility = View.GONE
                    main_actionbar_text.setText(R.string.explore)
                }
                else -> {
                    main_actionbar_text.visibility = View.VISIBLE
                    topChartsSpinner.visibility = View.GONE
                    main_actionbar_text.setText(R.string.app_name)
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
                gravity = Gravity.TOP
                show()
                setOnMenuItemClickListener {
                    if(it.itemId == R.id.menu_item_settings)
                        startActivity(newIntent<Settings>())
                    true
                }
            }
        }

        main_actionbar_search.setOnClickListener {

            // get the center for the clipping circle
            val pos = IntArray(2).apply {
                main_actionbar_search.getLocationInWindow(this)
            }

            val cx = pos[0] + main_actionbar_search.width / 2
            val cy = pos[1] + main_actionbar_search.height / 2

            // get the final radius for the clipping circle
            val finalRadius = Math.hypot(IdentityUtils.displayWidth2(this).toDouble(),
                    searchToolbar.height.toDouble()).toFloat()

            // create the animator for this view (the start radius is zero)
            val anim = ViewAnimationUtils
                    .createCircularReveal(searchToolbar, cx, cy, 0f, finalRadius)


            // make the view visible and start the animation
            searchToolbar.visibility = View.VISIBLE
            anim.duration = 300
            anim.start()

            Handler().postDelayed({
                searchQuery.requestFocus()

                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager
                inputMethodManager.showSoftInput(searchQuery, 0)
                /*inputMethodManager.toggleSoftInputFromWindow(
                        searchQuery.applicationWindowToken,
                        InputMethodManager.SHOW_FORCED, 0)*/
            }, 300)

        }

        suggestionsRecycler.layoutManager = LinearLayoutManager(this)

        searchQuery.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                suggestSubject.onNext(s.toString())
            }
        })

        searchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                        searchQuery.applicationWindowToken, 0)
                val frag = SearchController(searchQuery.text.toString())

                goto(frag)

                closeSearch()
            }
            true
        }

        val f = HomeController()

        Timber.d("Adding HomeController")
        router.setRoot(RouterTransaction.with(f))

        KeyboardVisibilityEvent.registerEventListener(this, { isOpen ->
            if(!isOpen) closeSearch()
        })

        sub()

        suggestSubject.onNext("")

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Icepick.saveInstanceState(this, outState)
        super.onSaveInstanceState(outState)
    }

    private fun sub() {
        suggestSubject.debounce(300, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .flatMap({ query ->
                    Protocol.suggest(this, query)
                })
                .map({ response -> response.suggested_queries })
                .retry(2)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ suggestions ->
                    suggestionsRecycler.adapter = SuggestionsAdapter(suggestions.take(6),{ itm ->
                        if(itm.entity?.album != null) gotoAlbum(itm.entity.album,
                                PaletteUtil.DEFAULT_SWATCH_PAIR)
                        else { itm.suggestion_string?.let { s ->
                            val frag = SearchController(s)

                            goto(frag)
                            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE)
                                as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(
                                    searchQuery.applicationWindowToken, 0)
                            closeSearch()
                        } }
                    }, Glide.with(this))
                }, { err ->
                    Timber.e(err)
                    sub()
                }).addToAutoDispose()
    }

    // When the genre list is retrieved from the network
    fun callbackWithTopChartsGenres(genres: List<TopChartsGenres.Genre>) {
        topChartsSpinner.adapter = ArrayAdapter(
                this,
                R.layout.topcharts_spinner_item,
                mutableListOf(getString(R.string.top_charts))
                        .apply { addAll(genres.map { it.title }) }
        )
    }

    fun registerSpinnerCallback(genres: List<TopChartsGenres.Genre>, callback: (String) -> Unit) {
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
        goto(AlbumController(album, swatchPair))
    }

    fun gotoStation(station: SkyjamStation, swatchPair: PaletteUtil.SwatchPair) {
        goto(StationController(station, swatchPair))
    }

    fun gotoPlaylist(playlist: IPlaylist, drawable: Drawable?, swatchPair: PaletteUtil.SwatchPair) {
        if(playlist is Playlist)
            goto(PlaylistController(this, drawable, playlist, swatchPair))
        else {
            val futurePlaylist = Protocol.getNautilusPlaylist(this, playlist.shareToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            goto(PlaylistController(this, playlist, futurePlaylist, swatchPair))
        }
    }

    // When an artist is selected
    @JvmOverloads fun gotoArtist(artist: IArtist, swatchPair: PaletteUtil.SwatchPair,
                   load: Boolean = true) {

        val frag = if(artist is Artist || !load) {
            ArtistController(artist, swatchPair)
        } else {
            val futureArtist = Protocol.getNautilusArtist(this, artist.artistId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            ArtistController(artist, futureArtist as Observable<IArtist>, swatchPair)
        }

        goto(frag)
    }

    fun scrollCb(dy: Int) {
        curAppbarOffset = dy
        functionalAppbar.postOnAnimation {
            functionalAppbar.translationY = curAppbarOffset.toFloat()
            functionalAppbar.invalidate()
        }
    }

    private fun execProtoAction(action: ActionListV1Proto.ActionList.Action,
                        itemTitle: String, itemSubtitle: String) {

        val showErr = {
            Toast.makeText(this, "Could not perform action", Toast.LENGTH_LONG).show()
        }

        when(action.typeCase) {
            DISMISSALOPTION -> Timber.d("Dismiss")
            ALBUMRELEASELINK -> {
                Timber.d("Album release")
            }
            ARTISTLINK -> when(action.artistLink.typeCase) {
                ArtistIdV1Proto.ArtistId.TypeCase.CATALOG ->
                    gotoArtist(SkyjamArtist(action.artistLink.catalog.metajamCompactKey),
                            PaletteUtil.DEFAULT_SWATCH_PAIR)
                else -> showErr()
            }
            SHARECONTENTOPTION -> {
                shareImpl(action.shareContentOption.shareLink, action.shareContentOption.shareTitle)
            }
            STARTRADIOSTATIONOPTION -> {
                npHelper.startRadio(action.startRadioStationOption.radioSeedId)
            }
            PURCHASEOPTION -> showErr()
            INTERNALURL -> showErr()
            EXTERNALURL -> showErr()
            PLAYCONTAINERNEXTOPTION -> showErr()
            ADDCONTAINERTOQUEUEOPTION -> showErr()
            ADDCONTAINERTOUSERLIBRARYOPTION -> showErr()
            REMOVECONTAINERFROMUSERLIBRARYOPTION -> showErr()
            DOWNLOADCONTAINEROPTION -> showErr()
            REMOVEDOWNLOADEDCONTAINEROPTION -> showErr()
            EDITPLAYLISTOPTION -> showErr()
            DELETEPLAYLISTOPTION -> showErr()
            SUBSCRIBETOPLAYLISTOPTION -> showErr()
            UNSUBSCRIBEFROMPLAYLISTOPTION -> showErr()
            PLAYLISTLINK -> showErr()
            RADIOSTATIONLINK -> showErr()
            REQUESTCAPABILITY -> showErr()
            SHOWPAGEOPTION -> showErr()
            STARTPLAYABLEITEM -> showErr()
            NAVIGATIONACTION -> showErr()
            TYPE_NOT_SET -> showErr()
            null -> Timber.e("Null action list typeCase")
        }
    }

    fun showContextMenuPopup(view: View, menuDesc: ContextMenuV1Proto.ContextMenuDescriptor,
                             itemTitle: String, itemSubtitle: String) {
        val pop = PopupMenu(ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view)
        val menu = pop.menu

        menuDesc.itemsList.forEachIndexed { i, it ->
            menu.add(0, it.clickAction.typeCase.number, i, it.displayText)
        }

        pop.setOnMenuItemClickListener { item ->
            val menuItem = menuDesc.itemsList.first {
                it.clickAction.typeCase.number == item.itemId
            }
            execProtoAction(menuItem.clickAction, itemTitle, itemSubtitle)
            true
        }
    }

    /** When an album's menu button is selected **/
    fun showAlbumPopup(view: View, album: IAlbum) {

        val pop = PopupMenu(ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view)
        pop.inflate(R.menu.album_popup)

        carbonAnalytics.logEntityEvent("album_popup", album)

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
                    shareItem(album.albumId, album.name, MediaType.ALBUM)
                }
                R.id.menu_go_to_artist -> {
                    if(album is Album)
                    {
                        album.artists?.first()?.let {
                            gotoArtist(it, PaletteUtil.DEFAULT_SWATCH_PAIR)
                        }
                    }
                    else if(album is SkyjamAlbum) {
                        gotoArtist(SkyjamArtist(album.artistId.first()),
                                PaletteUtil.DEFAULT_SWATCH_PAIR)
                    }
                }
                R.id.menu_start_radio -> {
                    npHelper.startRadio(RadioSeed.TYPE_ALBUM, album.albumId)
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

    private fun shareItem(id: String, name: String, type: String) {
        carbonAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundleOf(
                FirebaseAnalytics.Param.ITEM_ID to id,
                FirebaseAnalytics.Param.ITEM_NAME to name,
                FirebaseAnalytics.Param.ITEM_CATEGORY to type
        ))
        val shareBody = "https://play.google.com/music/m/${id}?signup_if_needed=1"
        shareImpl(shareBody, "Share via")
    }

    private fun shareImpl(shareLink: String, shareTitle: String) {
        val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareLink)
        startActivity(Intent.createChooser(sharingIntent, shareTitle))
    }

    fun goto(controller: Controller) {

        scrollCb(0)

        Timber.d("goto controller with backstack size ${router.backstackSize}")

        if(router.backstackSize == 1) lastAppbarText = main_actionbar_text.text.toString()

        router.pushController(RouterTransaction.with(controller)
                .pushChangeHandler(SimpleScaleTransition(this))
                .popChangeHandler(SimpleScaleTransition(this)))

        main_actionbar_text.text = ""
    }

    fun showArtistPopup(view: View, artist: IArtist) {

        val pop = PopupMenu(ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view)
        pop.inflate(R.menu.artist_popup)

        carbonAnalytics.logEntityEvent("popup", artist)

        pop.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.menu_share -> {
                    shareItem(artist.artistId, artist.name, MediaType.ARTIST)
                }
                R.id.menu_start_radio -> {
                    npHelper.startRadio(RadioSeed.TYPE_ARTIST, artist.artistId)
                }
                R.id.menu_artist_shuffle -> {
                    npHelper.startRadio(RadioSeed.TYPE_ARTIST_FOR_SHUFFLE, artist.artistId,
                            RadioFeedReason.ARTIST_SHUFFLE)
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

    fun showPlaylistPopup(view: View, playlist: IPlaylist) {

        val pop = PopupMenu(ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view)
        pop.inflate(R.menu.playlist_popup)

        carbonAnalytics.logEntityEvent("popup", playlist)

        pop.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.menu_start_radio -> {
                    playlist.id?.let {
                        npHelper.startRadio(RadioSeed.TYPE_PLAYLIST, it)
                    } ?: Toast.makeText(this, "Could not start radio",
                            Toast.LENGTH_SHORT).show()
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

    fun showTrackPopup(view: View, track: ITrack) {
        val pop = PopupMenu(ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view)
        pop.inflate(R.menu.remote_song_popup)

        carbonAnalytics.logEntityEvent("popup", track)

        pop.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.menu_share -> {
                    shareItem(track.storeId!!, track.title, MediaType.TRACK)
                }
                R.id.menu_start_radio -> {
                    track.storeId?.let {
                        npHelper.startRadio(RadioSeed.TYPE_SJ_TRACK, it)
                    } ?: Toast.makeText(this, "Could not start radio",
                            Toast.LENGTH_SHORT).show()
                }
                R.id.menu_go_to_album -> {
                    (track as? Track)?.albums?.first()?.let {
                        gotoAlbum(it, PaletteUtil.DEFAULT_SWATCH_PAIR)
                    } ?: gotoAlbum(SkyjamAlbum(track as SkyjamTrack),
                            PaletteUtil.DEFAULT_SWATCH_PAIR)
                }
                R.id.menu_go_to_artist -> {
                    (track as? Track)?.artists?.first()?.let {
                        gotoArtist(it, PaletteUtil.DEFAULT_SWATCH_PAIR)
                    } ?: (track as? SkyjamTrack)?.artistId?.first()?.let {
                        gotoArtist(Artist(it, track.albumArtist, false),
                                PaletteUtil.DEFAULT_SWATCH_PAIR)
                    } ?: (track as? ParcelableTrack)?.artistId?.first()?.let {
                        gotoArtist(Artist(it, track.albumArtist, false),
                                PaletteUtil.DEFAULT_SWATCH_PAIR)
                    }
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

    private fun closeSearch() {
        val pos = IntArray(2).apply {
            main_actionbar_search.getLocationInWindow(this)
        }
        val cx = pos[0] + main_actionbar_search.width / 2
        val cy = pos[1] + main_actionbar_search.height / 2

        // get the final radius for the clipping circle
        val finalRadius = (IdentityUtils.displayWidth2(this)).toFloat()

        // create the animator for this view (the end radius is zero)
        val anim = ViewAnimationUtils
                .createCircularReveal(searchToolbar, cx, cy, finalRadius, 0f)

        anim.duration = 400
        anim.start()

        Handler().postDelayed({
            searchToolbar.visibility = View.GONE
        }, 360)
    }

    override fun onBackPressed() {
        Timber.d("backStackEntry count %d", fragmentManager.backStackEntryCount)

        if(searchToolbar.visibility == View.VISIBLE) {
            closeSearch()
            return
        }

        if(router.backstackSize == 2) {
            main_actionbar_text.text = lastAppbarText
        }

        if(router.backstackSize > 0){
            router.popCurrentController()
            router.addChangeListener(object: ControllerChangeHandler.ControllerChangeListener {

                override fun onChangeStarted(to: Controller?, from: Controller?,
                                             isPush: Boolean, container: ViewGroup,
                                             handler: ControllerChangeHandler) {}

                override fun onChangeCompleted(to: Controller?, from: Controller?,
                                               isPush: Boolean, container: ViewGroup,
                                               handler: ControllerChangeHandler) {
                    to?.let {
                        carbonAnalytics.setCurrentScreen(this@MainActivity,
                                it.javaClass.simpleName, it.javaClass.canonicalName)
                    }

                }
            })

        } else this.finish()

    }
}