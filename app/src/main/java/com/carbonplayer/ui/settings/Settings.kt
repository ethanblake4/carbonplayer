package com.carbonplayer.ui.settings

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.carbonplayer.R
import com.carbonplayer.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * Settings main page
 */
class Settings: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings_item_general.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }


    }


}