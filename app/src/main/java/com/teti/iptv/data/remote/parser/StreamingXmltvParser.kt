package com.teti.iptv.data.remote.parser

import android.util.Xml
import com.teti.iptv.data.local.entity.EpgProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.PushbackInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

class StreamingXmltvParser {

    companion object {
        private const val BATCH_SIZE = 500
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
        private val SIMPLE_DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    }

    suspend fun parse(
        rawInputStream: InputStream,
        onBatchReady: suspend (List<EpgProgramEntity>) -> Unit
    ) {
        val stream = checkGzip(rawInputStream)
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, "UTF-8")

        val batch = ArrayList<EpgProgramEntity>(BATCH_SIZE)
        var eventType = parser.eventType

        var currentChannel: String? = null
        var currentStart: Long = 0L
        var currentEnd: Long = 0L
        var currentTitle = ""
        var currentDesc: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            currentChannel = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            currentStart = parseTime(startStr)
                            currentEnd = parseTime(stopStr)
                            currentTitle = ""
                            currentDesc = null
                        }
                        "title" -> {
                            currentTitle = parser.nextText().trim()
                        }
                        "desc" -> {
                            currentDesc = parser.nextText().trim().ifBlank { null }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && !currentChannel.isNullOrBlank() && currentTitle.isNotBlank()) {
                        batch.add(
                            EpgProgramEntity(
                                epgChannelId = currentChannel,
                                title = currentTitle,
                                description = currentDesc,
                                startTimeMs = currentStart,
                                endTimeMs = currentEnd
                            )
                        )
                        if (batch.size >= BATCH_SIZE) {
                            onBatchReady(batch.toList())
                            batch.clear()
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (batch.isNotEmpty()) {
            onBatchReady(batch)
        }
    }

    private fun checkGzip(input: InputStream): InputStream {
        val pushback = PushbackInputStream(input, 2)
        val header = ByteArray(2)
        val read = pushback.read(header, 0, 2)
        if (read == 2) {
            pushback.unread(header, 0, 2)
            val magic = (header[0].toInt() and 0xff) or ((header[1].toInt() and 0xff) shl 8)
            if (magic == GZIPInputStream.GZIP_MAGIC) {
                return GZIPInputStream(pushback)
            }
        } else if (read > 0) {
            pushback.unread(header, 0, read)
        }
        return pushback
    }

    @Synchronized
    private fun parseTime(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        val clean = dateStr.trim()
        return runCatching {
            DATE_FORMAT.parse(clean)?.time ?: 0L
        }.recoverCatching {
            SIMPLE_DATE_FORMAT.parse(clean.take(14))?.time ?: 0L
        }.getOrDefault(0L)
    }
}
