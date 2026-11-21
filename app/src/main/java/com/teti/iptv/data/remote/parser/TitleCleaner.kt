package com.teti.iptv.data.remote.parser

data class CleanedTitle(
    val title: String,
    val year: Int?
)

object TitleCleaner {
    private val PREFIX_REGEX = """^(\[[^\]]*\]|\([^)]*\)|[A-Z]{2,4}\s*[:|\-–—])\s*""".toRegex(RegexOption.IGNORE_CASE)
    private val YEAR_REGEX = """[\(\[\s]((?:19|20)\d{2})[\)\]\s]?""".toRegex()
    private val QUALITY_TAGS_REGEX = """\b(4K|UHD|FHD|HD|SD|1080p|720p|2160p|HEVC|H\.?265|H\.?264|x264|x265|HDR10?\+?|DV|WEB-?DL|BluRay|DUBLAJ|ALTYAZI|DUAL|MULTI|TR|EN|VIP|RAW)\b""".toRegex(RegexOption.IGNORE_CASE)
    private val CLEANUP_CHARS_REGEX = """[\[\]\(\)\{\}\|]""".toRegex()
    private val MULTI_SPACE_REGEX = """\s+""".toRegex()

    fun clean(rawTitle: String): CleanedTitle {
        var text = rawTitle.trim()

        // 1. Strip country/group prefixes like "[TR] ", "(TR) ", "TR - "
        text = text.replace(PREFIX_REGEX, "")

        // 2. Extract year if present
        val yearMatch = YEAR_REGEX.find(text)
        val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()

        // 3. Remove the year using the range found in step 2, before other edits shift the text
        if (yearMatch != null) {
            text = text.removeRange(yearMatch.range)
        }

        // 4. Remove quality and codec tags
        text = text.replace(QUALITY_TAGS_REGEX, " ")

        // 5. Clean punctuation and excess spaces
        text = text.replace(CLEANUP_CHARS_REGEX, " ")
        text = text.replace(MULTI_SPACE_REGEX, " ").trim()

        return CleanedTitle(
            title = text.ifBlank { rawTitle.trim() },
            year = year
        )
    }
}
