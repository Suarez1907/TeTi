package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.player.BufferProfile
import com.teti.iptv.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val profiles: StateFlow<List<PlaylistProfileEntity>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bufferProfile: StateFlow<BufferProfile> = preferences.bufferProfile
    val useSoftwareDecoder: StateFlow<Boolean> = preferences.useSoftwareDecoder
    val omdbApiKey: StateFlow<String> = preferences.omdbApiKey
    val tmdbApiKey: StateFlow<String> = preferences.tmdbApiKey
    val userAgent: StateFlow<String> = preferences.userAgent

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun selectProfile(profileId: Long) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
        }
    }

    fun addOrUpdateProfile(profile: PlaylistProfileEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            if (profile.id == 0L) {
                val newId = repository.addProfile(profile)
                if (profiles.value.isEmpty()) {
                    repository.setActiveProfile(newId)
                }
            } else {
                repository.updateProfile(profile)
            }
            onDone()
        }
    }

    fun deleteProfile(profile: PlaylistProfileEntity) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun syncCurrentProfile() {
        val current = activeProfile.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Senkronizasyon başlatılıyor..."
            try {
                repository.syncProfile(current.id) { status ->
                    _syncStatus.value = status
                }
                _syncStatus.value = "Senkronizasyon tamamlandı!"
            } catch (e: Exception) {
                _syncStatus.value = "Hata: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun setBufferProfile(p: BufferProfile) = preferences.setBufferProfile(p)
    fun setSoftwareDecoder(useSoftware: Boolean) = preferences.setSoftwareDecoder(useSoftware)
    fun setOmdbApiKey(key: String) = preferences.setOmdbApiKey(key)
    fun setTmdbApiKey(key: String) = preferences.setTmdbApiKey(key)
    fun setUserAgent(ua: String) = preferences.setUserAgent(ua)
}
