package com.teti.iptv.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.TeTiDatabase
import com.teti.iptv.data.local.dao.ChannelDao
import com.teti.iptv.data.local.dao.PlaylistDao
import com.teti.iptv.data.local.dao.SeriesDao
import com.teti.iptv.data.local.dao.VodDao
import com.teti.iptv.data.local.dao.WatchHistoryDao
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.PlaylistType
import com.teti.iptv.data.local.entity.WatchHistoryEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.domain.epg.EpgUpdateInterval
import com.teti.iptv.player.BufferProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long,
    val profiles: List<BackupProfile>,
    val favorites: List<String>,
    val hiddenChannels: List<String>,
    val history: List<BackupHistory>,
    val settings: BackupSettings,
    val lockedCategories: List<String> = emptyList()
)

@Serializable
data class BackupProfile(
    val id: Long,
    val name: String,
    val type: String,
    val serverUrl: String,
    val username: String? = null,
    val password: String? = null,
    val epgUrl: String? = null,
    val userAgent: String = "TeTi-IPTV/1.0",
    val isActive: Boolean = false
)

@Serializable
data class BackupHistory(
    val contentId: String,
    val profileId: Long,
    val contentType: String,
    val title: String,
    val posterUrl: String? = null,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long,
    val streamUrl: String? = null
)

@Serializable
data class BackupSettings(
    val bufferProfile: String? = null,
    val softwareDecoder: Boolean = false,
    val userAgent: String? = null,
    val tmdbKey: String = "",
    val omdbKey: String = "",
    val epgAutoXtream: Boolean = true,
    val epgCustomUrl: String = "",
    val epgInterval: String? = null,
    val epgOffsetHours: Int = 0,
    val vodSort: String? = null,
    val seriesSort: String? = null
)

/**
 * Profilleri, favorileri, izleme geçmişini ve ayarları kullanıcının seçtiği bir JSON
 * dosyasına yazar / dosyadan geri yükler (Google Drive vb. belge sağlayıcılarıyla bulut yedek).
 * Ebeveyn PIN'i yedeğe yazılmaz.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: TeTiDatabase,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val historyDao: WatchHistoryDao,
    private val preferences: AppPreferences,
    private val json: Json
) {
    private val pretty = Json(json) { prettyPrint = true }

    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val profiles = playlistDao.getAllProfiles().first()
        val favorites = profiles.flatMap { p ->
            channelDao.getFavoriteIds(p.id).map { "c:$it" } +
                vodDao.getFavoriteIds(p.id).map { "v:$it" } +
                seriesDao.getFavoriteIds(p.id).map { "s:$it" }
        } + preferences.pendingFavorites()
        val hidden = profiles.flatMap { channelDao.getHiddenIds(it.id) }
        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            profiles = profiles.map {
                BackupProfile(it.id, it.name, it.type.name, it.serverUrl, it.username, it.password, it.epgUrl, it.customUserAgent, it.isActive)
            },
            favorites = favorites.distinct(),
            hiddenChannels = hidden,
            history = historyDao.getAll().map {
                BackupHistory(it.contentId, it.profileId, it.contentType, it.title, it.posterUrl, it.positionMs, it.durationMs, it.lastWatchedAt, it.streamUrl)
            },
            settings = BackupSettings(
                bufferProfile = preferences.bufferProfile.value.name,
                softwareDecoder = preferences.useSoftwareDecoder.value,
                userAgent = preferences.userAgent.value,
                tmdbKey = preferences.tmdbApiKey.value,
                omdbKey = preferences.omdbApiKey.value,
                epgAutoXtream = preferences.epgAutoXtream.value,
                epgCustomUrl = preferences.epgCustomUrl.value,
                epgInterval = preferences.epgInterval.value.storageKey,
                epgOffsetHours = preferences.epgOffsetHours.value,
                vodSort = preferences.vodSort.value.storageKey,
                seriesSort = preferences.seriesSort.value.storageKey
            ),
            lockedCategories = preferences.lockedCategories.value.sorted()
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(pretty.encodeToString(BackupFile.serializer(), file).toByteArray())
        } ?: error("Dosya yazılamadı")
        profiles.size
    }

    /** @return geri yüklenen profil sayısı. Katalog, profil senkronize edilince yeniden iner. */
    suspend fun import(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Dosya okunamadı")
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.version == 1) { "Desteklenmeyen yedek sürümü: ${file.version}" }
        database.withTransaction {
            file.profiles.forEach { p ->
                playlistDao.insertProfile(
                    PlaylistProfileEntity(
                        id = p.id,
                        name = p.name,
                        type = runCatching { PlaylistType.valueOf(p.type) }.getOrDefault(PlaylistType.XTREAM),
                        serverUrl = p.serverUrl,
                        username = p.username,
                        password = p.password,
                        epgUrl = p.epgUrl,
                        customUserAgent = p.userAgent,
                        isActive = p.isActive
                    )
                )
            }
            historyDao.upsertAll(
                file.history.map {
                    WatchHistoryEntity(it.contentId, it.profileId, it.contentType, it.title, it.posterUrl, it.positionMs, it.durationMs, it.lastWatchedAt, it.streamUrl)
                }
            )
            // Katalog henüz yoksa favoriler ilk senkrondan sonra uygulanır.
            val pending = file.favorites.toMutableSet()
            file.favorites.forEach { key ->
                val id = key.substringAfter(':')
                val applied = when (key.substringBefore(':')) {
                    "c" -> channelDao.getById(id)?.also { channelDao.setFavorite(id, true) } != null
                    "v" -> vodDao.getById(id)?.also { vodDao.setFavorite(id, true) } != null
                    "s" -> seriesDao.getById(id)?.also { seriesDao.setFavorite(id, true) } != null
                    else -> true
                }
                if (applied) pending.remove(key)
            }
            preferences.setPendingFavorites(pending + preferences.pendingFavorites())
            file.hiddenChannels.forEach { channelDao.restoreHidden(it) }
        }
        with(file.settings) {
            bufferProfile?.let { name -> runCatching { BufferProfile.valueOf(name) }.getOrNull()?.let(preferences::setBufferProfile) }
            preferences.setSoftwareDecoder(softwareDecoder)
            userAgent?.let(preferences::setUserAgent)
            preferences.setTmdbApiKey(tmdbKey)
            preferences.setOmdbApiKey(omdbKey)
            preferences.setEpgAutoXtream(epgAutoXtream)
            preferences.setEpgCustomUrl(epgCustomUrl)
            preferences.setEpgInterval(EpgUpdateInterval.fromStorage(epgInterval))
            preferences.setEpgOffsetHours(epgOffsetHours)
            preferences.setVodSort(CatalogSort.fromStorage(vodSort))
            preferences.setSeriesSort(CatalogSort.fromStorage(seriesSort))
        }
        if (file.lockedCategories.isNotEmpty()) preferences.setLockedCategories(file.lockedCategories.toSet())
        file.profiles.size
    }
}
