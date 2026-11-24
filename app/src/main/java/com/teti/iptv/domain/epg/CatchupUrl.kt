package com.teti.iptv.domain.epg

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Xtream Codes arşiv (timeshift) adresi ve bir programın arşivde olup olmadığı. */
object CatchupUrl {
    /**
     * Xtream biçimi: {sunucu}/timeshift/{kullanıcı}/{şifre}/{süre-dk}/{yyyy-MM-dd:HH-mm}/{akış}.ts
     * [startMs] XMLTV'deki ham başlangıç zamanıdır (kullanıcının saat kaydırması uygulanmamış).
     */
    fun build(
        serverUrl: String,
        username: String,
        password: String,
        rawStreamId: String,
        startMs: Long,
        endMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val minutes = ((endMs - startMs) / 60_000L).coerceAtLeast(1L)
        val start = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply { this.timeZone = timeZone }
            .format(Date(startMs))
        return "${serverUrl.trimEnd('/')}/timeshift/${encode(username)}/${encode(password)}/$minutes/$start/$rawStreamId.ts"
    }

    /** Program bitmiş ve arşiv süresi içinde mi? */
    fun isAvailable(archiveDays: Int, startMs: Long, endMs: Long, nowMs: Long): Boolean {
        if (archiveDays <= 0 || endMs > nowMs) return false
        return startMs >= nowMs - archiveDays * 24L * 60 * 60 * 1000
    }

    private fun encode(value: String): String = Uri.encode(value)
}
