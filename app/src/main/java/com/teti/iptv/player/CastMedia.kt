package com.teti.iptv.player

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.google.android.gms.cast.MediaQueueItem

/** Chromecast'e gönderilecek adres ve MIME türü. */
object CastMedia {
    private val XTREAM_LIVE_TS = Regex("(/live/[^/]+/[^/]+/[^/?#]+)\\.ts(?=$|[?#])")

    /**
     * Varsayılan Cast alıcısı MPEG-TS oynatamaz. Xtream canlı yayınları aynı adreste
     * HLS olarak da sunar: .../live/u/p/123.ts → .../live/u/p/123.m3u8
     */
    fun castableUrl(url: String): String = url.replace(XTREAM_LIVE_TS, "$1.m3u8")

    fun mimeType(url: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
            path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
            path.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
            path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            path.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
            else -> MimeTypes.VIDEO_MP4
        }
    }
}

/** Oynatılan öğeyi Chromecast'in anlayacağı adres ve MIME türüyle gönderir. */
class IptvCastConverter : MediaItemConverter {
    private val delegate = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val uri = mediaItem.localConfiguration?.uri?.toString() ?: return delegate.toMediaQueueItem(mediaItem)
        val castUrl = CastMedia.castableUrl(uri)
        val converted = mediaItem.buildUpon()
            .setUri(castUrl)
            .setMimeType(CastMedia.mimeType(castUrl))
            .build()
        return delegate.toMediaQueueItem(converted)
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem = delegate.toMediaItem(mediaQueueItem)
}
