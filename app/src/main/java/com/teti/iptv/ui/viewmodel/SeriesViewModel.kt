package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.repository.IptvRepository
import com.teti.iptv.repository.RatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import com.teti.iptv.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesQuery(
    val profile: PlaylistProfileEntity?,
    val category: String?,
    val year: Int?,
    val sort: CatalogSort
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val ratingRepository: RatingRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCategory = MutableStateFlow(CatalogSort.CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _sort = MutableStateFlow(preferences.seriesSort.value)
    val sort: StateFlow<CatalogSort> = _sort.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val categories: StateFlow<List<String>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getSeriesCategories(profile.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val years: StateFlow<List<Int>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getSeriesYears(profile.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topRated: StateFlow<List<SeriesEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getTopRatedSeries(profile.id, 15) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Kaldığın yerden devam et. */
    val continueWatching: StateFlow<List<WatchHistoryEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getContinueWatching(profile.id, WatchHistoryEntity.TYPE_EPISODE) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** TMDB'de bu hafta trend olan ve katalogda bulunanlar (TMDB verisi dolunca yenilenir). */
    val trending: StateFlow<List<SeriesEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile == null) emptyFlow() else flow {
            emit(ratingRepository.trendingSeries(profile.id).filter { it.categoryName !in preferences.hiddenCategories.value })
            // Arka plandaki TMDB eşleştirmesi ilerledikçe raf da dolsun.
            repeat(5) {
                delay(60_000)
                emit(ratingRepository.trendingSeries(profile.id).filter { it.categoryName !in preferences.hiddenCategories.value })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<SeriesEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getRecentlyAddedSeries(profile.id, 15) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesPaging: Flow<PagingData<SeriesEntity>> = combine(
        activeProfile,
        selectedCategory,
        selectedYear,
        sort
    ) { profile, category, year, sortMode ->
        SeriesQuery(profile, category, year, sortMode)
    }.flatMapLatest { query ->
        val profile = query.profile ?: return@flatMapLatest emptyFlow()
        repository.getSeriesPaged(profile.id, query.category, query.year, query.sort)
    }.cachedIn(viewModelScope)

    fun selectCategory(cat: String?) {
        _selectedCategory.value = cat?.ifBlank { CatalogSort.CATEGORY_ALL } ?: CatalogSort.CATEGORY_ALL
    }

    fun selectYear(year: Int?) {
        _selectedYear.value = year
    }

    fun selectSort(sort: CatalogSort) {
        _sort.value = sort
        preferences.setSeriesSort(sort)
    }

    fun refresh() {
        val profile = activeProfile.value ?: return
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncProfile(profile.id)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleFavorite(series: SeriesEntity) {
        viewModelScope.launch {
            repository.toggleSeriesFavorite(series)
        }
    }

    fun fetchRating(series: SeriesEntity) {
        viewModelScope.launch {
            ratingRepository.enrichSeriesRating(series.seriesId, series.title)
        }
    }
}
