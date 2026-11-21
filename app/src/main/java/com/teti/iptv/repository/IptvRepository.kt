package com.teti.iptv.repository

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.teti.iptv.domain.catalog.CatalogQuery
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.domain.epg.EpgOffset
import com.teti.iptv.worker.BackgroundScheduler
import kotlinx.coroutines.flow.map
import com.teti.iptv.data.local.TeTiDatabase
import com.teti.iptv.data.local.dao.ChannelDao
import com.teti.iptv.data.local.dao.EpgDao
import com.teti.iptv.data.local.AppPreferences
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
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.data.local.entity.WatchHistoryEntity
import com.teti.iptv.data.remote.XtreamApiService
import com.teti.iptv.data.remote.parser.StreamingM3uParser
import com.teti.iptv.data.remote.parser.StreamingXmltvParser
import com.teti.iptv.data.remote.parser.StreamingXtreamParser
import com.teti.iptv.domain.model.XtreamCategory
import com.teti.iptv.domain.model.XtreamSeriesInfoResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.io.InputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val epgDao: EpgDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val m3uParser: StreamingM3uParser,
    private val xtreamParser: StreamingXtreamParser,
    private val xmltvParser: StreamingXmltvParser,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val database: TeTiDatabase,
    private val tmdbMetadataDao: TmdbMetadataDao,
    private val preferences: AppPreferences,
    private val scheduler: BackgroundScheduler
) {
    private fun createXtreamApi(baseUrl: String): XtreamApiService {
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val retrofit = Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(okHttpClient)
            .build()
        return retrofit.create(XtreamApiService::class.java)
    }

    // --- PROFİLLER ---
    fun getAllProfiles(): Flow<List<PlaylistProfileEntity>> = playlistDao.getAllProfiles()
    fun getActiveProfile(): Flow<PlaylistProfileEntity?> = playlistDao.getActiveProfile()
    suspend fun getActiveProfileOnce(): PlaylistProfileEntity? = playlistDao.getActiveProfileOnce()
    suspend fun addProfile(profile: PlaylistProfileEntity): Long = playlistDao.insertProfile(profile)
    suspend fun updateProfile(profile: PlaylistProfileEntity) = playlistDao.updateProfile(profile)
    suspend fun deleteProfile(profile: PlaylistProfileEntity) {
        channelDao.deleteByProfile(profile.id)
        vodDao.deleteByProfile(profile.id)
        seriesDao.deleteByProfile(profile.id)
        playlistDao.deleteProfile(profile)
    }
    suspend fun setActiveProfile(profileId: Long) = playlistDao.setActiveProfile(profileId)

    // --- KANALLAR (CANLI TV) ---
    fun getChannelsPaged(profileId: Long, category: String? = null): Flow<PagingData<ChannelEntity>> {
        return Pager(PagingConfig(pageSize = 50, prefetchDistance = 15, enablePlaceholders = false)) {
            if (category.isNullOrBlank() || category == "Tümü") {
                channelDao.getChannelsPaging(profileId)
            } else if (category == "Favoriler") {
                channelDao.getFavoriteChannelsPaging(profileId)
            } else {
                channelDao.getChannelsByCategoryPaging(profileId, category)
            }
        }.flow
    }

    suspend fun getChannelById(streamId: String): ChannelEntity? = channelDao.getById(streamId)

    fun getChannelCategories(profileId: Long): Flow<List<String>> = channelDao.getCategories(profileId)
    suspend fun toggleChannelFavorite(channel: ChannelEntity) {
        channelDao.setFavorite(channel.streamId, !channel.isFavorite)
    }

    // --- FİLMLER (VOD) ---
    fun getVodPaged(
        profileId: Long,
        category: String? = null,
        year: Int? = null,
        sort: CatalogSort = CatalogSort.RECENTLY_ADDED
    ): Flow<PagingData<VodEntity>> {
        val bound = CatalogQuery.vod(profileId, category, year, sort)
        return Pager(
            PagingConfig(
                pageSize = 48,
                prefetchDistance = 12,
                enablePlaceholders = false,
                maxSize = 240
            )
        ) {
            vodDao.getVodPagingRaw(SimpleSQLiteQuery(bound.sql, bound.args.toTypedArray()))
        }.flow
    }

    fun getVodCategories(profileId: Long): Flow<List<String>> = vodDao.getCategories(profileId)
    fun getVodYears(profileId: Long): Flow<List<Int>> = vodDao.getReleaseYears(profileId)
    fun getTopRatedMovies(profileId: Long, limit: Int = 20): Flow<List<VodEntity>> = vodDao.getTopRated(profileId, limit)
    fun getRecentlyAddedMovies(profileId: Long, limit: Int = 20): Flow<List<VodEntity>> = vodDao.getRecentlyAdded(profileId, limit)
    suspend fun getVodById(streamId: String): VodEntity? = vodDao.getById(streamId)
    fun getVodByIdFlow(streamId: String): Flow<VodEntity?> = vodDao.getByIdFlow(streamId)
    suspend fun toggleVodFavorite(vod: VodEntity) = vodDao.setFavorite(vod.streamId, !vod.isFavorite)

    // --- DİZİLER ---
    fun getSeriesPaged(
        profileId: Long,
        category: String? = null,
        year: Int? = null,
        sort: CatalogSort = CatalogSort.RECENTLY_ADDED
    ): Flow<PagingData<SeriesEntity>> {
        val bound = CatalogQuery.series(profileId, category, year, sort)
        return Pager(
            PagingConfig(
                pageSize = 48,
                prefetchDistance = 12,
                enablePlaceholders = false,
                maxSize = 240
            )
        ) {
            seriesDao.getSeriesPagingRaw(SimpleSQLiteQuery(bound.sql, bound.args.toTypedArray()))
        }.flow
    }

    fun getSeriesCategories(profileId: Long): Flow<List<String>> = seriesDao.getCategories(profileId)
    fun getSeriesYears(profileId: Long): Flow<List<Int>> = seriesDao.getReleaseYears(profileId)
    fun getTopRatedSeries(profileId: Long, limit: Int = 20): Flow<List<SeriesEntity>> = seriesDao.getTopRated(profileId, limit)
    fun getRecentlyAddedSeries(profileId: Long, limit: Int = 20): Flow<List<SeriesEntity>> = seriesDao.getRecentlyAdded(profileId, limit)
    suspend fun getSeriesById(seriesId: String): SeriesEntity? = seriesDao.getById(seriesId)
    fun getSeriesByIdFlow(seriesId: String): Flow<SeriesEntity?> = seriesDao.getByIdFlow(seriesId)
    suspend fun toggleSeriesFavorite(series: SeriesEntity) = seriesDao.setFavorite(series.seriesId, !series.isFavorite)

    suspend fun getSeriesInfo(profile: PlaylistProfileEntity, rawSeriesId: String): XtreamSeriesInfoResponse? = withContext(Dispatchers.IO) {
        if (profile.type != PlaylistType.XTREAM || profile.username == null || profile.password == null) return@withContext null
        val api = createXtreamApi(profile.serverUrl)
        val cleanId = rawSeriesId.removePrefix("${profile.id}_")
        runCatching {
            val responseBody = api.getSeriesInfo(profile.username, profile.password, seriesId = cleanId)
            val jsonStr = responseBody.string()
            json.decodeFromString<XtreamSeriesInfoResponse>(jsonStr)
        }.getOrNull()
    }

    // --- ARAMA ---
    fun searchChannels(profileId: Long, query: String): Flow<List<ChannelEntity>> = channelDao.searchChannels(profileId, query)
    fun searchVod(profileId: Long, query: String): Flow<List<VodEntity>> = vodDao.searchVod(profileId, query)
    fun searchSeries(profileId: Long, query: String): Flow<List<SeriesEntity>> = seriesDao.searchSeries(profileId, query)

    // --- EPG ---
    // Saatler ham XMLTV değeriyle saklanır; kullanıcının saat kaydırması (EPG Offset)
    // sorguda ve gösterimde uygulanır, böylece ayar değişince yeniden indirme gerekmez.
    private fun epgShiftMs(): Long = EpgOffset.toMillis(preferences.epgOffsetHours.value)

    private fun EpgProgramEntity.shifted(shift: Long): EpgProgramEntity =
        if (shift == 0L) this else copy(startTimeMs = startTimeMs + shift, endTimeMs = endTimeMs + shift)

    fun getCurrentEpg(epgChannelId: String): Flow<EpgProgramEntity?> {
        val shift = epgShiftMs()
        return epgDao.getCurrentProgram(epgChannelId, System.currentTimeMillis() - shift)
            .map { it?.shifted(shift) }
    }

    /** Şu an yayındaki program (kaydırma uygulanmış). */
    suspend fun getNowPlaying(epgChannelId: String, nowMs: Long = System.currentTimeMillis()): EpgProgramEntity? {
        val shift = epgShiftMs()
        return epgDao.getCurrentProgramOnce(epgChannelId, nowMs - shift)?.shifted(shift)
    }

    fun getProgramsForChannel(epgChannelId: String, fromMs: Long, toMs: Long): Flow<List<EpgProgramEntity>> {
        val shift = epgShiftMs()
        return epgDao.getProgramsForChannel(epgChannelId, fromMs - shift, toMs - shift)
            .map { list -> list.map { it.shifted(shift) } }
    }

    /**
     * Kullanılacak XMLTV adresi: Ayarlar'daki özel URL > profilin EPG URL'si >
     * (otomatik açıksa) Xtream sunucusunun xmltv.php adresi.
     */
    fun resolveEpgUrl(profile: PlaylistProfileEntity): String? {
        preferences.epgCustomUrl.value.takeIf { it.isNotBlank() }?.let { return it }
        profile.epgUrl?.takeIf { it.isNotBlank() }?.let { return it }
        if (preferences.epgAutoXtream.value && profile.type == PlaylistType.XTREAM) {
            val u = profile.username ?: return null
            val p = profile.password ?: return null
            return "${profile.serverUrl.trimEnd('/')}/xmltv.php?username=${Uri.encode(u)}&password=${Uri.encode(p)}"
        }
        return null
    }

    /** Etkin profilin EPG'sini indirir. @return indirilen URL yoksa false. */
    suspend fun syncEpgForActiveProfile(): Boolean = withContext(Dispatchers.IO) {
        val profile = playlistDao.getActiveProfileOnce() ?: return@withContext false
        val url = resolveEpgUrl(profile) ?: return@withContext false
        syncEpg(url)
        true
    }

    // --- İZLEME GEÇMİŞİ ---
    fun getRecentHistory(profileId: Long): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getRecentHistory(profileId)
    suspend fun getWatchPosition(contentId: String): Long = watchHistoryDao.getPosition(contentId) ?: 0L
    suspend fun saveWatchProgress(item: WatchHistoryEntity) = watchHistoryDao.upsert(item)

    // --- SENKRONİZASYON ---
    suspend fun syncProfile(profileId: Long, onProgress: ((String) -> Unit)? = null) = withContext(Dispatchers.IO) {
        val profile = playlistDao.getProfileById(profileId) ?: return@withContext
        when (profile.type) {
            PlaylistType.XTREAM -> syncXtream(profile, onProgress)
            PlaylistType.M3U_URL -> syncM3uUrl(profile, onProgress)
            PlaylistType.M3U_LOCAL -> syncM3uLocal(profile, onProgress)
        }
        resolveEpgUrl(profile)?.let { epgUrl ->
            onProgress?.invoke("EPG senkronize ediliyor...")
            runCatching { syncEpg(epgUrl) }
                .onFailure { onProgress?.invoke("EPG alınamadı: ${it.message}") }
        }
        playlistDao.updateLastSynced(profileId, System.currentTimeMillis())
        // Yeni gelen filmler/diziler için TMDB puan ve özetlerini arka planda doldur.
        scheduler.enqueueTmdbEnrich(restart = false)
    }

    private suspend fun syncXtream(profile: PlaylistProfileEntity, onProgress: ((String) -> Unit)?) {
        val u = profile.username ?: error("Xtream kullanıcı adı eksik")
        val p = profile.password ?: error("Xtream şifresi eksik")
        val api = createXtreamApi(profile.serverUrl)

        // 1. Kategoriler
        onProgress?.invoke("Kategoriler alınıyor...")
        val liveCatMap = runCatching {
            val body = api.getLiveCategories(u, p).string()
            json.decodeFromString<List<XtreamCategory>>(body).associate { it.categoryId to it.categoryName }
        }.getOrDefault(emptyMap())

        val vodCatMap = runCatching {
            val body = api.getVodCategories(u, p).string()
            json.decodeFromString<List<XtreamCategory>>(body).associate { it.categoryId to it.categoryName }
        }.getOrDefault(emptyMap())

        val seriesCatMap = runCatching {
            val body = api.getSeriesCategories(u, p).string()
            json.decodeFromString<List<XtreamCategory>>(body).associate { it.categoryId to it.categoryName }
        }.getOrDefault(emptyMap())

        // 2. Canlı Kanallar
        onProgress?.invoke("Canlı kanallar indiriliyor...")
        val liveFile = stagingFile("live")
        val vodFile = stagingFile("vod")
        val seriesFile = stagingFile("series")
        try {
            liveFile.bufferedWriter().use { writer ->
                api.getLiveStreams(u, p).byteStream().use { stream ->
                    xtreamParser.parseLiveStreams(stream, profile.id, profile.serverUrl, u, p, liveCatMap) { batch ->
                        writer.write(json.encodeToString(batch))
                        writer.newLine()
                    }
                }
            }

            // 3. Filmler (VOD)
            onProgress?.invoke("Filmler indiriliyor...")
            vodFile.bufferedWriter().use { writer ->
                api.getVodStreams(u, p).byteStream().use { stream ->
                    xtreamParser.parseVodStreams(stream, profile.id, profile.serverUrl, u, p, vodCatMap) { batch ->
                        writer.write(json.encodeToString(batch))
                        writer.newLine()
                    }
                }
            }

            // 4. Diziler
            onProgress?.invoke("Diziler indiriliyor...")
            seriesFile.bufferedWriter().use { writer ->
                api.getSeries(u, p).byteStream().use { stream ->
                    xtreamParser.parseSeriesStreams(stream, profile.id, seriesCatMap) { batch ->
                        writer.write(json.encodeToString(batch))
                        writer.newLine()
                    }
                }
            }

            val favoriteChannelIds = channelDao.getFavoriteIds(profile.id)
            val hiddenChannelIds = channelDao.getHiddenIds(profile.id)
            val favoriteVodIds = vodDao.getFavoriteIds(profile.id)
            val favoriteSeriesIds = seriesDao.getFavoriteIds(profile.id)
            database.withTransaction {
                channelDao.deleteByProfile(profile.id)
                replayStaged<ChannelEntity>(liveFile, channelDao::insertAll)
                favoriteChannelIds.forEach { channelDao.setFavorite(it, true) }
                hiddenChannelIds.forEach { channelDao.restoreHidden(it) }
                vodDao.deleteByProfile(profile.id)
                replayStaged<VodEntity>(vodFile, vodDao::insertAll)
                favoriteVodIds.forEach { vodDao.setFavorite(it, true) }
                tmdbMetadataDao.applyMovieRatings(profile.id)
                seriesDao.deleteByProfile(profile.id)
                replayStaged<SeriesEntity>(seriesFile, seriesDao::insertAll)
                favoriteSeriesIds.forEach { seriesDao.setFavorite(it, true) }
                tmdbMetadataDao.applySeriesRatings(profile.id)
            }
        } finally {
            liveFile.delete()
            vodFile.delete()
            seriesFile.delete()
        }
    }

    private suspend fun syncM3uUrl(profile: PlaylistProfileEntity, onProgress: ((String) -> Unit)?) {
        onProgress?.invoke("M3U çalma listesi indiriliyor...")
        val request = Request.Builder().url(profile.serverUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "M3U indirilemedi (HTTP ${response.code})" }
            val channelFile = stagingFile("m3u-channels")
            val vodFile = stagingFile("m3u-vod")
            val body = response.body ?: error("M3U yanıtı boş")
            try {
                channelFile.bufferedWriter().use { channelWriter ->
                    vodFile.bufferedWriter().use { vodWriter ->
                        body.byteStream().use {
                            m3uParser.parse(
                                it,
                                profile.id,
                                onChannelsBatch = { batch ->
                                    channelWriter.write(json.encodeToString(batch))
                                    channelWriter.newLine()
                                },
                                onVodBatch = { batch ->
                                    vodWriter.write(json.encodeToString(batch))
                                    vodWriter.newLine()
                                }
                            )
                        }
                    }
                }
                val favoriteChannelIds = channelDao.getFavoriteIds(profile.id)
                val hiddenChannelIds = channelDao.getHiddenIds(profile.id)
                val favoriteVodIds = vodDao.getFavoriteIds(profile.id)
                    database.withTransaction {
                    channelDao.deleteByProfile(profile.id)
                    replayStaged<ChannelEntity>(channelFile, channelDao::insertAll)
                    favoriteChannelIds.forEach { channelDao.setFavorite(it, true) }
                    hiddenChannelIds.forEach { channelDao.restoreHidden(it) }
                    vodDao.deleteByProfile(profile.id)
                    replayStaged<VodEntity>(vodFile, vodDao::insertAll)
                    favoriteVodIds.forEach { vodDao.setFavorite(it, true) }
                    tmdbMetadataDao.applyMovieRatings(profile.id)
                }
            } finally {
                channelFile.delete()
                vodFile.delete()
            }
        }
    }

    private suspend fun syncM3uLocal(profile: PlaylistProfileEntity, onProgress: ((String) -> Unit)?) {
        onProgress?.invoke("Yerel dosya okunuyor...")
        val uri = Uri.parse(profile.serverUrl)
        val stream: InputStream = context.contentResolver.openInputStream(uri)
            ?: error("Yerel M3U dosyası açılamadı")
        val channelFile = stagingFile("local-m3u-channels")
        val vodFile = stagingFile("local-m3u-vod")
        try {
            channelFile.bufferedWriter().use { channelWriter ->
                vodFile.bufferedWriter().use { vodWriter ->
                    stream.use {
                        m3uParser.parse(
                            it,
                            profile.id,
                            onChannelsBatch = { batch ->
                                channelWriter.write(json.encodeToString(batch))
                                channelWriter.newLine()
                            },
                            onVodBatch = { batch ->
                                vodWriter.write(json.encodeToString(batch))
                                vodWriter.newLine()
                            }
                        )
                    }
                }
            }
            val favoriteChannelIds = channelDao.getFavoriteIds(profile.id)
            val hiddenChannelIds = channelDao.getHiddenIds(profile.id)
            val favoriteVodIds = vodDao.getFavoriteIds(profile.id)
            database.withTransaction {
                channelDao.deleteByProfile(profile.id)
                replayStaged<ChannelEntity>(channelFile, channelDao::insertAll)
                favoriteChannelIds.forEach { channelDao.setFavorite(it, true) }
                hiddenChannelIds.forEach { channelDao.restoreHidden(it) }
                vodDao.deleteByProfile(profile.id)
                replayStaged<VodEntity>(vodFile, vodDao::insertAll)
                favoriteVodIds.forEach { vodDao.setFavorite(it, true) }
                tmdbMetadataDao.applyMovieRatings(profile.id)
            }
        } finally {
            channelFile.delete()
            vodFile.delete()
        }
    }

    suspend fun syncEpg(epgUrl: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(epgUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "EPG indirilemedi (HTTP ${response.code})" }
            val epgFile = stagingFile("epg")
            val body = response.body ?: error("EPG yanıtı boş")
            try {
                epgFile.bufferedWriter().use { writer ->
                    body.byteStream().use {
                        xmltvParser.parse(it) { batch ->
                            writer.write(json.encodeToString(batch))
                            writer.newLine()
                        }
                    }
                }
                database.withTransaction {
                    epgDao.clearAll()
                    replayStaged<EpgProgramEntity>(epgFile, epgDao::insertAll)
                }
                preferences.setEpgLastUpdated(System.currentTimeMillis())
            } finally {
                epgFile.delete()
            }
        }
    }

    private fun stagingFile(prefix: String): File =
        File(context.cacheDir, "teti-$prefix-${UUID.randomUUID()}.jsonl")

    private suspend inline fun <reified T> replayStaged(
        file: File,
        noinline insert: suspend (List<T>) -> Unit
    ) {
        file.bufferedReader().use { reader ->
            reader.lineSequence().filter(String::isNotBlank).forEach { line ->
                insert(json.decodeFromString<List<T>>(line))
            }
        }
    }
}
