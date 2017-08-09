package com.carbonplayer.ui.main

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bumptech.glide.RequestManager
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.R
import com.carbonplayer.ui.intro.IntroActivity
import kotlinx.android.synthetic.main.activity_container.*

import timber.log.Timber
import com.bluelinelabs.conductor.RouterTransaction



class MainActivity : AppCompatActivity() {

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (CarbonPlayerApplication.instance.preferences.firstStart) {
            val i = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(i)
            Timber.d("Started activity")
            return
        }

        setContentView(R.layout.activity_container)

        router = Conductor.attachRouter(this, layout_frame, savedInstanceState)

        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(MainController()))
        }
    }

    override fun onBackPressed() {
        if(!router.handleBack()) super.onBackPressed()
    }
}
