package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val repository: IptvRepository,
    preferences: AppPreferences
) : ViewModel() {

    val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCategory = MutableStateFlow<String?>("Tümü")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val categories: StateFlow<List<String>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getChannelCategories(profile.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channelsPaging: Flow<PagingData<ChannelEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile == null) return@flatMapLatest emptyFlow()
        selectedCategory.flatMapLatest { cat ->
            repository.getChannelsPaged(profile.id, cat)
        }
    }.cachedIn(viewModelScope)

    /** Dakikada bir ilerleyen saat: satırlardaki "şu an" programını ve ilerleme çubuğunu tazeler. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    /**
     * Satırlar bu değer değişince EPG'yi yeniden okur: saat, EPG indirme zamanı veya
     * saat kaydırma ayarı değiştiğinde.
     */
    val epgClock: StateFlow<EpgClock> = combine(
        ticker,
        preferences.epgLastUpdated,
        preferences.epgOffsetHours
    ) { now, updated, offset -> EpgClock(now, updated, offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EpgClock(System.currentTimeMillis(), 0L, 0))

    suspend fun nowPlaying(epgChannelId: String, nowMs: Long): EpgProgramEntity? =
        repository.getNowPlaying(epgChannelId, nowMs)

    fun selectCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleChannelFavorite(channel)
        }
    }

    fun refresh() {
        val profile = activeProfile.value ?: return
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncProfile(profile.id)
            } catch (_: Exception) {
                // Hata durumunda mevcut liste korunur.
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

data class EpgClock(val nowMs: Long, val epgUpdatedAt: Long, val offsetHours: Int)
