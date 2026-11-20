package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val debouncedQuery = _query
        .debounce(300)
        .distinctUntilChanged()

    val channelResults: Flow<List<ChannelEntity>> = debouncedQuery.flatMapLatest { q ->
        val profile = activeProfile.value
        if (q.length < 2 || profile == null) emptyFlow()
        else repository.searchChannels(profile.id, q)
    }

    val vodResults: Flow<List<VodEntity>> = debouncedQuery.flatMapLatest { q ->
        val profile = activeProfile.value
        if (q.length < 2 || profile == null) emptyFlow()
        else repository.searchVod(profile.id, q)
    }

    val seriesResults: Flow<List<SeriesEntity>> = debouncedQuery.flatMapLatest { q ->
        val profile = activeProfile.value
        if (q.length < 2 || profile == null) emptyFlow()
        else repository.searchSeries(profile.id, q)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }
}
