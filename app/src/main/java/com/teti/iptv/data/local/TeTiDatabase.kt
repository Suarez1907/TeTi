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
import com.teti.iptv.data.local.dao.TmdbMatchDao
import com.teti.iptv.data.local.dao.TmdbMetadataDao
import com.teti.iptv.data.local.dao.SeriesDao
import com.teti.iptv.data.local.dao.VodDao
import com.teti.iptv.data.local.dao.WatchHistoryDao
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.ChannelFts
import com.teti.iptv.data.local.entity.SeriesFts
import com.teti.iptv.data.local.entity.VodFts
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
        TmdbMetadataEntity::class,
        ChannelFts::class,
        VodFts::class,
        SeriesFts::class
    ],
    version = 3,
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
    abstract fun tmdbMatchDao(): TmdbMatchDao

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

        /**
         * Catch-up alanları, izleme geçmişinde akış adresi ve tam metin arama dizinleri.
         * FTS SQL'i Room'un ürettiği şemayla birebir aynıdır (değiştirirseniz uygulama açılmaz).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN tvArchive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE channels ADD COLUMN archiveDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE watch_history ADD COLUMN streamUrl TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_watch_history_profileId_lastWatchedAt` " +
                        "ON `watch_history` (`profileId`, `lastWatchedAt`)"
                )
                FTS_SCHEMA.forEach(db::execSQL)
                // Mevcut kataloğu dizine al.
                db.execSQL("INSERT INTO channels_fts(channels_fts) VALUES('rebuild')")
                db.execSQL("INSERT INTO vod_fts(vod_fts) VALUES('rebuild')")
                db.execSQL("INSERT INTO series_fts(series_fts) VALUES('rebuild')")
            }
        }

        private val FTS_SCHEMA = listOf(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `channels_fts` USING FTS4(`name` TEXT NOT NULL, `categoryName` TEXT NOT NULL, content=`channels`)",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_channels_fts_BEFORE_UPDATE BEFORE UPDATE ON `channels` BEGIN DELETE FROM `channels_fts` WHERE `docid`=OLD.`rowid`; END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_channels_fts_BEFORE_DELETE BEFORE DELETE ON `channels` BEGIN DELETE FROM `channels_fts` WHERE `docid`=OLD.`rowid`; END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_channels_fts_AFTER_UPDATE AFTER UPDATE ON `channels` BEGIN INSERT INTO `channels_fts`(`docid`, `name`, `categoryName`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`categoryName`); END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_channels_fts_AFTER_INSERT AFTER INSERT ON `channels` BEGIN INSERT INTO `channels_fts`(`docid`, `name`, `categoryName`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`categoryName`); END",
                "CREATE VIRTUAL TABLE IF NOT EXISTS `vod_fts` USING FTS4(`title` TEXT NOT NULL, `categoryName` TEXT NOT NULL, content=`vod_movies`)",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_vod_fts_BEFORE_UPDATE BEFORE UPDATE ON `vod_movies` BEGIN DELETE FROM `vod_fts` WHERE `docid`=OLD.`rowid`; END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_vod_fts_BEFORE_DELETE BEFORE DELETE ON `vod_movies` BEGIN DELETE FROM `vod_fts` WHERE `docid`=OLD.`rowid`; END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_vod_fts_AFTER_UPDATE AFTER UPDATE ON `vod_movies` BEGIN INSERT INTO `vod_fts`(`docid`, `title`, `categoryName`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`categoryName`); END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_vod_fts_AFTER_INSERT AFTER INSERT ON `vod_movies` BEGIN INSERT INTO `vod_fts`(`docid`, `title`, `categoryName`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`categoryName`); END",
                "CREATE VIRTUAL TABLE IF NOT EXISTS `series_fts` USING FTS4(`title` TEXT NOT NULL, `categoryName` TEXT NOT NULL, content=`vod_series`)",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_series_fts_BEFORE_UPDATE BEFORE UPDATE ON `vod_series` BEGIN DELETE FROM `series_fts` WHERE `docid`=OLD.`rowid`; END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_series_fts_BEFORE_DELETE BEFORE DELETE ON `vod_series` BEGIN DELETE FROM `series_fts` WHERE `docid`=OLD.`rowid`; END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_series_fts_AFTER_UPDATE AFTER UPDATE ON `vod_series` BEGIN INSERT INTO `series_fts`(`docid`, `title`, `categoryName`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`categoryName`); END",
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_series_fts_AFTER_INSERT AFTER INSERT ON `vod_series` BEGIN INSERT INTO `series_fts`(`docid`, `title`, `categoryName`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`categoryName`); END"
        )
    }
}
