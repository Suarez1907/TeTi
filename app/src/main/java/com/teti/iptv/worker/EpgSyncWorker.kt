package com.teti.iptv.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teti.iptv.repository.IptvRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: IptvRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        repository.syncEpgForActiveProfile()
        Result.success()
    } catch (e: Exception) {
        Log.w("EpgSyncWorker", "EPG güncellenemedi", e)
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
