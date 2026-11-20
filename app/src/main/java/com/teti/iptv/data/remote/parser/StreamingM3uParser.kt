package com.teti.iptv.data.remote.parser

import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.VodEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

class StreamingM3uParser {

    companion object {
        private const val BATCH_SIZE = 500
        private val TVG_ID_REGEX = """tvg-id="([^"]*)"""".toRegex()
        private val TVG_LOGO_REGEX = """tvg-logo="([^"]*)"""".toRegex()
        private val GROUP_TITLE_REGEX = """group-title="([^"]*)"""".toRegex()
        private val VOD_EXTENSIONS = listOf(".mp4", ".mkv", ".avi", ".mov")
    }

    suspend fun parse(
        inputStream: InputStream,
        profileId: Long,
        onChannelsBatch: suspend (List<ChannelEntity>) -> Unit,
        onVodBatch: suspend (List<VodEntity>) -> Unit
    ) {
        val channelBatch = ArrayList<ChannelEntity>(BATCH_SIZE)
        val vodBatch = ArrayList<VodEntity>(BATCH_SIZE)

        var currentTitle = ""
        var currentGroup = "Genel"
        var currentLogo: String? = null
        var currentEpgId: String? = null
        var sortIndex = 0

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 64 * 1024).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#EXTINF:")) {
                    currentEpgId = TVG_ID_REGEX.find(trimmed)?.groupValues?.get(1)?.ifBlank { null }
                    currentLogo = TVG_LOGO_REGEX.find(trimmed)?.groupValues?.get(1)?.ifBlank { null }
                    currentGroup = GROUP_TITLE_REGEX.find(trimmed)?.groupValues?.get(1)?.ifBlank { "Genel" } ?: "Genel"
                    currentTitle = trimmed.substringAfterLast(",").trim()
                } else if (!trimmed.startsWith("#")) {
                    val streamUrl = trimmed
                    val isVod = VOD_EXTENSIONS.any { streamUrl.endsWith(it, ignoreCase = true) } ||
                            streamUrl.contains("/movie/", ignoreCase = true)

                    if (isVod) {
                        vodBatch.add(
                            VodEntity(
                                streamId = "${profileId}_vod_${UUID.nameUUIDFromBytes(streamUrl.toByteArray())}",
                                profileId = profileId,
                                title = currentTitle.ifBlank { "İsimsiz Film" },
                                categoryName = currentGroup,
                                streamUrl = streamUrl,
                                posterUrl = currentLogo,
                                imdbRating = null
                            )
                        )
                        if (vodBatch.size >= BATCH_SIZE) {
                            onVodBatch(vodBatch.toList())
                            vodBatch.clear()
                        }
                    } else {
                        channelBatch.add(
                            ChannelEntity(
                                streamId = "${profileId}_ch_${UUID.nameUUIDFromBytes(streamUrl.toByteArray())}",
                                profileId = profileId,
                                name = currentTitle.ifBlank { "İsimsiz Kanal" },
                                categoryName = currentGroup,
                                streamUrl = streamUrl,
                                logoUrl = currentLogo,
                                epgChannelId = currentEpgId,
                                sortOrder = sortIndex++
                            )
                        )
                        if (channelBatch.size >= BATCH_SIZE) {
                            onChannelsBatch(channelBatch.toList())
                            channelBatch.clear()
                        }
                    }
                    currentTitle = ""
                    currentLogo = null
                    currentEpgId = null
                }
            }
        }

        if (channelBatch.isNotEmpty()) onChannelsBatch(channelBatch)
        if (vodBatch.isNotEmpty()) onVodBatch(vodBatch)
    }
}
