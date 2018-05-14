package com.carbonplayer.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.TrackCache
import com.carbonplayer.model.entity.enums.StreamQuality
import kotlinx.android.synthetic.main.activity_settings_general.*

class SettingsGeneral: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_general)

        val app = application as CarbonPlayerApplication

        settings_general_wifi_quality.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle(R.string.settings_general_opt_quality_wifi)
                    .setSingleChoiceItems(
                            R.array.stream_qualities,
                            app.preferences.preferredStreamQualityWifi.ordinal - 1,
                            { d, i ->
                                app.preferences.preferredStreamQualityWifi =
                                        StreamQuality.values()[i + 1]
                                app.preferences.save()

                                updateTexts()
                                d.dismiss()
                            }
                    ).show()
        }

        settings_general_mobile_quality.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle(R.string.settings_general_opt_quality_mobile)
                    .setSingleChoiceItems(
                            R.array.stream_qualities,
                            app.preferences.preferredStreamQualityMobile.ordinal - 1,
                            { d, i ->
                                app.preferences.preferredStreamQualityMobile =
                                        StreamQuality.values()[i + 1]
                                app.preferences.save()

                                updateTexts()
                                d.dismiss()
                            }
                    )
                    .show()
        }

        settings_general_evict_cache.setOnClickListener {

            Toast.makeText(this, "Trimming cache", Toast.LENGTH_LONG).show()

            TrackCache.evictCache(this,
                    CarbonPlayerApplication.instance.preferences.maxAudioCacheSizeMB.toLong())
        }

        updateTexts()
    }

    private fun updateTexts() {

        val app = application as CarbonPlayerApplication

        settings_general_wifi_quality_desc.text =
                resources.getStringArray(R.array.stream_qualities)[
                        app.preferences.preferredStreamQualityWifi.ordinal - 1
                        ]

        settings_general_mobile_quality_desc.text =
                resources.getStringArray(R.array.stream_qualities)[
                        app.preferences.preferredStreamQualityMobile.ordinal - 1
                        ]
    }
}