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
import org.json.JSONArray
import org.json.JSONObject
import org.parceler.Parcels
import timber.log.Timber

fun JSONObject.maybeGetInt (key: String?): Int? = maybeGet (key, { getInt(key) })
fun JSONObject.maybeGetString (key: String?): String? = maybeGet (key, { getString(key) })
fun JSONObject.maybeGetBool (key: String?): Boolean? = maybeGet (key, { getBoolean(key) })
fun JSONObject.maybeGetDouble (key: String?): Double? = maybeGet (key, { getDouble(key) })
fun JSONObject.maybeGetLong (key: String?): Long? = maybeGet (key, { getLong(key) })
fun JSONObject.maybeGetObj (key: String?): JSONObject? = maybeGet (key, { getJSONObject(key) })
fun JSONObject.maybeGetArray (key: String?): JSONArray? = maybeGet (key, { getJSONArray(key) })


inline fun <T> JSONArray.mapArray(
        sGet: JSONArray.(i: Int) -> T
): MutableList<T> = (0..length()).mapTo(mutableListOf(), { i-> sGet(i)})

inline fun <T, R : MutableList<T>> JSONArray.mapArrayTo(
        to: R, sGet: JSONArray.(i: Int) -> T
): R = (0..length()).mapTo(to, { i-> sGet(i)})

inline fun <T> JSONObject.maybeGet(
        key: String?,
        sGet: JSONObject.() -> T) =
    if(key == null) null else {
        if (has(key)) sGet(this) else null
    }

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

fun List<ITrack>.parcelable(): MutableList<ParcelableTrack> =
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
