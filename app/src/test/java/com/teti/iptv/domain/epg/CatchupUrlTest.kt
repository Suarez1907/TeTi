package com.teti.iptv.domain.epg

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatchupUrlTest {
    private val hour = 3_600_000L
    private val now = 1_800_000_000_000L

    @Test
    fun availableOnlyForFinishedProgramsInsideArchive() {
        assertTrue(CatchupUrl.isAvailable(archiveDays = 2, startMs = now - 3 * hour, endMs = now - 2 * hour, nowMs = now))
        assertFalse(CatchupUrl.isAvailable(archiveDays = 2, startMs = now - hour, endMs = now + hour, nowMs = now))
        assertFalse(CatchupUrl.isAvailable(archiveDays = 1, startMs = now - 30 * hour, endMs = now - 29 * hour, nowMs = now))
        assertFalse(CatchupUrl.isAvailable(archiveDays = 0, startMs = now - 3 * hour, endMs = now - 2 * hour, nowMs = now))
    }
}
