package com.teti.iptv.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.PlaylistType
import com.teti.iptv.domain.model.XtreamEpisode
import com.teti.iptv.domain.model.XtreamSeason
import com.teti.iptv.domain.model.XtreamSeriesInfoResponse
import com.teti.iptv.repository.IptvRepository
import com.teti.iptv.repository.RatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _vod = MutableStateFlow<VodEntity?>(null)
    val vod: StateFlow<VodEntity?> = _vod.asStateFlow()

    private val _series = MutableStateFlow<SeriesEntity?>(null)
    val series: StateFlow<SeriesEntity?> = _series.asStateFlow()

    private val _seriesInfo = MutableStateFlow<XtreamSeriesInfoResponse?>(null)
    val seriesInfo: StateFlow<XtreamSeriesInfoResponse?> = _seriesInfo.asStateFlow()

    private val _selectedSeasonNumber = MutableStateFlow<Int>(1)
    val selectedSeasonNumber: StateFlow<Int> = _selectedSeasonNumber.asStateFlow()
    private var seriesProfile: PlaylistProfileEntity? = null

    fun loadVod(streamId: String) {
        viewModelScope.launch {
            val item = repository.getVodById(streamId)
            _vod.value = item
            if (item != null && item.imdbRating == null) {
                ratingRepository.enrichVodRating(item.streamId, item.title)
                _vod.value = repository.getVodById(streamId)
            }
        }
    }

    fun loadSeries(seriesId: String) {
        viewModelScope.launch {
            val item = repository.getSeriesById(seriesId)
            _series.value = item
            if (item != null) {
                if (item.imdbRating == null) {
                    ratingRepository.enrichSeriesRating(item.seriesId, item.title)
                    _series.value = repository.getSeriesById(seriesId)
                }
                val activeProfile = repository.getActiveProfileOnce()
                if (activeProfile != null) {
                    seriesProfile = activeProfile
                    val info = repository.getSeriesInfo(activeProfile, seriesId)
                    _seriesInfo.value = info
                    val firstSeason = info?.seasons?.firstOrNull()?.seasonNumber ?: 1
                    _selectedSeasonNumber.value = firstSeason
                }
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        _selectedSeasonNumber.value = seasonNumber
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
