package com.teti.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teti.iptv.data.local.entity.WatchHistoryEntity
import com.teti.iptv.player.TeTiPlayerManager
import com.teti.iptv.player.TrackInfo
import com.teti.iptv.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: TeTiPlayerManager,
    private val repository: IptvRepository
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val audioTracks: StateFlow<List<TrackInfo>> = playerManager.audioTracks
    val subtitleTracks: StateFlow<List<TrackInfo>> = playerManager.subtitleTracks
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration

    fun getPlayer() = playerManager.getPlayer()

    private var progressTrackerJob: Job? = null
    private var currentContentId: String? = null
    private var currentProfileId: Long = 0L
    private var currentTitle: String = ""
    private var currentPoster: String? = null
    private var isLiveStream: Boolean = false

    fun startPlayback(
        url: String,
        contentId: String,
        profileId: Long,
        title: String,
        poster: String? = null,
        isLive: Boolean = false
    ) {
        currentContentId = contentId
        currentProfileId = profileId
        currentTitle = title
        currentPoster = poster
        isLiveStream = isLive

        viewModelScope.launch {
            val startPos = if (!isLive) repository.getWatchPosition(contentId) else 0L
            playerManager.play(url, startPos)
            startProgressTracking()
        }
    }

    private fun startProgressTracking() {
        progressTrackerJob?.cancel()
        if (isLiveStream) return

        progressTrackerJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val cid = currentContentId ?: continue
                val player = playerManager.getPlayer()
                val pos = player.currentPosition
                val dur = player.duration
                if (dur > 0 && pos > 0) {
                    repository.saveWatchProgress(
                        WatchHistoryEntity(
                            contentId = cid,
                            profileId = currentProfileId,
                            contentType = "VOD",
                            title = currentTitle,
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

    fun seekTo(posMs: Long) = playerManager.seekTo(posMs)
    fun togglePlayPause() = playerManager.togglePlayPause()
    fun selectAudio(track: TrackInfo) = playerManager.selectAudioTrack(track)
    fun selectSubtitle(track: TrackInfo?) = playerManager.selectSubtitleTrack(track)

    override fun onCleared() {
        progressTrackerJob?.cancel()
        playerManager.release()
        super.onCleared()
    }
}
