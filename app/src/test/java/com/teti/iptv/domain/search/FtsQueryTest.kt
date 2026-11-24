package com.teti.iptv.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryTest {
    @Test
    fun eachWordBecomesPrefixTerm() {
        assertEquals("kara* sevda*", FtsQuery.build("Kara Sevda"))
    }

    @Test
    fun stripsFtsOperatorsAndQuotes() {
        assertEquals("tr* spor* or*", FtsQuery.build("\"TR\" - spor* OR"))
    }

    @Test
    fun emptyOrSymbolsOnlyReturnsNull() {
        assertNull(FtsQuery.build("  -*\" "))
    }
}
