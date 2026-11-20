package com.teti.iptv.data.local

import android.content.Context
import android.content.SharedPreferences
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

    companion object {
        private const val KEY_BUFFER_PROFILE = "buffer_profile"
        private const val KEY_SOFTWARE_DECODER = "software_decoder"
        private const val KEY_OMDB_KEY = "omdb_api_key"
        private const val KEY_TMDB_KEY = "tmdb_api_key"
        private const val KEY_USER_AGENT = "user_agent"
    }
}
