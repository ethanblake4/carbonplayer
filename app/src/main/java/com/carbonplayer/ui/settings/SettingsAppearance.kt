package com.carbonplayer.ui.settings

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.model.entity.enums.PaletteMode
import kotlinx.android.synthetic.main.activity_settings_appearance.*

class SettingsAppearance: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_appearance)

        val app = application as CarbonPlayerApplication

        settings_appearance_primaryColor.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle(R.string.settings_appearance_opt_primary_color_mode)
                    .setSingleChoiceItems(R.array.colorModes,
                            app.preferences.primaryPaletteMode.ordinal - 1,
                            { d, i ->
                                app.preferences.primaryPaletteMode = PaletteMode.values()[i + 1]
                                app.preferences.save()
                                updateTexts()
                                d.dismiss()
                            })
                    .show()
        }

        settings_appearance_accentColor.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle(R.string.settings_opt_appearance_accent_color_mode)
                    .setSingleChoiceItems(R.array.colorModes,
                            app.preferences.secondaryPaletteMode.ordinal - 1,
                            { d, i ->
                                app.preferences.secondaryPaletteMode = PaletteMode.values()[i + 1]
                                app.preferences.save()
                                updateTexts()
                                d.dismiss()
                            })
                    .show()
        }

        updateTexts()
    }

    private fun updateTexts() {

        val app = application as CarbonPlayerApplication

        settings_appearance_primaryColor_desc.text =
                resources.getStringArray(R.array.colorModes)[
                        app.preferences.primaryPaletteMode.ordinal - 1
                        ]
        settings_appearance_accentColor_desc.text =
                resources.getStringArray(R.array.colorModes)[
                        app.preferences.secondaryPaletteMode.ordinal - 1
                        ]
    }

}