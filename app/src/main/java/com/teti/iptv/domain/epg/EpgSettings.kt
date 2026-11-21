package com.teti.iptv.domain.epg

/** EPG otomatik güncelleme sıklığı. */
enum class EpgUpdateInterval(val storageKey: String, val label: String, val hours: Long?) {
    EVERY_12H("12h", "12 Saat", 12),
    EVERY_24H("24h", "24 Saat", 24),
    ON_LAUNCH("launch", "Her Açılışta", null);

    companion object {
        fun fromStorage(raw: String?): EpgUpdateInterval =
            entries.firstOrNull { it.storageKey == raw } ?: EVERY_24H
    }
}

object EpgOffset {
    /** Ayarlarda sunulan saat kaydırma seçenekleri. */
    val OPTIONS = listOf(-2, -1, 0, 1, 2)

    fun label(hours: Int): String = when {
        hours > 0 -> "+${hours}s"
        hours < 0 -> "${hours}s"
        else -> "0"
    }

    fun toMillis(hours: Int): Long = hours * 3_600_000L
}
