package com.carbonplayer.utils.jobs

import com.carbonplayer.utils.jobs.CacheEvictionJob
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class CarbonJobCreator : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            CacheEvictionJob.jobTag -> CacheEvictionJob()
            else -> null
        }
    }

}