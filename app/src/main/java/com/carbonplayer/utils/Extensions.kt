package com.carbonplayer.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.ParcelableTrack
import com.carbonplayer.model.entity.base.IAlbum
import com.carbonplayer.model.entity.base.IArtist
import com.carbonplayer.model.entity.base.IPlaylist
import com.carbonplayer.model.entity.base.ITrack
import com.carbonplayer.model.entity.enums.MediaType
import com.carbonplayer.model.entity.radio.SkyjamStation
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.disposables.Disposable
import io.realm.Realm
import org.json.JSONArray
import org.json.JSONObject
import org.parceler.Parcels
import timber.log.Timber
import android.app.Activity
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK



fun JSONObject.maybeGetInt (key: String?): Int? = maybeGet (key, { getInt(key) })
fun JSONObject.maybeGetString (key: String?): String? = maybeGet (key, { getString(key) })
fun JSONObject.maybeGetBool (key: String?): Boolean? = maybeGet (key, { getBoolean(key) })
fun JSONObject.maybeGetDouble (key: String?): Double? = maybeGet (key, { getDouble(key) })
fun JSONObject.maybeGetLong (key: String?): Long? = maybeGet (key, { getLong(key) })
fun JSONObject.maybeGetObj (key: String?): JSONObject? = maybeGet (key, { getJSONObject(key) })
fun JSONObject.maybeGetArray (key: String?): JSONArray? = maybeGet (key, { getJSONArray(key) })

val Any.carbonAnalytics: FirebaseAnalytics
    get() = CarbonPlayerApplication.instance.analytics


fun FirebaseAnalytics.logEntityEvent(event: String, artist: IArtist) =
        logEvent(event, bundleOf(
                FirebaseAnalytics.Param.ITEM_ID to artist.artistId,
                FirebaseAnalytics.Param.ITEM_NAME to artist.name,
                FirebaseAnalytics.Param.ITEM_CATEGORY to MediaType.ARTIST
        ))

fun FirebaseAnalytics.logEntityEvent(event: String, album: IAlbum) =
        logEvent(event, bundleOf(
                FirebaseAnalytics.Param.ITEM_ID to album.albumId,
                FirebaseAnalytics.Param.ITEM_NAME to album.name,
                FirebaseAnalytics.Param.ITEM_CATEGORY to MediaType.ALBUM
        ))

fun FirebaseAnalytics.logEntityEvent(event: String, track: ITrack) =
        logEvent(event, bundleOf(
                FirebaseAnalytics.Param.ITEM_ID to track.storeId,
                FirebaseAnalytics.Param.ITEM_NAME to track.title,
                FirebaseAnalytics.Param.ITEM_CATEGORY to MediaType.TRACK
        ))

fun FirebaseAnalytics.logEntityEvent(event: String, playlist: IPlaylist) =
        logEvent(event, bundleOf(
                FirebaseAnalytics.Param.ITEM_ID to playlist.id,
                FirebaseAnalytics.Param.ITEM_NAME to playlist.name,
                FirebaseAnalytics.Param.ITEM_CATEGORY to MediaType.PLAYLIST
        ))

fun FirebaseAnalytics.logEntityEvent(event: String, station: SkyjamStation) =
        logEvent(event, bundleOf(
                FirebaseAnalytics.Param.ITEM_ID to station.id,
                FirebaseAnalytics.Param.ITEM_NAME to station.bestName,
                FirebaseAnalytics.Param.ITEM_CATEGORY to MediaType.STATION
        ))



inline fun <T> JSONObject.maybeGet(
        key: String?,
        getter: JSONObject.() -> T
) = if(key == null) null else {
        if (has(key)) getter(this) else null
    }

inline fun <reified T, R> JSONArray.map(transform: (T?) -> R): MutableList<R?> =
        mapTo(mutableListOf(), transform)

inline fun <reified T, R> JSONArray.mapTo(to: MutableList<R?>, transform: (T?) -> R): MutableList<R?> =
        (0..length()).mapTo(to, { i -> transform(get(i) as? T) })

inline fun <reified T, R> JSONArray.mapNotNull(transform: (T) -> R): MutableList<R> =
        mapNotNullTo(mutableListOf(), transform)

