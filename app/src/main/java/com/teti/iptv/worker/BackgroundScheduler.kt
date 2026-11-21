package com.teti.iptv.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.domain.epg.EpgUpdateInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** WorkManager işlerini tek yerden planlar: EPG güncellemesi ve TMDB zenginleştirme. */
@Singleton
class BackgroundScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences
) {
    private val workManager get() = WorkManager.getInstance(context)

    private val network = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Uygulama açılışında çağrılır. */
    fun onAppLaunch() {
        applyEpgSchedule(preferences.epgInterval.value)
        if (preferences.epgInterval.value == EpgUpdateInterval.ON_LAUNCH) {
            workManager.enqueueUniqueWork(
                EPG_LAUNCH,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<EpgSyncWorker>().setConstraints(network).build()
            )
        }
        enqueueTmdbEnrich(restart = false)
    }

    /** 12/24 saatte bir periyodik güncelleme; "Her Açılışta" seçiliyse periyodik işi kaldırır. */
    fun applyEpgSchedule(interval: EpgUpdateInterval) {
        val hours = interval.hours
        if (hours == null) {
            workManager.cancelUniqueWork(EPG_PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<EpgSyncWorker>(hours, TimeUnit.HOURS)
            .setConstraints(network)
            .setInitialDelay(hours, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(EPG_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /**
     * Henüz TMDB'de aranmamış film/dizileri arka planda doldurur.
     * @param restart önbellek temizlendiğinde çalışan işi baştan başlatır.
     */
    fun enqueueTmdbEnrich(restart: Boolean) {
        workManager.enqueueUniqueWork(
            TMDB_ENRICH,
            if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<TmdbEnrichWorker>().setConstraints(network).build()
        )
    }

    fun tmdbProgress(): Flow<TmdbProgress?> =
        workManager.getWorkInfosForUniqueWorkFlow(TMDB_ENRICH).map { infos ->
            val info = infos.firstOrNull() ?: return@map null
            val data = if (info.state.isFinished) info.outputData else info.progress
            TmdbProgress(
                state = info.state,
                done = data.getInt(TmdbEnrichWorker.KEY_DONE, 0),
                total = data.getInt(TmdbEnrichWorker.KEY_TOTAL, 0),
                error = data.getString(TmdbEnrichWorker.KEY_ERROR)
            )
        }

    companion object {
        const val EPG_PERIODIC = "epg-periodic"
        const val EPG_LAUNCH = "epg-launch"
        const val TMDB_ENRICH = "tmdb-enrich"
    }
}

data class TmdbProgress(val state: WorkInfo.State, val done: Int, val total: Int, val error: String?)
