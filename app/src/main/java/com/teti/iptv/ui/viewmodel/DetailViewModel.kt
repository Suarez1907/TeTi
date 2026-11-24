package com.teti.iptv.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.PlaylistType
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.TmdbMetadataEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.domain.model.XtreamEpisode
import com.teti.iptv.domain.model.XtreamSeriesInfoResponse
import com.teti.iptv.repository.IptvRepository
import com.teti.iptv.repository.RatingRepository
import com.teti.iptv.repository.TmdbAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val ratingRepository: RatingRepository
) : ViewModel() {

    enum class DetailStatus { Loading, Ready, Missing }

    private val vodId = MutableStateFlow<String?>(null)
    private val seriesId = MutableStateFlow<String?>(null)

    /** Favori değişince ekran anında güncellensin diye Room akışı izlenir. */
    val vod: StateFlow<VodEntity?> = vodId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getVodByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vodMeta: StateFlow<TmdbMetadataEntity?> = vodId.flatMapLatest { id ->
        if (id == null) flowOf(null) else ratingRepository.observeMovie(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _vodStatus = MutableStateFlow(DetailStatus.Loading)
    val vodStatus: StateFlow<DetailStatus> = _vodStatus.asStateFlow()

    val series: StateFlow<SeriesEntity?> = seriesId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getSeriesByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val seriesMeta: StateFlow<TmdbMetadataEntity?> = seriesId.flatMapLatest { id ->
        if (id == null) flowOf(null) else ratingRepository.observeSeries(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _seriesStatus = MutableStateFlow(DetailStatus.Loading)
    val seriesStatus: StateFlow<DetailStatus> = _seriesStatus.asStateFlow()

    private val _seriesInfo = MutableStateFlow<XtreamSeriesInfoResponse?>(null)
    val seriesInfo: StateFlow<XtreamSeriesInfoResponse?> = _seriesInfo.asStateFlow()

    private val _selectedSeasonNumber = MutableStateFlow(1)
    val selectedSeasonNumber: StateFlow<Int> = _selectedSeasonNumber.asStateFlow()

    private val _metaLoading = MutableStateFlow(false)
    val metaLoading: StateFlow<Boolean> = _metaLoading.asStateFlow()

    private var seriesProfile: PlaylistProfileEntity? = null

    private val _similarMovies = MutableStateFlow<List<VodEntity>>(emptyList())
    val similarMovies: StateFlow<List<VodEntity>> = _similarMovies.asStateFlow()

    private val _similarSeries = MutableStateFlow<List<SeriesEntity>>(emptyList())
    val similarSeries: StateFlow<List<SeriesEntity>> = _similarSeries.asStateFlow()

    fun loadVod(streamId: String) {
        if (vodId.value == streamId) return
        vodId.value = streamId
        viewModelScope.launch {
            _vodStatus.value = DetailStatus.Loading
            val item = repository.getVodById(streamId)
            _vodStatus.value = if (item == null) DetailStatus.Missing else DetailStatus.Ready
            if (item != null) {
                loadMeta { ratingRepository.ensureMovieDetails(item.streamId, item.title) }
                _similarMovies.value = ratingRepository.similarMovies(item.profileId, item.streamId)
            }
        }
    }

    fun loadSeries(id: String) {
        if (seriesId.value == id) return
        seriesId.value = id
        viewModelScope.launch {
            _seriesStatus.value = DetailStatus.Loading
            val item = repository.getSeriesById(id)
            _seriesStatus.value = if (item == null) DetailStatus.Missing else DetailStatus.Ready
            if (item == null) return@launch
            launch {
                loadMeta { ratingRepository.ensureSeriesDetails(item.seriesId, item.title) }
                _similarSeries.value = ratingRepository.similarSeries(item.profileId, item.seriesId)
            }
            val activeProfile = repository.getActiveProfileOnce() ?: return@launch
            seriesProfile = activeProfile
            val info = repository.getSeriesInfo(activeProfile, id)
            _seriesInfo.value = info
            _selectedSeasonNumber.value = info?.seasons?.firstOrNull()?.seasonNumber
                ?: info?.episodes?.keys?.mapNotNull { it.toIntOrNull() }?.minOrNull()
                ?: 1
        }
    }

    private suspend fun loadMeta(block: suspend () -> Unit) {
        _metaLoading.value = true
        try {
            block()
        } catch (_: TmdbAuthException) {
            // Geçersiz anahtar: ekran IPTV verisiyle çalışmaya devam eder.
        } finally {
            _metaLoading.value = false
        }
    }

    fun toggleVodFavorite() {
        viewModelScope.launch {
            val item = vod.filterNotNull().first()
            repository.toggleVodFavorite(item)
        }
    }

    fun toggleSeriesFavorite() {
        viewModelScope.launch {
            val item = series.filterNotNull().first()
            repository.toggleSeriesFavorite(item)
        }
    }

    fun selectSeason(seasonNumber: Int) {
        _selectedSeasonNumber.value = seasonNumber
    }

    /** Sezon listesi: API sezon vermese bile bölüm anahtarlarından türetilir. */
    fun seasonNumbers(info: XtreamSeriesInfoResponse?): List<Int> {
        val fromSeasons = info?.seasons.orEmpty().map { it.seasonNumber }
        val fromEpisodes = info?.episodes?.keys.orEmpty().mapNotNull { it.toIntOrNull() }
        return (fromSeasons + fromEpisodes).filter { s -> info?.episodes?.get(s.toString()).orEmpty().isNotEmpty() }
            .distinct()
            .sorted()
    }

    fun getEpisodesForSeason(seasonNumber: Int): List<XtreamEpisode> {
        val info = _seriesInfo.value ?: return emptyList()
        return info.episodes?.get(seasonNumber.toString()) ?: emptyList()
    }

    fun getEpisodeStreamUrl(episode: XtreamEpisode): String? {
        val profile = seriesProfile ?: return null
        val username = profile.username ?: return null
        val password = profile.password ?: return null
        if (profile.type != PlaylistType.XTREAM) return null
        val extension = episode.containerExtension
            .trim()
            .removePrefix(".")
            .ifBlank { "mp4" }
        return "${profile.serverUrl.trimEnd('/')}/series/${Uri.encode(username)}/${Uri.encode(password)}/${episode.id}.$extension"
    }
}
