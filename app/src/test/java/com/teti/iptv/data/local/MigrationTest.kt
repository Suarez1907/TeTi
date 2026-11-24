package com.teti.iptv.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.teti.iptv.data.local.entity.TmdbMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sürüm 1.0.x kullanıcısının veritabanını (şema v1) oluşturur, uygulamanın gerçek
 * migration zincirini çalıştırır ve Room'un şema doğrulamasından geçtiğini, verinin
 * korunduğunu ve yeni FTS aramasının çalıştığını doğrular.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before
    fun createV1Database() {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            V1_SCHEMA.forEach(db::execSQL)
            db.execSQL(
                "INSERT INTO playlist_profiles (id, name, type, serverUrl, username, password, epgUrl, customUserAgent, isActive, lastSyncedAt) " +
                    "VALUES (1, 'Ev', 'XTREAM', 'http://h:80', 'u', 'p', NULL, 'TeTi-IPTV/1.0', 1, 0)"
            )
            db.execSQL(
                "INSERT INTO channels VALUES ('1_10', 1, 'TR: Spor Bir HD', 'Spor', 'http://h/live/u/p/10.ts', NULL, 'spor1.tr', 1, 0, 0)"
            )
            db.execSQL(
                "INSERT INTO vod_movies VALUES ('1_55', 1, 'Kış Uykusu', 'Dram', 'http://h/movie/u/p/55.mkv', NULL, NULL, 9.9, 2014, 'mkv', 0, 1)"
            )
            db.execSQL("INSERT INTO watch_history VALUES ('1_55', 1, 'VOD', 'Kış Uykusu', NULL, 60000, 600000, 5)")
            db.version = 1
        }
    }

    @After
    fun cleanup() {
        context.deleteDatabase(name)
    }

    @Test
    fun migratesFromV1KeepingProfilesFavoritesAndHistory() = runBlocking {
        val db = Room.databaseBuilder(context, TeTiDatabase::class.java, name)
            .addMigrations(TeTiDatabase.MIGRATION_1_2, TeTiDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            // Room şemayı açılışta doğrular; uyumsuzlukta burada istisna fırlar.
            val profile = db.playlistDao().getActiveProfileOnce()
            assertEquals("Ev", profile?.name)

            assertEquals(listOf("1_10"), db.channelDao().getFavoriteIds(1))
            val channel = db.channelDao().getById("1_10")!!
            assertEquals(false, channel.tvArchive)

            // Sağlayıcı puanı (9.9) temizlendi, TMDB doldurana kadar boş.
            val movie = db.vodDao().getById("1_55")!!
            assertNull(movie.imdbRating)
            assertTrue(movie.isFavorite)

            assertEquals(60000L, db.watchHistoryDao().getPosition("1_55"))

            // Mevcut katalog FTS dizinine alındı; yeni eklenenler tetikleyicilerle girer.
            assertEquals(listOf("1_10"), db.channelDao().searchChannelsFts(1, "spor*").first().map { it.streamId })
            assertEquals(listOf("1_55"), db.vodDao().searchVodFts(1, "kış*").first().map { it.streamId })
            db.vodDao().insertAll(listOf(movie.copy(streamId = "1_56", title = "Uzak")))
            assertEquals(listOf("1_56"), db.vodDao().searchVodFts(1, "uzak*").first().map { it.streamId })

            db.tmdbMetadataDao().upsert(TmdbMetadataEntity(contentKey = "movie:1_55", tmdbId = 7L, voteAverage = 8.1f))
            db.tmdbMetadataDao().applyMovieRatings(1)
            assertEquals(8.1f, db.vodDao().getById("1_55")!!.imdbRating!!, 0.001f)
        } finally {
            db.close()
        }
    }

    private companion object {
        val V1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `playlist_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `serverUrl` TEXT NOT NULL, `username` TEXT, `password` TEXT, `epgUrl` TEXT, `customUserAgent` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `lastSyncedAt` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `channels` (`streamId` TEXT NOT NULL, `profileId` INTEGER NOT NULL, `name` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `logoUrl` TEXT, `epgChannelId` TEXT, `isFavorite` INTEGER NOT NULL, `isHidden` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`streamId`))",
            "CREATE INDEX IF NOT EXISTS `index_channels_profileId_categoryName` ON `channels` (`profileId`, `categoryName`)",
            "CREATE INDEX IF NOT EXISTS `index_channels_epgChannelId` ON `channels` (`epgChannelId`)",
            "CREATE INDEX IF NOT EXISTS `index_channels_isFavorite` ON `channels` (`isFavorite`)",
            "CREATE INDEX IF NOT EXISTS `index_channels_name` ON `channels` (`name`)",
            "CREATE TABLE IF NOT EXISTS `vod_movies` (`streamId` TEXT NOT NULL, `profileId` INTEGER NOT NULL, `title` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `posterUrl` TEXT, `backdropUrl` TEXT, `imdbRating` REAL, `releaseYear` INTEGER, `containerExtension` TEXT, `addedTimestamp` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`streamId`))",
            "CREATE INDEX IF NOT EXISTS `index_vod_movies_profileId_categoryName` ON `vod_movies` (`profileId`, `categoryName`)",
            "CREATE INDEX IF NOT EXISTS `index_vod_movies_imdbRating` ON `vod_movies` (`imdbRating`)",
            "CREATE INDEX IF NOT EXISTS `index_vod_movies_addedTimestamp` ON `vod_movies` (`addedTimestamp`)",
            "CREATE INDEX IF NOT EXISTS `index_vod_movies_title` ON `vod_movies` (`title`)",
            "CREATE TABLE IF NOT EXISTS `vod_series` (`seriesId` TEXT NOT NULL, `profileId` INTEGER NOT NULL, `title` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `posterUrl` TEXT, `backdropUrl` TEXT, `plot` TEXT, `imdbRating` REAL, `releaseYear` INTEGER, `lastModified` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`seriesId`))",
            "CREATE INDEX IF NOT EXISTS `index_vod_series_profileId_categoryName` ON `vod_series` (`profileId`, `categoryName`)",
            "CREATE INDEX IF NOT EXISTS `index_vod_series_imdbRating` ON `vod_series` (`imdbRating`)",
            "CREATE INDEX IF NOT EXISTS `index_vod_series_title` ON `vod_series` (`title`)",
            "CREATE TABLE IF NOT EXISTS `watch_history` (`contentId` TEXT NOT NULL, `profileId` INTEGER NOT NULL, `contentType` TEXT NOT NULL, `title` TEXT NOT NULL, `posterUrl` TEXT, `positionMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `lastWatchedAt` INTEGER NOT NULL, PRIMARY KEY(`contentId`))",
            "CREATE TABLE IF NOT EXISTS `epg_programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epgChannelId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `startTimeMs` INTEGER NOT NULL, `endTimeMs` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_epg_programs_epgChannelId_startTimeMs_endTimeMs` ON `epg_programs` (`epgChannelId`, `startTimeMs`, `endTimeMs`)"
        )
    }
}
