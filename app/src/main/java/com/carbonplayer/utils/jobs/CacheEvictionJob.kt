package com.carbonplayer.utils.jobs

import android.app.ActivityManager
import android.content.Context
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.audio.MusicPlayerService
import com.carbonplayer.model.entity.TrackCache
import com.evernote.android.job.DailyJob
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * A job that periodically frees up cache space
 */
class CacheEvictionJob : DailyJob() {

    override fun onRunDailyJob(params: Params?): DailyJobResult {

        Timber.d("Running cache eviction job to free up space")

        if(isServiceRunning(context)) return DailyJobResult.SUCCESS

        TrackCache.evictCache(context,
                CarbonPlayerApplication.instance.preferences.maxAudioCacheSizeMB.toLong())

        return DailyJobResult.SUCCESS
    }


    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context) : Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            MusicPlayerService::class.java.name == it.service.className
        }

    }

    companion object {
        val jobTag = "cache_eviction_job"

        @JvmStatic fun schedule() {
            if (!JobManager.instance().getAllJobRequestsForTag(jobTag).isEmpty()) {
                // job already scheduled, nothing to do
                return
            }

            val builder = JobRequest.Builder(jobTag)

            // run job
            DailyJob.schedule(builder,
                    TimeUnit.HOURS.toMillis(1),
                    TimeUnit.HOURS.toMillis(23))
        }
    }

}