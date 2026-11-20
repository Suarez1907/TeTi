package com.teti.iptv.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teti.iptv.repository.IptvRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PlaylistSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val iptvRepository: IptvRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROFILE_ID = "profile_id"
    }

    override suspend fun doWork(): Result {
        val targetProfileId = inputData.getLong(KEY_PROFILE_ID, -1L)
        val profileId = if (targetProfileId != -1L) {
            targetProfileId
        } else {
            iptvRepository.getActiveProfileOnce()?.id ?: return Result.success()
        }

        return try {
            iptvRepository.syncProfile(profileId)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
