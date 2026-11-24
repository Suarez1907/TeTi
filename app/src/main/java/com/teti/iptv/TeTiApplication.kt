package com.teti.iptv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.media3.cast.Cast
import androidx.work.Configuration
import com.teti.iptv.worker.BackgroundScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TeTiApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var scheduler: BackgroundScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Chromecast: Google Play Hizmetleri yoksa (Fire TV vb.) sessizce devre dışı kalır.
        runCatching { Cast.getSingletonInstance(this).initialize() }
        scheduler.onAppLaunch()
    }
}
