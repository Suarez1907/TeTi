package com.teti.iptv.ui.navigation

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

data class PlaybackRequest(
    val url: String,
    val title: String,
    val contentId: String,
    val isLive: Boolean
)

/**
 * Navigasyon yoluna ham yayın adresi koymak, `?`, `&`, `%` ve `/` yüzünden
 * rotayı böler veya yanlış ekrana düşürür. Kimlik ve oynatma bilgisi
 * URL-safe Base64 ile tek path parçasına yazılır.
 */
object NavArgs {
    fun encodeId(value: String): String = b64(value.toByteArray(Charsets.UTF_8))

    fun decodeId(value: String): String {
        return try {
            String(b64Decode(value), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            value
        }
    }

    fun encodePlayback(url: String, title: String, contentId: String, isLive: Boolean): String {
        val out = ByteArrayOutputStream()
        writeString(out, url)
        writeString(out, title)
        writeString(out, contentId)
        out.write(if (isLive) 1 else 0)
        return b64(out.toByteArray())
    }

    fun decodePlayback(payload: String): PlaybackRequest? {
        return try {
            val input = ByteArrayInputStream(b64Decode(payload))
            val url = readString(input)
            val title = readString(input)
            val contentId = readString(input)
            val flag = input.read()
            if (flag != 0 && flag != 1) return null
            PlaybackRequest(url, title, contentId, flag == 1)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun writeString(out: ByteArrayOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.size > 0x00FFFFFF) error("Nav arg too large")
        out.write((bytes.size shr 16) and 0xFF)
        out.write((bytes.size shr 8) and 0xFF)
        out.write(bytes.size and 0xFF)
        out.write(bytes)
    }

    private fun readString(input: ByteArrayInputStream): String {
        val b0 = input.read()
        val b1 = input.read()
        val b2 = input.read()
        if (b0 < 0 || b1 < 0 || b2 < 0) error("truncated")
        val size = (b0 shl 16) or (b1 shl 8) or b2
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(bytes, offset, size - offset)
            if (read < 0) error("truncated")
            offset += read
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun b64(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun b64Decode(value: String): ByteArray =
        Base64.getUrlDecoder().decode(value)
}
