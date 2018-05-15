package com.carbonplayer.ui.settings

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.TrackCache
import com.jakewharton.processphoenix.ProcessPhoenix
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_settings_account.*
import timber.log.Timber

class SettingsAccount: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_account)

        settings_account_email.text =
                CarbonPlayerApplication.instance.preferences.userEmail

        settings_account_sign_out.setOnClickListener {
            val d = AlertDialog.Builder(this)
                    .setTitle(R.string.sign_out)
                    .setMessage(R.string.sign_out_confirmation)
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        dialog.dismiss()

                        @Suppress("DEPRECATION")
                        val p = ProgressDialog(this)
                        p.setMessage("Signing out...")
                        p.isIndeterminate = true
                        p.show()

                        val continueAfterComplete = {
                            val prefs = CarbonPlayerApplication.instance.preferences

                            prefs.firstStart = true
                            prefs.masterToken = null
                            prefs.BearerAuth = null
                            prefs.OAuthToken = null
                            prefs.PlayMusicOAuth = null
                            prefs.userEmail = null

                            prefs.saveSync()

                            ProcessPhoenix.triggerRebirth(this)
                        }

                        Realm.getDefaultInstance().apply {
                            executeTransactionAsync ({ rlm ->
                                rlm.deleteAll()
                            }, { // onComplete
                                Completable.fromCallable {
                                    TrackCache.deleteCache(this@SettingsAccount)
                                }.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe ({
                                    continueAfterComplete()
                                }) {
                                    continueAfterComplete()
                                }
                            }, { e-> // onError
                                Timber.e(e)
                                p.dismiss()
                                Toast.makeText(this@SettingsAccount,
                                        "Error signing out", Toast.LENGTH_LONG)
                                        .show()
                            })
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }

            d.create().show()

        }
    }

}