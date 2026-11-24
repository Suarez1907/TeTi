package com.teti.iptv.data.local

import android.content.Context
import android.content.SharedPreferences
import com.teti.iptv.BuildConfig
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.domain.epg.EpgOffset
import com.teti.iptv.domain.parental.PinHasher
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

    // --- Altyazı (erişilebilirlik) ---
    /** 1.0 = sistem ayarı; 1.25 ve 1.5 büyütür. */
    private val _subtitleScale = MutableStateFlow(prefs.getFloat(KEY_SUBTITLE_SCALE, 1f))
    val subtitleScale: StateFlow<Float> = _subtitleScale.asStateFlow()

    private val _subtitleBackground = MutableStateFlow(prefs.getBoolean(KEY_SUBTITLE_BG, false))
    val subtitleBackground: StateFlow<Boolean> = _subtitleBackground.asStateFlow()

    fun setSubtitleScale(scale: Float) {
        prefs.edit().putFloat(KEY_SUBTITLE_SCALE, scale).apply()
        _subtitleScale.value = scale
    }

    fun setSubtitleBackground(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUBTITLE_BG, enabled).apply()
        _subtitleBackground.value = enabled
    }

    // --- Ebeveyn kilidi ---
    private val _pinHash = MutableStateFlow(prefs.getString(KEY_PIN_HASH, null))
    private val _lockedCategories = MutableStateFlow(prefs.getStringSet(KEY_LOCKED_CATEGORIES, emptySet())?.toSet() ?: emptySet())
    val lockedCategories: StateFlow<Set<String>> = _lockedCategories.asStateFlow()
    private val _sessionUnlocked = MutableStateFlow(false)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()
    private val _parentalEnabled = MutableStateFlow(_pinHash.value != null)
    val parentalEnabled: StateFlow<Boolean> = _parentalEnabled.asStateFlow()

    /** Listelerden gizlenecek kategoriler: PIN varsa ve oturumda kilit açılmadıysa. */
    private val _hiddenCategories = MutableStateFlow(computeHidden())
    val hiddenCategories: StateFlow<Set<String>> = _hiddenCategories.asStateFlow()

    private fun computeHidden(): Set<String> =
        if (_pinHash.value == null || _sessionUnlocked.value) emptySet() else _lockedCategories.value

    private fun refreshParental() {
        _parentalEnabled.value = _pinHash.value != null
        _hiddenCategories.value = computeHidden()
    }

    fun setParentalPin(pin: String) {
        require(PinHasher.isValidPin(pin)) { "PIN 4-8 haneli olmalı" }
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        prefs.edit().putString(KEY_PIN_SALT, salt).putString(KEY_PIN_HASH, hash).apply()
        _pinHash.value = hash
        _sessionUnlocked.value = false
        refreshParental()
    }

    fun verifyPin(pin: String): Boolean {
        val hash = _pinHash.value ?: return true
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    /** Doğru PIN ile uygulama kapanana kadar kilitli içerikler görünür. */
    fun unlockSession(pin: String): Boolean {
        if (!verifyPin(pin)) return false
        _sessionUnlocked.value = true
        refreshParental()
        return true
    }

    fun lockSession() {
        _sessionUnlocked.value = false
        refreshParental()
    }

    fun clearParentalPin(pin: String): Boolean {
        if (!verifyPin(pin)) return false
        prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply()
        _pinHash.value = null
        refreshParental()
        return true
    }

    /** Yedekten gelen, kataloğun ilk senkronundan sonra uygulanacak favoriler ("c:", "v:", "s:" önekli). */
    fun pendingFavorites(): Set<String> = prefs.getStringSet(KEY_PENDING_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun setPendingFavorites(items: Set<String>) {
        prefs.edit().putStringSet(KEY_PENDING_FAVORITES, items).apply()
    }

    fun setLockedCategories(categories: Set<String>) {
        prefs.edit().putStringSet(KEY_LOCKED_CATEGORIES, categories).apply()
        _lockedCategories.value = categories
        refreshParental()
    }

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
        private const val KEY_PENDING_FAVORITES = "pending_favorites"
        private const val KEY_SUBTITLE_SCALE = "subtitle_scale"
        private const val KEY_SUBTITLE_BG = "subtitle_background"
        private const val KEY_PIN_HASH = "parental_pin_hash"
        private const val KEY_PIN_SALT = "parental_pin_salt"
        private const val KEY_LOCKED_CATEGORIES = "parental_locked_categories"
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
