package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.repository.IptvRepository
import com.teti.iptv.repository.RatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VodViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val ratingRepository: RatingRepository
) : ViewModel() {

    val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCategory = MutableStateFlow<String?>("Tümü")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<String>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getVodCategories(profile.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topRated: StateFlow<List<VodEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getTopRatedMovies(profile.id, 15) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<VodEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getRecentlyAddedMovies(profile.id, 15) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vodPaging: Flow<PagingData<VodEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile == null) return@flatMapLatest emptyFlow()
        selectedCategory.flatMapLatest { cat ->
            repository.getVodPaged(profile.id, cat)
        }
    }.cachedIn(viewModelScope)

    fun selectCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    fun toggleFavorite(vod: VodEntity) {
        viewModelScope.launch {
            repository.toggleVodFavorite(vod)
        }
    }

    fun fetchRating(vod: VodEntity) {
        viewModelScope.launch {
            ratingRepository.enrichVodRating(vod.streamId, vod.title)
        }
    }
}
