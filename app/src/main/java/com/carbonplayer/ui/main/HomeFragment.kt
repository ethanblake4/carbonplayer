package com.carbonplayer.ui.main

import android.app.Fragment
import android.os.Bundle
import com.carbonplayer.model.network.Protocol
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber

/**
 * Created by ethanelshyeb on 8/10/17.
 */
class HomeFragment: Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Protocol.listenNow(activity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d("complete")
                }, { err ->
                    Timber.e("Error in listennow", err)
                })
    }
}