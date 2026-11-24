package com.teti.iptv.repository

import android.util.Log
import androidx.room.withTransaction
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.TeTiDatabase
import com.teti.iptv.data.local.TmdbCredential
import com.teti.iptv.data.local.dao.SeriesDao
import com.teti.iptv.data.local.dao.TmdbMatchDao
import com.teti.iptv.data.local.dao.TmdbMetadataDao
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.domain.model.TmdbIdPage
import com.teti.iptv.data.local.dao.VodDao
import com.teti.iptv.data.local.entity.TmdbMetadataEntity
import com.teti.iptv.data.remote.RatingApiService
import com.teti.iptv.data.remote.RatingApiService.Companion.TMDB_BASE
import com.teti.iptv.data.remote.RatingApiService.Companion.TMDB_IMAGE_BASE
import com.teti.iptv.data.remote.parser.TitleCleaner
import com.teti.iptv.domain.model.OmdbResponse
import com.teti.iptv.domain.model.TmdbDetails
import com.teti.iptv.domain.model.TmdbSearchResponse
import com.teti.iptv.domain.tmdb.TmdbMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** TMDB anahtarı reddedildi (HTTP 401). Toplu işlem durur, kullanıcıya bildirilir. */
class TmdbAuthException : Exception("TMDB anahtarı geçersiz (HTTP 401)")

/**
 * Puan ve meta veri servisi. Birincil kaynak TMDB v3 (tr-TR); OMDb yalnızca
 * TMDB eşleşme bulamazsa ve kullanıcı OMDb anahtarı girdiyse yedek olarak denenir.
 * Sağlayıcının (IPTV paneli) gönderdiği puanlar kullanılmaz.
 */
