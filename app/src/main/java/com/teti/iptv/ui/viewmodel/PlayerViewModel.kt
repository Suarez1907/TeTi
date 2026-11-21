package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.WatchHistoryEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.player.TeTiPlayerManager
import com.teti.iptv.player.TrackInfo
import com.teti.iptv.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Oynatılan içerik. Canlı TV'de çekmeceden kanal değiştirilince güncellenir. */
data class PlaybackItem(
    val url: String,
    val title: String,
    val contentId: String,
    val isLive: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: TeTiPlayerManager,
    private val repository: IptvRepository,
    preferences: AppPreferences
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val audioTracks: StateFlow<List<TrackInfo>> = playerManager.audioTracks
    val subtitleTracks: StateFlow<List<TrackInfo>> = playerManager.subtitleTracks
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val playbackError: StateFlow<String?> = playerManager.playbackError

    fun getPlayer() = playerManager.getPlayer()

    private val _current = MutableStateFlow<PlaybackItem?>(null)
    val current: StateFlow<PlaybackItem?> = _current.asStateFlow()

    private var progressTrackerJob: Job? = null
    private var uiPositionTrackerJob: Job? = null
    private var currentProfileId: Long = 0L
    private var currentPoster: String? = null

    // --- Hızlı kanal çekmecesi ---
    private val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _drawerCategory = MutableStateFlow(CatalogSort.CATEGORY_ALL)
    val drawerCategory: StateFlow<String> = _drawerCategory.asStateFlow()

    val drawerCategories: StateFlow<List<String>> = activeProfile.flatMapLatest { profile ->
        if (profile != null) repository.getChannelCategories(profile.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drawerChannels: Flow<PagingData<ChannelEntity>> = combine(activeProfile, _drawerCategory) { p, c -> p to c }
        .flatMapLatest { (profile, category) ->
            if (profile == null) emptyFlow() else repository.getChannelsPaged(profile.id, category)
        }
        .cachedIn(viewModelScope)

    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    val epgClock: StateFlow<EpgClock> = combine(
        ticker,
        preferences.epgLastUpdated,
        preferences.epgOffsetHours
    ) { now, updated, offset -> EpgClock(now, updated, offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EpgClock(System.currentTimeMillis(), 0L, 0))

    /** Canlı yayındaki kanalın şu anki programı (üst çubukta gösterilir). */
    val currentProgram: StateFlow<EpgProgramEntity?> = combine(_current, epgClock) { item, clock -> item to clock }
        .flatMapLatest { (item, clock) ->
            flow {
                if (item == null || !item.isLive) {
                    emit(null)
                } else {
                    val epgId = repository.getChannelById(item.contentId)?.epgChannelId
                    emit(epgId?.let { repository.getNowPlaying(it, clock.nowMs) })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun nowPlaying(epgChannelId: String, nowMs: Long): EpgProgramEntity? =
        repository.getNowPlaying(epgChannelId, nowMs)

    fun selectDrawerCategory(category: String) {
        _drawerCategory.value = category
    }

    /**
     * Ekran ilk açıldığında çağrılır. Döndürme sonrası yeniden çağrılsa da çalan
     * içerik (zapping ile değişmiş olabilir) baştan başlatılmaz.
     */
    fun open(item: PlaybackItem) {
        if (_current.value != null) return
        play(item)
    }

    /** Çekmeceden kanal değiştir: oynatıcı kapanmadan yeni akışa geçer. */
    fun zapTo(channel: ChannelEntity) {
        if (_current.value?.contentId == channel.streamId) return
        play(PlaybackItem(channel.streamUrl, channel.name, channel.streamId, isLive = true))
    }

    private fun play(item: PlaybackItem) {
        _current.value = item
        viewModelScope.launch {
            currentProfileId = repository.getActiveProfileOnce()?.id ?: 0L
            val startPos = if (!item.isLive) repository.getWatchPosition(item.contentId) else 0L
            playerManager.play(item.url, startPos)
            startProgressTracking(item)
            startUiPositionTracking()
        }
    }

    private fun startUiPositionTracking() {
        uiPositionTrackerJob?.cancel()
        uiPositionTrackerJob = viewModelScope.launch {
            while (isActive) {
                playerManager.updatePosition()
                delay(500)
            }
        }
    }

    private fun startProgressTracking(item: PlaybackItem) {
        progressTrackerJob?.cancel()
        if (item.isLive) return

        progressTrackerJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val player = playerManager.getPlayer()
                val pos = player.currentPosition
                val dur = player.duration
                if (dur > 0 && pos > 0) {
                    repository.saveWatchProgress(
                        WatchHistoryEntity(
                            contentId = item.contentId,
                            profileId = currentProfileId,
                            contentType = "VOD",
                            title = item.title,
                            posterUrl = currentPoster,
                            positionMs = pos,
                            durationMs = dur,
                            lastWatchedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun seekBy(deltaMs: Long) {
        val player = playerManager.getPlayer()
        val upper = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        playerManager.seekTo((player.currentPosition + deltaMs).coerceIn(0L, upper))
    }

    fun seekTo(posMs: Long) = playerManager.seekTo(posMs)
    fun togglePlayPause() = playerManager.togglePlayPause()
    fun selectAudio(track: TrackInfo) = playerManager.selectAudioTrack(track)
    fun selectSubtitle(track: TrackInfo?) = playerManager.selectSubtitleTrack(track)
    fun retry() = playerManager.retry()

    override fun onCleared() {
        progressTrackerJob?.cancel()
        uiPositionTrackerJob?.cancel()
        playerManager.release()
        super.onCleared()
    }
}
