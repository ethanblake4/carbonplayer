package com.carbonplayer.model.entity

import timber.log.Timber
import java.util.*

data class SuggestResponse (
        val suggested_queries: List<SuggestResponseEntry> = ArrayList()
) {

     data class SuggestResponseEntry (
             val suggestionCategory: Int = 0,
             val entity: SearchResponse.SearchResult? = null,
             val suggestion_string:String? = null,
             val type: Int = 0

     ) {
            /*get() = if (this.category != 2) {
            null
            } else this.entity!!.subtitle*/
         val artUrl: String? get() {
                if (suggestionCategory != 2) return null
                if(entity == null) return null
                return when (entity.type) {
                    1 -> entity.track?.albumArtURL
                    2 -> entity.artist?.artistArtRef
                    3 -> entity.album?.albumArtRef
                    /*6 -> getFirstImageUrl(this.entity.mArtComposites)*/
                    /*7 -> this.entity!!.situation.mImageUrl*/
                    /*9 -> getFirstImageUrl(this.entity!!.podcastSeries.mArt)*/
                    else -> {
                        Timber.e("Unsupported entity type : ${entity.type}")
                        null
                    }
                } }

         val metajamId: String? get() {
             if (suggestionCategory != 2) return null
             if(entity == null) return null
             return when (entity.type) {
                 1 -> entity.track?.storeId
                 2 -> entity.artist?.artistId
                 3 -> entity.album?.albumId
                 //6 -> entity.station.id
                 //7 -> entity!!.situation.mId
                 //9 -> entity!!.podcastSeries.mSeriesId
                 else -> {
                     Timber.e("Unsupported entity type : ${entity.type}")
                     null
                 }
             }
         }

         val searchType: Int get() = if (suggestionCategory != 2) 0 else entity?.type ?: 0
     }

/*
 fun getSuggestion():String? {
if (this.category != 2)
{
return this.suggestion
}
when (this.entity!!.type) {
1 -> return this.entity!!.track.mTitle
2 -> return this.entity!!.artist.mName
3 -> return this.entity!!.album.mName
6 -> return this.entity!!.station.mName
7 -> return this.entity!!.situation.mTitle
9 -> return this.entity!!.podcastSeries.mTitle
else -> {
Log.m906e("QuerySuggestionResponse", "Unsupported entity type : " + this.entity!!.type)
return null
}
}
}

private fun getFirstImageUrl(images:List<ImageRefJson>?):String? {
    return if (images == null || images!!.isEmpty()) {
        null
    } else (images!!.get(0) as ImageRefJson).mUrl
}
}*/
}