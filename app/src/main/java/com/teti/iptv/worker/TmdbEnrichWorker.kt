package com.teti.iptv.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.teti.iptv.repository.IptvRepository
import com.teti.iptv.repository.RatingRepository
import com.teti.iptv.repository.TmdbAuthException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Katalogdaki filmleri ve dizileri TMDB'den puan/özet/afişle doldurur. */
@HiltWorker
class TmdbEnrichWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val iptvRepository: IptvRepository,
    private val ratingRepository: RatingRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val profile = iptvRepository.getActiveProfileOnce() ?: return Result.success()
        var last = 0 to 0
        return try {
            ratingRepository.enrichPending(profile.id) { done, total ->
                last = done to total
                setProgress(workDataOf(KEY_DONE to done, KEY_TOTAL to total))
            }
            Result.success(workDataOf(KEY_DONE to last.first, KEY_TOTAL to last.second))
        } catch (e: TmdbAuthException) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "TMDB anahtarı geçersiz")))
        }
    }

    companion object {
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
    }
}
