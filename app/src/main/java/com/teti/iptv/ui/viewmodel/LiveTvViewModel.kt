package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.repository.IptvRepository
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
class LiveTvViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCategory = MutableStateFlow<String?>("Tümü")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedChannel = MutableStateFlow<ChannelEntity?>(null)
    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannel.asStateFlow()

    val categories: StateFlow<List<String>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getChannelCategories(profile.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channelsPaging: Flow<PagingData<ChannelEntity>> = activeProfile.flatMapLatest { profile ->
        if (profile == null) return@flatMapLatest emptyFlow()
        selectedCategory.flatMapLatest { cat ->
            repository.getChannelsPaged(profile.id, cat)
        }
    }.cachedIn(viewModelScope)

    val currentEpg: StateFlow<EpgProgramEntity?> = _selectedChannel.flatMapLatest { channel ->
        val epgId = channel?.epgChannelId
        if (epgId != null) repository.getCurrentEpg(epgId) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    fun selectChannel(channel: ChannelEntity) {
        _selectedChannel.value = channel
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleChannelFavorite(channel)
        }
    }
}
