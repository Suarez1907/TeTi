package com.teti.iptv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teti.iptv.data.local.dao.ChannelDao
import com.teti.iptv.data.local.dao.EpgDao
import com.teti.iptv.data.local.dao.PlaylistDao
import com.teti.iptv.data.local.dao.TmdbMetadataDao
import com.teti.iptv.data.local.dao.SeriesDao
import com.teti.iptv.data.local.dao.VodDao
import com.teti.iptv.data.local.dao.WatchHistoryDao
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.PlaylistType
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.TmdbMetadataEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.data.local.entity.WatchHistoryEntity

class Converters {
    @TypeConverter
    fun fromPlaylistType(value: PlaylistType): String = value.name

    @TypeConverter
    fun toPlaylistType(value: String): PlaylistType =
        runCatching { PlaylistType.valueOf(value) }.getOrDefault(PlaylistType.XTREAM)
}

@Database(
    entities = [
        PlaylistProfileEntity::class,
        ChannelEntity::class,
        VodEntity::class,
        SeriesEntity::class,
        WatchHistoryEntity::class,
        EpgProgramEntity::class,
        TmdbMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TeTiDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun vodDao(): VodDao
    abstract fun seriesDao(): SeriesDao
    abstract fun epgDao(): EpgDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun tmdbMetadataDao(): TmdbMetadataDao

    companion object {
        /** Profilleri ve favorileri silmeden TMDB önbellek tablosunu ekler. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tmdb_metadata` (" +
                        "`contentKey` TEXT NOT NULL, `tmdbId` INTEGER, `voteAverage` REAL, " +
                        "`overview` TEXT, `backdropUrl` TEXT, `posterUrl` TEXT, `releaseDate` TEXT, " +
                        "`runtimeMinutes` INTEGER, `genres` TEXT, `cast` TEXT, `director` TEXT, " +
                        "`detailsLoaded` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`contentKey`))"
                )
                // Sağlayıcıdan gelen puanlar güvenilmez; TMDB doldurana kadar boş bırak.
                db.execSQL("UPDATE vod_movies SET imdbRating = NULL")
                db.execSQL("UPDATE vod_series SET imdbRating = NULL")
            }
        }
    }
}