inline fun <reified T, R> JSONArray.mapNotNullTo(to: MutableList<R>, transform: (T) -> R): MutableList<R> =
        to.apply {
            (0..length()).forEach { i -> (get(i) as? T)?.let { add(transform(it)) } }
        }

inline fun <reified T, R> JSONArray.mapIndexed(transform: (index: Int, T?) -> R): MutableList<R?> =
        mapIndexedTo(mutableListOf(), transform)

inline fun <reified T, R> JSONArray.mapIndexedTo(
        to: MutableList<R?>,
        transform: (index: Int, T?) -> R?
): MutableList<R?> =
        (0..length()).mapTo(to, { i ->  transform(i, get(i) as? T) }  )

inline fun <reified T, R> JSONArray.mapIndexedNotNull(transform: (index: Int, T) -> R): MutableList<R> =
        mapIndexedNotNullTo(mutableListOf(), transform)

inline fun <reified T, R> JSONArray.mapIndexedNotNullTo(
        to: MutableList<R>,
        transform: (index: Int, T) -> R
): MutableList<R> =
        to.apply {
            (0..length()).forEach { i -> (get(i) as? T)?.let { add(transform(i, it)) } }
        }

inline fun <reified T> JSONArray.toList() = List(length(), { i -> get(i) as? T })

inline fun <reified T> JSONArray.toMutableList() = MutableList(length(), { i -> get(i) as? T })

inline fun <reified T> JSONArray.all(predicate: (T?) -> Boolean) =
        map<T, Boolean> { q -> predicate(q) }.all { it == true }


inline fun <reified T> JSONArray.any(predicate: (T?) -> Boolean) =
        map<T, Boolean> { q -> predicate(q) }.any { it == true }

inline fun <reified T, R> JSONArray.fold(initial: R, operation: (acc: R, T?) -> R) =
        toList<T>().fold(initial, operation)

inline fun <reified T, R> JSONArray.foldIndexed(initial: R, operation: (index:Int, acc: R, T?) -> R) =
        toList<T>().foldIndexed(initial, operation)

inline fun <reified T> JSONArray.forEach(action: (T?) -> Unit) {
    (0..length()).forEach { action(get(it) as? T) }
}

inline fun <reified T> JSONArray.forEachIndexed(action: (index: Int, T?) -> Unit) {
    (0..length()).forEach { action(it, get(it) as? T) }
}

inline fun <reified T> JSONArray.forEachNotNull(action: (T) -> Unit) {
    (0..length()).forEach { action(get(it) as T) }
}

inline fun <reified T: Comparable<T>> JSONArray.max() =
        toList<T>().filterNotNull().max()

inline fun <reified T: Comparable<T>> JSONArray.min() =
        toList<T>().filterNotNull().min()

operator fun JSONArray.plus(other: Any?) : JSONArray {
    return JSONArray(toList<Any?>()).apply { plusAssign(other) }
}

operator fun JSONArray.plusAssign(other: Any?) {
    put(other)
}

operator fun JSONArray.set(index: Int, other: Any?) {
    put(index, other)
}

operator fun JSONArray.iterator() = object: Iterator<Any?> {

    var position = 0



    override fun hasNext(): Boolean = (position < this@iterator.length() -1)

    override fun next(): Any = get(position++)
}

operator fun JSONArray.contains(other: Any?) = any { it: Any? -> it == other }


fun <E> Collection<E>.nullIfEmpty(): Collection<E>? {

    val array = JSONArray()
    val obj = JSONObject()

    // -------

    (0..array.length()).mapNotNull { i ->
        array.get(i) as? Int
    }.max()

    array.max<Int>()

    array += "hi"

    array[9] = "hi"

    // -------

    (0..array.length()).map { i ->
        array.getJSONObject(i).let {
            if (it.has("people"))
                return@map it.getString("people")
            else
                return@map null
        }
    }

    array.map { it: JSONObject? -> it?.maybeGetString("people") }

    // -------

    obj.let { if(it.has("people")) it.getString("people") else null }

    obj.maybeGetString("people")

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
