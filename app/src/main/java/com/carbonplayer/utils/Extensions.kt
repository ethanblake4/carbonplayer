package com.carbonplayer.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Parcelable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.carbonplayer.model.entity.MusicTrack
import com.carbonplayer.model.entity.ParcelableMusicTrack
import org.parceler.Parcels
import timber.log.Timber

fun MutableList<ParcelableMusicTrack>.asParcel(): Parcelable =
        Parcels.wrap<MutableList<ParcelableMusicTrack>>(this)

fun List<MusicTrack>.parcelable(): MutableList<ParcelableMusicTrack> =
        MutableList(size, { i -> get(i).parcelable() })

inline fun <reified T : Context> Context.newIntent(
        action: String,
        noinline f: Intent.() -> Unit = {}
): Intent = Intent(this, T::class.java).apply(f).apply({ this.action = action })

inline fun <reified T : Context> Context.newIntent(
        noinline f: Intent.() -> Unit = {}
): Intent = Intent(this, T::class.java).apply(f)

fun Context.pendingActivityIntent(
        i: Intent,
        requestCode: Int = 0,
        flags: Int = 0
): PendingIntent = PendingIntent.getActivity(this, requestCode, i, flags)

fun Context.pendingServiceIntent(
        i: Intent,
        requestCode: Int = 0,
        flags: Int = 0
): PendingIntent = PendingIntent.getService(this, requestCode, i, flags)

fun Context.loadImageBitmap(
        url: String,
        completed: (Bitmap?) -> Unit
) {
    Glide.with(this)
            .asBitmap()
            .load(url)
            .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    Timber.i("image load fail")
                    completed(null)
                }

                override fun onResourceReady(resource: Bitmap, t: Transition<in Bitmap>) {
                    Timber.i("image load complete")
                    completed(resource)
                }
            })
}
