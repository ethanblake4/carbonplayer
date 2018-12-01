package com.carbonplayer.ui.intro.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.TrackCache
import com.jakewharton.processphoenix.ProcessPhoenix
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.intro_fragment_3.view.*
import timber.log.Timber

/**
 * Signing in / fetching library screen
 */
class IntroPageThreeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.intro_fragment_3, container, false) as ViewGroup

        rootView.nautilusFailedOKButton.setOnClickListener { v ->
            if (!v.isEnabled) return@setOnClickListener

            val continueAfterComplete = {
                val prefs = CarbonPlayerApplication.instance.preferences

                prefs.firstStart = true
                prefs.masterToken = null
                prefs.BearerAuth = null
                prefs.OAuthToken = null
                prefs.PlayMusicOAuth = null
                prefs.userEmail = null

                prefs.saveSync()

                ProcessPhoenix.triggerRebirth(activity!!)
            }

            Realm.getDefaultInstance().apply {
                executeTransactionAsync ({ rlm ->
                    rlm.deleteAll()
                }, { // onComplete
                    Completable.fromCallable {
                        TrackCache.deleteCache(activity!!)
                    }.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe ({
                                continueAfterComplete()
                            }) {
                                continueAfterComplete()
                            }
                }, { e-> // onError
                    Timber.e(e)
                    Toast.makeText(activity,
                            "Error signing out", Toast.LENGTH_LONG)
                            .show()
                })
            }
        }

        return rootView
    }
}