package com.carbonplayer.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.URISyntaxException
import java.net.URL

import kotlin.jvm.javaClass;


class LinkActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_VIEW) {
            val url = "https://songlink.io/" + intent.data.scheme + "://" + intent.data.host + intent.data.path + "?" + intent.data.encodedQuery;
            songlinkGoogleID(url, {id ->
                val i = Intent()
                i.setClass(this@LinkActivity, AlbumActivity::class.java)
                i.putExtra("id", id)
                startActivity(i)
            })
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("https://songlink.io/" + intent.data.scheme + "://" + intent.data.host + intent.data.path + "?" + intent.data.encodedQuery)
            startActivity(i)
            finish()
        }

    }

    @Throws(IOException::class, URISyntaxException::class)
    fun songlinkGoogleID(url: String, completion: (String) -> Unit) {
        Thread(Runnable {
            val conn = URL("https://songlink.io/" + url).openConnection()
            conn.connect()
            val inputS = conn.getInputStream()
            val page = inputS.convertToString()
            val id = page.substring(page.indexOf("play.google.com/music/m/"), page.indexOf("?signup_if_needed=1"));
            inputS.close()
            completion(id)
        }).start()
    }

    fun InputStream.convertToString(): String {
        val s = java.util.Scanner(this).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

}