@Singleton
class RatingRepository @Inject constructor(
    private val api: RatingApiService,
    private val preferences: AppPreferences,
    private val metadataDao: TmdbMetadataDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val database: TeTiDatabase,
    private val matchDao: TmdbMatchDao,
    private val json: Json
) {
    enum class Kind(val path: String) { MOVIE("movie"), TV("tv") }

    private val inFlight = Mutex()
    private val inFlightKeys = HashSet<String>()

    fun observeMovie(streamId: String): Flow<TmdbMetadataEntity?> =
        metadataDao.observe(TmdbMetadataEntity.movieKey(streamId))

    fun observeSeries(seriesId: String): Flow<TmdbMetadataEntity?> =
        metadataDao.observe(TmdbMetadataEntity.tvKey(seriesId))

    /** Detay ekranı: arama + ayrıntı (süre, türler, oyuncular) önbellekte yoksa çeker. */
    suspend fun ensureMovieDetails(streamId: String, rawTitle: String) =
        ensure(Kind.MOVIE, streamId, rawTitle, withDetails = true)

    suspend fun ensureSeriesDetails(seriesId: String, rawTitle: String) =
        ensure(Kind.TV, seriesId, rawTitle, withDetails = true)

    /** Eski API: yalnızca puanı doldurur. */
    suspend fun enrichVodRating(streamId: String, rawTitle: String) =
        ensure(Kind.MOVIE, streamId, rawTitle, withDetails = false)

    suspend fun enrichSeriesRating(seriesId: String, rawTitle: String) =
        ensure(Kind.TV, seriesId, rawTitle, withDetails = false)

    /** Önbelleği ve tüm puanları siler; ardından [enrichPending] TMDB'den yeniden doldurur. */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        database.withTransaction {
            metadataDao.clearAll()
            metadataDao.clearMovieRatings()
            metadataDao.clearSeriesRatings()
        }
    }

    /** Senkron sonrası katalog satırlarına TMDB puanlarını geri yazar. */
    suspend fun reapplyRatings(profileId: Long) = withContext(Dispatchers.IO) {
        metadataDao.applyMovieRatings(profileId)
        metadataDao.applySeriesRatings(profileId)
    }

    /**
     * Henüz TMDB'de aranmamış tüm filmleri ve dizileri arar.
     * @return işlenen öğe sayısı
     */
    suspend fun enrichPending(
        profileId: Long,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val total = metadataDao.catalogSize(profileId)
        var done = 0
        val gate = Semaphore(PARALLEL_REQUESTS)
        for (kind in Kind.entries) {
            while (true) {
                val batch = when (kind) {
                    Kind.MOVIE -> metadataDao.pendingMovies(profileId, BATCH)
                    Kind.TV -> metadataDao.pendingSeries(profileId, BATCH)
                }
                if (batch.isEmpty()) break
                val written = coroutineScope {
                    batch.map { item ->
                        async { gate.withPermit { ensure(kind, item.id, item.title, withDetails = false) } }
                    }.awaitAll()
                }.count { it }
                done += batch.size
                onProgress(done.coerceAtMost(total), total)
                // Ağ hatasıyla hiçbiri yazılamadıysa aynı listeyi sonsuza dek denemeyelim.
                if (written == 0) break
            }
        }
        done
    }

    // --- Öneri rafları: TMDB listeleri katalogdaki içeriklerle eşleştirilir ---
    private data class CachedIds(val ids: List<Long>, val at: Long)
    private val idCache = java.util.concurrent.ConcurrentHashMap<String, CachedIds>()

    /** Bu hafta TMDB'de trend olan ve katalogda bulunan filmler (trend sırasıyla). */
    suspend fun trendingMovies(profileId: Long, limit: Int = 20): List<VodEntity> = withContext(Dispatchers.IO) {
        val ids = idsFor("trending/movie/week") ?: return@withContext emptyList()
        ordered(ids, matchDao.moviesByTmdbIds(profileId, ids.take(MATCH_WINDOW))) { streamId }.take(limit)
    }

    suspend fun trendingSeries(profileId: Long, limit: Int = 20): List<SeriesEntity> = withContext(Dispatchers.IO) {
        val ids = idsFor("trending/tv/week") ?: return@withContext emptyList()
        ordered(ids, matchDao.seriesByTmdbIds(profileId, ids.take(MATCH_WINDOW))) { seriesId }.take(limit)
    }

    /** Detay ekranı: TMDB önerilerinden katalogda olanlar. */
    suspend fun similarMovies(profileId: Long, streamId: String): List<VodEntity> = withContext(Dispatchers.IO) {
        val tmdbId = metadataDao.get(TmdbMetadataEntity.movieKey(streamId))?.tmdbId ?: return@withContext emptyList()
        val ids = idsFor("movie/$tmdbId/recommendations") ?: return@withContext emptyList()
        ordered(ids, matchDao.moviesByTmdbIds(profileId, ids)) { this.streamId }.filter { it.streamId != streamId }
    }

    suspend fun similarSeries(profileId: Long, seriesId: String): List<SeriesEntity> = withContext(Dispatchers.IO) {
        val tmdbId = metadataDao.get(TmdbMetadataEntity.tvKey(seriesId))?.tmdbId ?: return@withContext emptyList()
        val ids = idsFor("tv/$tmdbId/recommendations") ?: return@withContext emptyList()
        ordered(ids, matchDao.seriesByTmdbIds(profileId, ids)) { this.seriesId }.filter { it.seriesId != seriesId }
    }

    /** TMDB sırasını korur; aynı TMDB kimliğine bağlı birden fazla kopyadan birini alır. */
    private suspend fun <T> ordered(ids: List<Long>, items: List<T>, idOf: T.() -> String): List<T> {
        if (items.isEmpty()) return emptyList()
        val keyPrefix = if (items.first() is VodEntity) "movie:" else "tv:"
        val tmdbByKey = matchDao.tmdbIdsFor(items.map { keyPrefix + it.idOf() }).associate { it.contentKey to it.tmdbId }
        val rank = ids.withIndex().associate { (i, id) -> id to i }
        return items
            .mapNotNull { item -> tmdbByKey[keyPrefix + item.idOf()]?.let { item to it } }
            .sortedBy { (_, tmdb) -> rank[tmdb] ?: Int.MAX_VALUE }
            .distinctBy { (_, tmdb) -> tmdb }
            .map { it.first }
    }

    private suspend fun idsFor(path: String): List<Long>? {
        idCache[path]?.takeIf { System.currentTimeMillis() - it.at < LIST_TTL_MS }?.let { return it.ids }
        val credential = preferences.tmdbCredential()
        if (credential.isBlank) return null
        return try {
            val pages = (1..2).mapNotNull { page ->
                tmdbCall {
                    val body = api.tmdbGet(
                        url = "$TMDB_BASE$path",
                        authorization = credential.header(),
                        apiKey = credential.queryKey(),
                        page = page
                    ).string()
                    json.decodeFromString<TmdbIdPage>(body)
                }
            }
            pages.flatMap { it.results.orEmpty().mapNotNull { r -> r.id } }.distinct()
                .also { idCache[path] = CachedIds(it, System.currentTimeMillis()) }
        } catch (e: IOException) {
            null
        } catch (e: TmdbAuthException) {
            null
        }
    }

    /** @return önbelleğe bir kayıt yazıldıysa true (eşleşme yok da bir sonuçtur). */
    private suspend fun ensure(kind: Kind, id: String, rawTitle: String, withDetails: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val key = keyOf(kind, id)
            val cached = metadataDao.get(key)
            if (cached != null && (cached.detailsLoaded || !withDetails || cached.tmdbId == null)) {
                return@withContext false
            }
            if (!claim(key)) return@withContext false
            try {
                val credential = preferences.tmdbCredential()
                val cleaned = TitleCleaner.clean(rawTitle)
                var entity = cached ?: search(kind, credential, cleaned.title, cleaned.year)?.let { hit ->
                    TmdbMetadataEntity(
                        contentKey = key,
                        tmdbId = hit.id,
                        voteAverage = TmdbMatcher.trustedRating(hit.voteAverage, hit.voteCount),
                        overview = hit.overview?.ifBlank { null },
                        backdropUrl = image(hit.backdropPath, "w1280"),
                        posterUrl = image(hit.posterPath, "w500"),
                        releaseDate = (hit.releaseDate ?: hit.firstAirDate)?.ifBlank { null },
                        fetchedAt = System.currentTimeMillis()
                    )
                } ?: omdbFallback(key, cleaned.title, cleaned.year)

                val tmdbId = entity.tmdbId
                if (withDetails && tmdbId != null) {
                    details(kind, credential, tmdbId)?.let { d -> entity = merge(entity, d) }
                    if (entity.overview.isNullOrBlank()) {
                        // Türkçe özet yoksa İngilizcesini göster.
                        details(kind, credential, tmdbId, language = "en-US", withCredits = false)
                            ?.overview?.ifBlank { null }
                            ?.let { entity = entity.copy(overview = it) }
                    }
                }
                metadataDao.upsert(entity)
                when (kind) {
                    Kind.MOVIE -> vodDao.updateRating(id, entity.voteAverage)
                    Kind.TV -> seriesDao.updateRating(id, entity.voteAverage)
                }
                true
            } catch (e: TmdbAuthException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "TMDB isteği başarısız: ${e.message}")
                false
            } finally {
                release(key)
            }
        }

    private suspend fun claim(key: String): Boolean = inFlight.withLock { inFlightKeys.add(key) }
    private suspend fun release(key: String) = inFlight.withLock { inFlightKeys.remove(key) }

    private suspend fun search(kind: Kind, credential: TmdbCredential, title: String, year: Int?) =
        tmdbCall {
            val body = api.tmdbGet(
                url = "${TMDB_BASE}search/${kind.path}",
                authorization = credential.header(),
                apiKey = credential.queryKey(),
                query = title,
                year = if (kind == Kind.MOVIE) year else null,
                firstAirYear = if (kind == Kind.TV) year else null
            ).string()
            val results = json.decodeFromString<TmdbSearchResponse>(body).results.orEmpty()
            TmdbMatcher.pickBest(results, title, year)
        }

    private suspend fun details(
        kind: Kind,
        credential: TmdbCredential,
        tmdbId: Long,
        language: String = RatingApiService.TMDB_LANGUAGE,
        withCredits: Boolean = true
    ): TmdbDetails? = tmdbCall {
        val body = api.tmdbGet(
            url = "${TMDB_BASE}${kind.path}/$tmdbId",
            authorization = credential.header(),
            apiKey = credential.queryKey(),
            language = language,
            append = if (withCredits) "credits" else null
        ).string()
        json.decodeFromString<TmdbDetails>(body)
    }

    /** HTTP 401 → [TmdbAuthException]; 404 ve bozuk yanıt → null; ağ hatası → IOException. */
    private suspend fun <T> tmdbCall(block: suspend () -> T?): T? {
        return try {
            block()
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> throw TmdbAuthException()
                429 -> throw IOException("TMDB hız sınırı (429)")
                in 500..599 -> throw IOException("TMDB sunucu hatası (${e.code()})")
                else -> null
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            null
        }
    }

    private fun merge(base: TmdbMetadataEntity, d: TmdbDetails): TmdbMetadataEntity {
        val runtime = d.runtime?.takeIf { it > 0 } ?: d.episodeRunTime?.firstOrNull { it > 0 }
        val directors = d.credits?.crew.orEmpty().filter { it.job == "Director" }.mapNotNull { it.name }
        val creators = d.createdBy.orEmpty().mapNotNull { it.name }
        return base.copy(
            overview = d.overview?.ifBlank { null } ?: base.overview,
            backdropUrl = image(d.backdropPath, "w1280") ?: base.backdropUrl,
            posterUrl = image(d.posterPath, "w500") ?: base.posterUrl,
            releaseDate = (d.releaseDate ?: d.firstAirDate)?.ifBlank { null } ?: base.releaseDate,
            runtimeMinutes = runtime,
            genres = d.genres.orEmpty().mapNotNull { it.name }.take(3).joinToString(", ").ifBlank { null },
            cast = d.credits?.cast.orEmpty().sortedBy { it.order ?: Int.MAX_VALUE }
                .mapNotNull { it.name }.take(6).joinToString(", ").ifBlank { null },
            director = (directors.ifEmpty { creators }).take(2).joinToString(", ").ifBlank { null },
            detailsLoaded = true,
            fetchedAt = System.currentTimeMillis()
        )
    }

    /** TMDB bulamadığında: OMDb anahtarı varsa puan ve özet oradan, yoksa "eşleşme yok" kaydı. */
    private suspend fun omdbFallback(key: String, title: String, year: Int?): TmdbMetadataEntity {
        val omdbKey = preferences.omdbApiKey.value
        val omdb = if (omdbKey.isNotBlank()) {
            runCatching {
                val body = api.getOmdbByTitle(apiKey = omdbKey, title = title, year = year).string()
                json.decodeFromString<OmdbResponse>(body).takeIf { it.response.equals("True", true) }
            }.getOrNull()
        } else null
        return TmdbMetadataEntity(
            contentKey = key,
            voteAverage = omdb?.imdbRating?.toFloatOrNull()?.takeIf { it > 0f },
            overview = omdb?.plot?.takeIf { it.isNotBlank() && it != "N/A" },
            releaseDate = omdb?.year?.take(4),
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun keyOf(kind: Kind, id: String) = when (kind) {
        Kind.MOVIE -> TmdbMetadataEntity.movieKey(id)
        Kind.TV -> TmdbMetadataEntity.tvKey(id)
    }

    private fun image(path: String?, size: String): String? =
        path?.takeIf { it.isNotBlank() }?.let { "$TMDB_IMAGE_BASE$size$it" }

    private fun TmdbCredential.header(): String? = (this as? TmdbCredential.Bearer)?.let { "Bearer ${it.token}" }
    private fun TmdbCredential.queryKey(): String? = (this as? TmdbCredential.ApiKey)?.key

    companion object {
        private const val TAG = "RatingRepository"
        private const val PARALLEL_REQUESTS = 4
        private const val BATCH = 40
        private const val LIST_TTL_MS = 6 * 60 * 60 * 1000L
        private const val MATCH_WINDOW = 400
    }
}
