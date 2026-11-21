package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.domain.epg.EpgUpdateInterval
import com.teti.iptv.player.BufferProfile
import com.teti.iptv.repository.IptvRepository
import com.teti.iptv.repository.RatingRepository
import com.teti.iptv.worker.BackgroundScheduler
import com.teti.iptv.worker.TmdbProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val ratingRepository: RatingRepository,
    private val preferences: AppPreferences,
    private val scheduler: BackgroundScheduler
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

    val epgAutoXtream: StateFlow<Boolean> = preferences.epgAutoXtream
    val epgCustomUrl: StateFlow<String> = preferences.epgCustomUrl
    val epgInterval: StateFlow<EpgUpdateInterval> = preferences.epgInterval
    val epgOffsetHours: StateFlow<Int> = preferences.epgOffsetHours
    val epgLastUpdated: StateFlow<Long> = preferences.epgLastUpdated

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _epgStatus = MutableStateFlow<String?>(null)
    val epgStatus: StateFlow<String?> = _epgStatus.asStateFlow()

    private val _isEpgSyncing = MutableStateFlow(false)
    val isEpgSyncing: StateFlow<Boolean> = _isEpgSyncing.asStateFlow()

    val tmdbProgress: StateFlow<TmdbProgress?> = scheduler.tmdbProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tmdbRunning: StateFlow<Boolean> = tmdbProgress
        .map { it?.state == WorkInfo.State.RUNNING || it?.state == WorkInfo.State.ENQUEUED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    // --- EPG ---
    fun syncEpgNow() {
        if (_isEpgSyncing.value) return
        viewModelScope.launch {
            _isEpgSyncing.value = true
            _epgStatus.value = "EPG indiriliyor…"
            try {
                val done = repository.syncEpgForActiveProfile()
                _epgStatus.value = if (done) "EPG güncellendi." else
                    "EPG kaynağı yok: Xtream otomatik çekmeyi açın veya özel XMLTV URL girin."
            } catch (e: Exception) {
                _epgStatus.value = "EPG alınamadı: ${e.message}"
            } finally {
                _isEpgSyncing.value = false
            }
        }
    }

    fun setEpgAutoXtream(enabled: Boolean) = preferences.setEpgAutoXtream(enabled)
    fun setEpgCustomUrl(url: String) = preferences.setEpgCustomUrl(url)
    fun setEpgOffsetHours(hours: Int) = preferences.setEpgOffsetHours(hours)
    fun setEpgInterval(interval: EpgUpdateInterval) {
        preferences.setEpgInterval(interval)
        scheduler.applyEpgSchedule(interval)
    }

    // --- TMDB ---
    /** Önbelleği temizler ve tüm katalog için TMDB verisini baştan çeker. */
    fun refreshTmdbCache() {
        viewModelScope.launch {
            ratingRepository.clearCache()
            scheduler.enqueueTmdbEnrich(restart = true)
        }
    }

    fun setBufferProfile(p: BufferProfile) = preferences.setBufferProfile(p)
    fun setSoftwareDecoder(useSoftware: Boolean) = preferences.setSoftwareDecoder(useSoftware)
    fun setOmdbApiKey(key: String) = preferences.setOmdbApiKey(key)
    fun setTmdbApiKey(key: String) = preferences.setTmdbApiKey(key)
    fun setUserAgent(ua: String) = preferences.setUserAgent(ua)
}
