package com.teti.iptv.ui.viewmodel

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import javax.inject.Inject

data class MultiViewSlot(
    val channel: ChannelEntity? = null,
    val error: String? = null
)

/**
 * Çoklu ekran: her karo kendi ExoPlayer'ına sahiptir. Ses yalnızca seçili karoda açıktır;
 * diğerleri sessiz oynar. Düşük gecikmeli tampon kullanılır ki 4 akış belleği şişirmesin.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MultiViewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IptvRepository,
    private val okHttpClient: OkHttpClient,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _slotCount = MutableStateFlow(4)
    val slotCount: StateFlow<Int> = _slotCount.asStateFlow()

    private val _slots = MutableStateFlow(List(MAX_SLOTS) { MultiViewSlot() })
    val slots: StateFlow<List<MultiViewSlot>> = _slots.asStateFlow()

    private val _audioSlot = MutableStateFlow(0)
    val audioSlot: StateFlow<Int> = _audioSlot.asStateFlow()

    private val players = arrayOfNulls<ExoPlayer>(MAX_SLOTS)

    // Kanal seçici
    private val activeProfile: StateFlow<PlaylistProfileEntity?> = repository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _pickerCategory = MutableStateFlow(CatalogSort.CATEGORY_ALL)
    val pickerCategory: StateFlow<String> = _pickerCategory.asStateFlow()
    val pickerCategories: StateFlow<List<String>> = activeProfile.flatMapLatest { p ->
        if (p != null) repository.getChannelCategories(p.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pickerChannels: Flow<PagingData<ChannelEntity>> = combine(activeProfile, _pickerCategory) { p, c -> p to c }
        .flatMapLatest { (p, c) -> if (p == null) emptyFlow() else repository.getChannelsPaged(p.id, c) }
        .cachedIn(viewModelScope)

    suspend fun nowPlaying(epgChannelId: String, nowMs: Long): EpgProgramEntity? = repository.getNowPlaying(epgChannelId, nowMs)

    fun playerFor(slot: Int): ExoPlayer = players[slot] ?: createPlayer(slot).also { players[slot] = it }

    fun setSlotCount(count: Int) {
        val clean = count.coerceIn(2, MAX_SLOTS)
        _slotCount.value = clean
        for (i in clean until MAX_SLOTS) clear(i)
        if (_audioSlot.value >= clean) selectAudio(0)
    }

    fun selectPickerCategory(category: String) {
        _pickerCategory.value = category
    }

    fun assign(slot: Int, channel: ChannelEntity) {
        _slots.update { list -> list.toMutableList().also { it[slot] = MultiViewSlot(channel) } }
        val player = playerFor(slot)
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.playWhenReady = true
        applyVolumes()
    }

    fun clear(slot: Int) {
        players[slot]?.let {
            it.stop()
            it.clearMediaItems()
        }
        _slots.update { list -> list.toMutableList().also { it[slot] = MultiViewSlot() } }
    }

    fun selectAudio(slot: Int) {
        _audioSlot.value = slot
        applyVolumes()
    }

    private fun applyVolumes() {
        players.forEachIndexed { index, p -> p?.volume = if (index == _audioSlot.value) 1f else 0f }
    }

    private fun createPlayer(slot: Int): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_000, 8_000, 1_000, 2_000)
            .build()
        val renderers = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val dataSource = OkHttpDataSource.Factory(okHttpClient).setUserAgent(preferences.userAgent.value)
        return ExoPlayer.Builder(context, renderers)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSource))
            .build()
            .also { player ->
                player.setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    false // Dört oynatıcı birbirinin ses odağını çalmasın.
                )
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        _slots.update { list ->
                            list.toMutableList().also {
                                it[slot] = it[slot].copy(error = "Oynatılamadı (${error.errorCodeName})")
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && _slots.value[slot].error != null) {
                            _slots.update { list -> list.toMutableList().also { it[slot] = it[slot].copy(error = null) } }
                        }
                    }
                })
            }
    }

    override fun onCleared() {
        players.forEach { it?.release() }
        super.onCleared()
    }

    companion object {
        const val MAX_SLOTS = 4
    }
}
