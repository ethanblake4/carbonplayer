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
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.base.ITrack
import io.reactivex.disposables.Disposable
import io.realm.Realm
import org.json.JSONArray
import org.json.JSONObject
import org.parceler.Parcels
import timber.log.Timber

fun <E> Collection<E>.nullIfEmpty(): Collection<E>? {
    return if (this.isEmpty()) null else this
}

fun Disposable.addToAutoDispose() = apply {
    CarbonPlayerApplication.compositeDisposable.add(this)
}

fun JSONObject.toByteArray() : ByteArray {
    return toString().toByteArray(Charsets.UTF_8)
}

fun MutableList<ParcelableTrack>.asParcel(): Parcelable =
        Parcels.wrap<MutableList<ParcelableTrack>>(this)

fun List<ITrack>.parcelable(realm: Realm? = null): MutableList<ParcelableTrack> =
        MutableList(size, { i -> get(i).parcelable(realm) })

inline fun <reified T : Context> Context.newIntent(
        action: String,
        f: Intent.() -> Unit = {}
): Intent = Intent(this, T::class.java).apply(f).apply({ this.action = action })

inline fun <reified T : Context> Context.newIntent(
        f: Intent.() -> Unit = {}
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
