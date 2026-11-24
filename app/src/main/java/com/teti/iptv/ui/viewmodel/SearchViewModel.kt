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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Combine the query with the active profile so results update when the profile
    // loads (reading activeProfile.value directly stayed null: nothing subscribed to it).
    private val searchInput: Flow<Pair<String, PlaylistProfileEntity?>> =
        combine(
            _query.debounce(300).distinctUntilChanged(),
            repository.getActiveProfile()
        ) { q, profile -> q.trim() to profile }
            .distinctUntilChanged()

    private fun <T> search(block: (Long, String) -> Flow<List<T>>): Flow<List<T>> =
        searchInput.flatMapLatest { (q, profile) ->
            if (q.length < 2 || profile == null) flowOf(emptyList())
            else block(profile.id, q)
        }

    val channelResults: Flow<List<ChannelEntity>> = search(repository::searchChannels)

    val vodResults: Flow<List<VodEntity>> = search(repository::searchVod)

    val seriesResults: Flow<List<SeriesEntity>> = search(repository::searchSeries)

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }
}
