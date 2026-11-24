package com.teti.iptv.domain.parental

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun verifiesOnlyTheRightPin() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("2468", salt)
        assertTrue(PinHasher.verify("2468", salt, hash))
        assertFalse(PinHasher.verify("2469", salt, hash))
    }

    @Test
    fun sameePinDifferentSaltGivesDifferentHash() {
        assertNotEquals(PinHasher.hash("1234", "a"), PinHasher.hash("1234", "b"))
    }

    @Test
    fun pinMustBeFourToEightDigits() {
        assertTrue(PinHasher.isValidPin("0000"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("12a4"))
    }
}
