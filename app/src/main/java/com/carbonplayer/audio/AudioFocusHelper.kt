package com.carbonplayer.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.os.Build

/**
 * Convenience class to deal with audio focus. This class deals with everything related to audio
 * focus: it can request and abandon focus, and will intercept focus change events and deliver
 * them to a MusicFocusable interface (which, in our case, is implemented by [MusicPlayerService]).

 * This class can only be used on SDK level 8 and above, since it uses API features that are not
 * available on previous SDK's.
 */
class AudioFocusHelper(ctx: Context, internal var focusable: MusicFocusable) : AudioManager.OnAudioFocusChangeListener {

    internal var mAM: AudioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** Requests audio focus. Returns whether request was successful or not.  */
    fun requestFocus(): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .build()
            mAM.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            mAM.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AUDIOFOCUS_GAIN)
        }
    }

    /** Abandons audio focus. Returns whether request was successful or not.  */
    fun abandonFocus(): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAM.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            mAM.abandonAudioFocus (this)
        }
    }

    /**
     * Called by AudioManager on audio focus changes. We implement this by calling our
     * MusicFocusable appropriately to relay the message.
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AUDIOFOCUS_GAIN -> focusable.onGainedAudioFocus()
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> focusable.onLostAudioFocus(false)
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> focusable.onLostAudioFocus(true)
        }
    }

    companion object {
        lateinit var focusRequest: AudioFocusRequest
    }
}