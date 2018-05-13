package com.carbonplayer.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.carbonplayer.utils.carbonAnalytics

class BecomingNoisyReceiver (
        val callback: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
            carbonAnalytics.logEvent("becoming_noisy", null)
            callback()
        }
    }
}