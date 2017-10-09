package com.carbonplayer.utils.jobs

import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.TrackCache
import com.evernote.android.job.Job

/**
 * A job that periodically frees up cache space
 */
class CacheEvictionJob : Job() {

    override fun onRunJob(params: Params?): Result {
        val context = CarbonPlayerApplication.instance
        TrackCache.removeLowerQualities(context)
        TrackCache.evictCache(context, CarbonPlayerApplication.instance.preferences.maxAudioCacheSizeMB.toLong())

        return Result.SUCCESS
    }

    companion object {
        val jobTag = "cache_eviction_job"
    }

}