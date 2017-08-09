package com.carbonplayer.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.carbonplayer.R
import com.carbonplayer.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * Settings main page
 */
class Settings: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings_item_general.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }


    }


}