package com.smartfoo.android.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FooCryptoTest {

    // SHA256 — deterministic golden value

    @Test fun sha256_emptyInput() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val expected = byteArrayOf(
            0xe3.toByte(), 0xb0.toByte(), 0xc4.toByte(), 0x42.toByte(),
            0x98.toByte(), 0xfc.toByte(), 0x1c.toByte(), 0x14.toByte(),
            0x9a.toByte(), 0xfb.toByte(), 0xf4.toByte(), 0xc8.toByte(),
            0x99.toByte(), 0x6f.toByte(), 0xb9.toByte(), 0x24.toByte(),
            0x27.toByte(), 0xae.toByte(), 0x41.toByte(), 0xe4.toByte(),
            0x64.toByte(), 0x9b.toByte(), 0x93.toByte(), 0x4c.toByte(),
            0xa4.toByte(), 0x95.toByte(), 0x99.toByte(), 0x1b.toByte(),
            0x78.toByte(), 0x52.toByte(), 0xb8.toByte(), 0x55.toByte()
        )
        assertArrayEquals(expected, FooCrypto.SHA256(byteArrayOf()))
    }

    @Test fun sha256_knownInput() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469fa72a4000f7f5572
        val input = "abc".toByteArray(Charsets.UTF_8)
        val result = FooCrypto.SHA256(input)
        assertEquals(32, result.size)
        assertEquals(0xba.toByte(), result[0])
        assertEquals(0x78.toByte(), result[1])
        assertEquals(0x16.toByte(), result[2])
    }

    @Test fun sha256_isStable() {
        val input = "hello".toByteArray()
        assertArrayEquals(FooCrypto.SHA256(input), FooCrypto.SHA256(input))
    }

    // HMACSHA256 — golden value for known key + data

    @Test fun hmacSha256_knownKeyAndData() {
        // HMAC-SHA256(key="key", data="The quick brown fox jumps over the lazy dog")
        // = f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8
        val key = "key".toByteArray(Charsets.UTF_8)
        val data = "The quick brown fox jumps over the lazy dog".toByteArray(Charsets.UTF_8)
        val result = FooCrypto.HMACSHA256(key, data)
        assertEquals(32, result.size)
        assertEquals(0xf7.toByte(), result[0])
        assertEquals(0xbc.toByte(), result[1])
        assertEquals(0x83.toByte(), result[2])
        assertEquals(0xf4.toByte(), result[3])
    }

    @Test fun hmacSha256_isStable() {
        val key = byteArrayOf(1, 2, 3)
        val data = byteArrayOf(4, 5, 6)
        assertArrayEquals(FooCrypto.HMACSHA256(key, data), FooCrypto.HMACSHA256(key, data))
    }

    @Test fun hmacSha256_offsetAndLength() {
        val key = byteArrayOf(1, 2, 3)
        val data = byteArrayOf(0, 4, 5, 6, 0)
        val full = FooCrypto.HMACSHA256(key, byteArrayOf(4, 5, 6))
        val partial = FooCrypto.HMACSHA256(key, data, 1, 3)
        assertArrayEquals(full, partial)
    }

    // getRandomBytes

    @Test fun getRandomBytes_correctLength() {
        assertEquals(16, FooCrypto.getRandomBytes(16).size)
        assertEquals(0, FooCrypto.getRandomBytes(0).size)
        assertEquals(32, FooCrypto.getRandomBytes(32).size)
    }

    @Test fun getRandomBytes_differentEachCall() {
        val a = FooCrypto.getRandomBytes(16)
        val b = FooCrypto.getRandomBytes(16)
        assertFalse("two random byte arrays should differ (probabilistic)", a.contentEquals(b))
    }

    // randomInt32 / randomInt64

    @Test fun randomInt32_notAlwaysSame() {
        val a = FooCrypto.randomInt32
        val b = FooCrypto.randomInt32
        // probabilistic: 1 in 2^32 chance this fails legitimately
        assertFalse("two random int32 values should differ", a == b)
    }

    @Test fun randomInt64_notAlwaysSame() {
        val a = FooCrypto.randomInt64
        val b = FooCrypto.randomInt64
        assertFalse("two random int64 values should differ", a == b)
    }

    // proprietaryHash

    @Test fun proprietaryHash_nonEmpty() {
        val h = FooCrypto.proprietaryHash("test")
        assertNotNull(h)
        assertTrue(h.isNotEmpty())
    }

    @Test fun proprietaryHash_stable() {
        assertEquals(FooCrypto.proprietaryHash("hello"), FooCrypto.proprietaryHash("hello"))
    }

    @Test fun proprietaryHash_differentInputs() {
        assertFalse(FooCrypto.proprietaryHash("abc") == FooCrypto.proprietaryHash("xyz"))
    }

    @Test fun proprietaryHash_length16() {
        // The implementation takes a 16-char substring (indices 8..24)
        assertEquals(16, FooCrypto.proprietaryHash("anything").length)
    }
}
