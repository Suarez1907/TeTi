package com.teti.iptv.data.local

import android.content.Context
import android.content.SharedPreferences
import com.teti.iptv.BuildConfig
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.domain.epg.EpgOffset
import com.teti.iptv.domain.epg.EpgUpdateInterval
import com.teti.iptv.player.BufferProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("teti_preferences", Context.MODE_PRIVATE)

    private val _bufferProfile = MutableStateFlow(
        runCatching {
            BufferProfile.valueOf(prefs.getString(KEY_BUFFER_PROFILE, BufferProfile.BALANCED.name)!!)
        }.getOrDefault(BufferProfile.BALANCED)
    )
    val bufferProfile: StateFlow<BufferProfile> = _bufferProfile.asStateFlow()

    private val _useSoftwareDecoder = MutableStateFlow(
        prefs.getBoolean(KEY_SOFTWARE_DECODER, false)
    )
    val useSoftwareDecoder: StateFlow<Boolean> = _useSoftwareDecoder.asStateFlow()

    private val _omdbApiKey = MutableStateFlow(prefs.getString(KEY_OMDB_KEY, "") ?: "")
    val omdbApiKey: StateFlow<String> = _omdbApiKey.asStateFlow()

    private val _tmdbApiKey = MutableStateFlow(prefs.getString(KEY_TMDB_KEY, "") ?: "")
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey.asStateFlow()

    private val _userAgent = MutableStateFlow(prefs.getString(KEY_USER_AGENT, "TeTi-IPTV/1.0") ?: "TeTi-IPTV/1.0")
    val userAgent: StateFlow<String> = _userAgent.asStateFlow()

    private val _vodSort = MutableStateFlow(CatalogSort.fromStorage(prefs.getString(KEY_VOD_SORT, null)))
    val vodSort: StateFlow<CatalogSort> = _vodSort.asStateFlow()

    private val _seriesSort = MutableStateFlow(CatalogSort.fromStorage(prefs.getString(KEY_SERIES_SORT, null)))
    val seriesSort: StateFlow<CatalogSort> = _seriesSort.asStateFlow()

    private val _epgAutoXtream = MutableStateFlow(prefs.getBoolean(KEY_EPG_AUTO_XTREAM, true))
    val epgAutoXtream: StateFlow<Boolean> = _epgAutoXtream.asStateFlow()

    private val _epgCustomUrl = MutableStateFlow(prefs.getString(KEY_EPG_CUSTOM_URL, "") ?: "")
    val epgCustomUrl: StateFlow<String> = _epgCustomUrl.asStateFlow()

    private val _epgInterval = MutableStateFlow(EpgUpdateInterval.fromStorage(prefs.getString(KEY_EPG_INTERVAL, null)))
    val epgInterval: StateFlow<EpgUpdateInterval> = _epgInterval.asStateFlow()

    private val _epgOffsetHours = MutableStateFlow(
        prefs.getInt(KEY_EPG_OFFSET, 0).takeIf { it in EpgOffset.OPTIONS } ?: 0
    )
    val epgOffsetHours: StateFlow<Int> = _epgOffsetHours.asStateFlow()

    private val _epgLastUpdated = MutableStateFlow(prefs.getLong(KEY_EPG_LAST_UPDATED, 0L))
    val epgLastUpdated: StateFlow<Long> = _epgLastUpdated.asStateFlow()

    /**
     * Her TMDB isteğinde okunur: kullanıcı Ayarlar'da anahtar girdiyse o, yoksa yerleşik anahtar.
     * "eyJ" ile başlayan değer v4 okuma jetonudur (Bearer); diğerleri v3 api_key.
     */
    fun tmdbCredential(): TmdbCredential {
        val user = _tmdbApiKey.value.trim()
        if (user.isNotEmpty()) return TmdbCredential.of(user)
        if (BuildConfig.TMDB_READ_TOKEN.isNotBlank()) return TmdbCredential.Bearer(BuildConfig.TMDB_READ_TOKEN)
        return TmdbCredential.of(BuildConfig.TMDB_API_KEY)
    }

    fun setBufferProfile(profile: BufferProfile) {
        prefs.edit().putString(KEY_BUFFER_PROFILE, profile.name).apply()
        _bufferProfile.value = profile
    }

    fun setSoftwareDecoder(useSoftware: Boolean) {
        prefs.edit().putBoolean(KEY_SOFTWARE_DECODER, useSoftware).apply()
        _useSoftwareDecoder.value = useSoftware
    }

    fun setOmdbApiKey(key: String) {
        prefs.edit().putString(KEY_OMDB_KEY, key.trim()).apply()
        _omdbApiKey.value = key.trim()
    }

    fun setTmdbApiKey(key: String) {
        prefs.edit().putString(KEY_TMDB_KEY, key.trim()).apply()
        _tmdbApiKey.value = key.trim()
    }

    fun setUserAgent(ua: String) {
        val clean = ua.ifBlank { "TeTi-IPTV/1.0" }
        prefs.edit().putString(KEY_USER_AGENT, clean).apply()
        _userAgent.value = clean
    }

    fun setVodSort(sort: CatalogSort) {
        prefs.edit().putString(KEY_VOD_SORT, sort.storageKey).apply()
        _vodSort.value = sort
    }

    fun setSeriesSort(sort: CatalogSort) {
        prefs.edit().putString(KEY_SERIES_SORT, sort.storageKey).apply()
        _seriesSort.value = sort
    }

    fun setEpgAutoXtream(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EPG_AUTO_XTREAM, enabled).apply()
        _epgAutoXtream.value = enabled
    }

    fun setEpgCustomUrl(url: String) {
        prefs.edit().putString(KEY_EPG_CUSTOM_URL, url.trim()).apply()
        _epgCustomUrl.value = url.trim()
    }

    fun setEpgInterval(interval: EpgUpdateInterval) {
        prefs.edit().putString(KEY_EPG_INTERVAL, interval.storageKey).apply()
        _epgInterval.value = interval
    }

    fun setEpgOffsetHours(hours: Int) {
        val clean = hours.takeIf { it in EpgOffset.OPTIONS } ?: 0
        prefs.edit().putInt(KEY_EPG_OFFSET, clean).apply()
        _epgOffsetHours.value = clean
    }

    fun setEpgLastUpdated(timeMs: Long) {
        prefs.edit().putLong(KEY_EPG_LAST_UPDATED, timeMs).apply()
        _epgLastUpdated.value = timeMs
    }

    companion object {
        private const val KEY_EPG_AUTO_XTREAM = "epg_auto_xtream"
        private const val KEY_EPG_CUSTOM_URL = "epg_custom_url"
        private const val KEY_EPG_INTERVAL = "epg_interval"
        private const val KEY_EPG_OFFSET = "epg_offset_hours"
        private const val KEY_EPG_LAST_UPDATED = "epg_last_updated"

        private const val KEY_BUFFER_PROFILE = "buffer_profile"
        private const val KEY_SOFTWARE_DECODER = "software_decoder"
        private const val KEY_OMDB_KEY = "omdb_api_key"
        private const val KEY_TMDB_KEY = "tmdb_api_key"
        private const val KEY_USER_AGENT = "user_agent"
        private const val KEY_VOD_SORT = "vod_sort"
        private const val KEY_SERIES_SORT = "series_sort"
    }
}

sealed interface TmdbCredential {
    data class ApiKey(val key: String) : TmdbCredential
    data class Bearer(val token: String) : TmdbCredential

    val isBlank: Boolean
        get() = when (this) {
            is ApiKey -> key.isBlank()
            is Bearer -> token.isBlank()
        }

    companion object {
        fun of(value: String): TmdbCredential =
            if (value.startsWith("eyJ")) Bearer(value) else ApiKey(value)
    }
}
