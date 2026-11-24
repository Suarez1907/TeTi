package com.teti.iptv.di

import android.content.Context
import androidx.room.Room
import com.teti.iptv.data.local.TeTiDatabase
import com.teti.iptv.data.local.dao.ChannelDao
import com.teti.iptv.data.local.dao.EpgDao
import com.teti.iptv.data.local.dao.PlaylistDao
import com.teti.iptv.data.local.dao.TmdbMatchDao
import com.teti.iptv.data.local.dao.TmdbMetadataDao
import com.teti.iptv.data.local.dao.SeriesDao
import com.teti.iptv.data.local.dao.VodDao
import com.teti.iptv.data.local.dao.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TeTiDatabase {
        return Room.databaseBuilder(
            context,
            TeTiDatabase::class.java,
            "teti_iptv.db"
        )
            .addMigrations(TeTiDatabase.MIGRATION_1_2, TeTiDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlaylistDao(db: TeTiDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideChannelDao(db: TeTiDatabase): ChannelDao = db.channelDao()

    @Provides
    fun provideVodDao(db: TeTiDatabase): VodDao = db.vodDao()

    @Provides
    fun provideSeriesDao(db: TeTiDatabase): SeriesDao = db.seriesDao()

    @Provides
    fun provideEpgDao(db: TeTiDatabase): EpgDao = db.epgDao()

    @Provides
    fun provideWatchHistoryDao(db: TeTiDatabase): WatchHistoryDao = db.watchHistoryDao()

    @Provides
    fun provideTmdbMetadataDao(db: TeTiDatabase): TmdbMetadataDao = db.tmdbMetadataDao()

    @Provides
    fun provideTmdbMatchDao(db: TeTiDatabase): TmdbMatchDao = db.tmdbMatchDao()
}
