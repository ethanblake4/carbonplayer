package com.carbonplayer.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.carbonplayer.R
import com.carbonplayer.utils.newIntent
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * Settings main page
 */
class Settings: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings_item_general.setOnClickListener {
            startActivity(newIntent<SettingsGeneral>())
        }
        settings_item_appearance.setOnClickListener {
            startActivity(newIntent<SettingsAppearance>())
        }
        settings_item_account.setOnClickListener {
            startActivity(newIntent<SettingsAccount>())
        }

    }


}