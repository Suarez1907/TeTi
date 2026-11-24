package com.teti.iptv.domain.parental

import java.security.MessageDigest
import java.security.SecureRandom

/** Ebeveyn PIN'i düz metin saklanmaz: rastgele tuz + SHA-256 özeti. */
object PinHasher {
    fun isValidPin(pin: String): Boolean = pin.length in 4..8 && pin.all(Char::isDigit)

    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return bytes.toHex()
    }

    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray()).toHex()

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
