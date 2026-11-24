package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/** Izgara TV rehberi: gün seçimi, kategori ve kanal başına günlük program listesi. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GuideViewModel @Inject constructor(
    private val repository: IptvRepository,
    preferences: AppPreferences
) : ViewModel() {

    private val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _category = MutableStateFlow(CatalogSort.CATEGORY_ALL)
    val category: StateFlow<String> = _category.asStateFlow()

    /** Bugüne göre gün farkı: −3 (arşiv) … +7. */
    private val _dayOffset = MutableStateFlow(0)
    val dayOffset: StateFlow<Int> = _dayOffset.asStateFlow()

    val categories: StateFlow<List<String>> = activeProfile.flatMapLatest { p ->
        if (p != null) repository.getChannelCategories(p.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: Flow<PagingData<ChannelEntity>> = combine(activeProfile, _category) { p, c -> p to c }
        .flatMapLatest { (p, c) -> if (p == null) emptyFlow() else repository.getChannelsPaged(p.id, c) }
        .cachedIn(viewModelScope)

    val epgVersion: StateFlow<Pair<Long, Int>> = combine(preferences.epgLastUpdated, preferences.epgOffsetHours) { a, b -> a to b }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L to 0)

    fun dayStart(offset: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, offset)
    }.timeInMillis

    fun programs(epgChannelId: String, dayStartMs: Long): Flow<List<EpgProgramEntity>> =
        repository.getProgramsForChannel(epgChannelId, dayStartMs, dayStartMs + DAY_MS)

    suspend fun catchupUrl(channel: ChannelEntity, program: EpgProgramEntity): String? =
        repository.buildCatchupUrl(channel, program)

    fun selectCategory(category: String) {
        _category.value = category
    }

    fun selectDay(offset: Int) {
        _dayOffset.value = offset.coerceIn(MIN_DAY, MAX_DAY)
    }

    companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val MIN_DAY = -3
        const val MAX_DAY = 7
    }
}
