package com.teti.iptv.data.remote.parser

import android.util.JsonReader
import android.util.JsonToken
import android.net.Uri
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.VodEntity
import java.io.InputStream
import java.io.InputStreamReader

class StreamingXtreamParser {

    companion object {
        private const val BATCH_SIZE = 500
    }

    suspend fun parseLiveStreams(
        inputStream: InputStream,
        profileId: Long,
        serverUrl: String,
        username: String,
        password: String,
        categoryMap: Map<String, String>,
        onBatchReady: suspend (List<ChannelEntity>) -> Unit
    ) {
        val batch = ArrayList<ChannelEntity>(BATCH_SIZE)
        val cleanHost = serverUrl.trimEnd('/')
        val encodedUsername = Uri.encode(username)
        val encodedPassword = Uri.encode(password)
        var sortIndex = 0

        JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.isLenient = true
            check(reader.peek() == JsonToken.BEGIN_ARRAY) { "Xtream canlı kanal yanıtı dizi değil" }

            reader.beginArray()
            while (reader.hasNext()) {
                var streamId = ""
                var name = ""
                var categoryId = ""
                var logoUrl: String? = null
                var epgChannelId: String? = null
                var tvArchive = false
                var archiveDays = 0

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "stream_id" -> streamId = readSafeString(reader)
                        "name" -> name = readSafeString(reader)
                        "category_id" -> categoryId = readSafeString(reader)
                        "stream_icon" -> logoUrl = readSafeString(reader).ifBlank { null }
                        "epg_channel_id" -> epgChannelId = readSafeString(reader).ifBlank { null }
                        "tv_archive" -> tvArchive = readSafeString(reader).trim() in setOf("1", "true")
                        "tv_archive_duration" -> archiveDays = readSafeString(reader).trim().toIntOrNull() ?: 0
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (streamId.isNotBlank()) {
                    batch.add(
                        ChannelEntity(
                            streamId = "${profileId}_$streamId",
                            profileId = profileId,
                            name = name.ifBlank { "Kanal $streamId" },
                            categoryName = categoryMap[categoryId] ?: "Canlı",
                            streamUrl = "$cleanHost/live/$encodedUsername/$encodedPassword/$streamId.ts",
                            logoUrl = logoUrl,
                            epgChannelId = epgChannelId,
                            sortOrder = sortIndex++,
                            tvArchive = tvArchive && archiveDays > 0,
                            archiveDays = archiveDays
                        )
                    )
                    if (batch.size >= BATCH_SIZE) {
                        onBatchReady(batch.toList())
                        batch.clear()
                    }
                }
            }
            reader.endArray()
        }

        if (batch.isNotEmpty()) onBatchReady(batch)
    }

    suspend fun parseVodStreams(
        inputStream: InputStream,
        profileId: Long,
        serverUrl: String,
        username: String,
        password: String,
        categoryMap: Map<String, String>,
        onBatchReady: suspend (List<VodEntity>) -> Unit
    ) {
        val batch = ArrayList<VodEntity>(BATCH_SIZE)
        val cleanHost = serverUrl.trimEnd('/')
        val encodedUsername = Uri.encode(username)
        val encodedPassword = Uri.encode(password)

        JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.isLenient = true
            check(reader.peek() == JsonToken.BEGIN_ARRAY) { "Xtream film yanıtı dizi değil" }

            reader.beginArray()
            while (reader.hasNext()) {
                var streamId = ""
                var name = ""
                var categoryId = ""
                var posterUrl: String? = null
                var ext = "mp4"
                var added = 0L

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "stream_id" -> streamId = readSafeString(reader)
                        "name" -> name = readSafeString(reader)
                        "category_id" -> categoryId = readSafeString(reader)
                        "stream_icon" -> posterUrl = readSafeString(reader).ifBlank { null }
                        "container_extension" -> ext = readSafeString(reader).ifBlank { "mp4" }
                        "added" -> added = readSafeString(reader).toLongOrNull() ?: 0L
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (streamId.isNotBlank()) {
                    batch.add(
                        VodEntity(
                            streamId = "${profileId}_$streamId",
                            profileId = profileId,
                            title = name,
                            categoryName = categoryMap[categoryId] ?: "Diğer",
                            streamUrl = "$cleanHost/movie/$encodedUsername/$encodedPassword/$streamId.$ext",
                            posterUrl = posterUrl,
                            imdbRating = null, // sağlayıcı puanı yok sayılır; TMDB doldurur
                            containerExtension = ext,
                            addedTimestamp = added
                        )
                    )
                    if (batch.size >= BATCH_SIZE) {
                        onBatchReady(batch.toList())
                        batch.clear()
                    }
                }
            }
            reader.endArray()
        }

        if (batch.isNotEmpty()) onBatchReady(batch)
    }

    suspend fun parseSeriesStreams(
        inputStream: InputStream,
        profileId: Long,
        categoryMap: Map<String, String>,
        onBatchReady: suspend (List<SeriesEntity>) -> Unit
    ) {
        val batch = ArrayList<SeriesEntity>(BATCH_SIZE)

        JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.isLenient = true
            check(reader.peek() == JsonToken.BEGIN_ARRAY) { "Xtream dizi yanıtı dizi değil" }

            reader.beginArray()
            while (reader.hasNext()) {
                var seriesId = ""
                var name = ""
                var categoryId = ""
                var posterUrl: String? = null
                var backdropUrl: String? = null
                var plot: String? = null
                var releaseYear: Int? = null
                var lastModified = 0L

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "series_id" -> seriesId = readSafeString(reader)
                        "name" -> name = readSafeString(reader)
                        "category_id" -> categoryId = readSafeString(reader)
                        "cover" -> posterUrl = readSafeString(reader).ifBlank { null }
                        "backdrop_path" -> {
                            val arr = readSafeString(reader).ifBlank { null }
                            backdropUrl = arr
                        }
                        "plot" -> plot = readSafeString(reader).ifBlank { null }
                        "releaseDate" -> {
                            val date = readSafeString(reader)
                            releaseYear = date.take(4).toIntOrNull()
                        }
                        "last_modified" -> lastModified = readSafeString(reader).toLongOrNull() ?: 0L
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (seriesId.isNotBlank()) {
                    batch.add(
                        SeriesEntity(
                            seriesId = "${profileId}_$seriesId",
                            profileId = profileId,
                            title = name,
                            categoryName = categoryMap[categoryId] ?: "Diziler",
                            posterUrl = posterUrl,
                            backdropUrl = backdropUrl,
                            plot = plot,
                            imdbRating = null, // sağlayıcı puanı yok sayılır; TMDB doldurur
                            releaseYear = releaseYear,
                            lastModified = lastModified
                        )
                    )
                    if (batch.size >= BATCH_SIZE) {
                        onBatchReady(batch.toList())
                        batch.clear()
                    }
                }
            }
            reader.endArray()
        }

        if (batch.isNotEmpty()) onBatchReady(batch)
    }

    private fun readSafeString(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                ""
            }
            JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            else -> {
                reader.skipValue()
                ""
            }
        }
    }
}
