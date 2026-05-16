package com.smartfoo.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FooStringTest {

    // isNullOrEmpty(String?)

    @Test fun isNullOrEmpty_null() = assertTrue(FooString.isNullOrEmpty(null as String?))
    @Test fun isNullOrEmpty_empty() = assertTrue(FooString.isNullOrEmpty(""))
    @Test fun isNullOrEmpty_nonEmpty() = assertFalse(FooString.isNullOrEmpty("x"))

    // isNullOrEmpty(CharSequence?)

    @Test fun isNullOrEmpty_charSequence_null() = assertTrue(FooString.isNullOrEmpty(null as CharSequence?))
    @Test fun isNullOrEmpty_charSequence_empty() = assertTrue(FooString.isNullOrEmpty("" as CharSequence))
    @Test fun isNullOrEmpty_charSequence_nonEmpty() = assertFalse(FooString.isNullOrEmpty("x" as CharSequence))

    // getBytes / getString round-trip

    @Test fun getBytes_getString_roundTrip() {
        val original = "Hello, World! 😀"
        val bytes = FooString.getBytes(original)
        val result = FooString.getString(bytes, 0, bytes.size)
        assertEquals(original, result)
    }

    @Test fun getString_subrange() {
        val bytes = FooString.getBytes("ABCDE")
        assertEquals("BCD", FooString.getString(bytes, 1, 3))
    }

    // fromNullTerminatedBytes / toNullTerminatedBytes

    @Test fun nullTerminated_roundTrip() {
        val original = "hello"
        val bytes = FooString.toNullTerminatedBytes(original)
        assertEquals(original.length + 1, bytes.size)
        assertEquals(0.toByte(), bytes.last())
        assertEquals(original, FooString.fromNullTerminatedBytes(bytes, 0))
    }

    @Test fun fromNullTerminated_null() = assertNull(FooString.fromNullTerminatedBytes(null, 0))

    @Test fun fromNullTerminated_immediateNull() {
        assertNull(FooString.fromNullTerminatedBytes(byteArrayOf(0), 0))
    }

    // toHexString

    @Test fun toHexString_null() = assertEquals("null", FooString.toHexString(null as ByteArray?))

    @Test fun toHexString_knownBytes() {
        val bytes = byteArrayOf(0x00, 0xFF.toByte(), 0x0A, 0xBE.toByte())
        assertEquals("00-FF-0A-BE", FooString.toHexString(bytes))
    }

    @Test fun toHexString_singleByte() = assertEquals("AB", FooString.toHexString(byteArrayOf(0xAB.toByte())))

    // toBitString

    @Test fun toBitString_allZeros() = assertEquals("00000000", FooString.toBitString(0x00.toByte(), 8))

    @Test fun toBitString_allOnes() = assertEquals("11111111", FooString.toBitString(0xFF.toByte(), 8))

    @Test fun toBitString_knownByte() {
        // 0b10110100 = 0xB4 — bits 7,5,4,2 are set
        assertEquals("10110100", FooString.toBitString(0xB4.toByte(), 8))
    }

    @Test fun toBitString_maxBitsTruncates() {
        // maxBits=4 reads bits 3..0 of 0b10110100 → bits 3,2,1,0 = 0,1,0,0 → "0100"
        assertEquals("0100", FooString.toBitString(0xB4.toByte(), 4))
    }

    // capitalize

    @Test fun capitalize_lowercase() = assertEquals("Hello", FooString.capitalize("hello"))
    @Test fun capitalize_alreadyCapital() = assertEquals("Hello", FooString.capitalize("Hello"))
    @Test fun capitalize_empty() = assertEquals("", FooString.capitalize(""))
    @Test fun capitalize_null() = assertEquals("", FooString.capitalize(null))
    @Test fun capitalize_singleChar() = assertEquals("A", FooString.capitalize("a"))

    // split

    @Test fun split_basic() {
        val parts = FooString.split("a,b,c", ",", 0)!!
        assertEquals(3, parts.size)
        assertEquals("a", parts[0])
        assertEquals("b", parts[1])
        assertEquals("c", parts[2])
    }

    @Test fun split_null() = assertNull(FooString.split(null, ",", 0))

    @Test fun split_noSeparator() {
        val parts = FooString.split("abc", ",", 0)!!
        assertEquals(1, parts.size)
        assertEquals("abc", parts[0])
    }

    // replace

    @Test fun replace_basic() = assertEquals("xbc", FooString.replace("abc", "a", "x"))
    @Test fun replace_null() = assertEquals("", FooString.replace(null, "a", "x"))
    @Test fun replace_multipleOccurrences() = assertEquals("x-x-x", FooString.replace("a-a-a", "a", "x"))
    @Test fun replaceFirst_onlyFirst() = assertEquals("x-a-a", FooString.replaceFirst("a-a-a", "a", "x"))

    // equals

    @Test fun equals_bothNull() = assertTrue(FooString.equals(null, null))
    @Test fun equals_firstNull() = assertFalse(FooString.equals(null, "x"))
    @Test fun equals_secondNull() = assertFalse(FooString.equals("x", null))
    @Test fun equals_same() = assertTrue(FooString.equals("abc", "abc"))
    @Test fun equals_different() = assertFalse(FooString.equals("abc", "xyz"))

    // quote

    @Test fun quote_null() = assertEquals("null", FooString.quote(null))
    @Test fun quote_string() = assertEquals("\"hello\"", FooString.quote("hello"))
    @Test fun quote_number() = assertEquals("42", FooString.quote(42))

    // padNumber / formatNumber

    @Test fun padNumber_padsToLength() = assertEquals("007", FooString.padNumber(7, '0', 3))
    @Test fun padNumber_nopad() = assertEquals("42", FooString.padNumber(42, '0', 2))
    @Test fun formatNumber_long() = assertEquals("007", FooString.formatNumber(7L, 3))

    // getTimeElapsedString

    @Test fun getTimeElapsedString_zero() = assertEquals("00:00:00.000", FooString.getTimeElapsedString(0))

    @Test fun getTimeElapsedString_oneSecond() = assertEquals("00:00:01.000", FooString.getTimeElapsedString(1_000))

    @Test fun getTimeElapsedString_oneMinute() = assertEquals("00:01:00.000", FooString.getTimeElapsedString(60_000))

    @Test fun getTimeElapsedString_oneHour() = assertEquals("01:00:00.000", FooString.getTimeElapsedString(3_600_000))

    @Test fun getTimeElapsedString_combined() = assertEquals("01:02:03.456", FooString.getTimeElapsedString(3_723_456))

    @Test fun getTimeElapsedString_negative() = assertEquals("00:00:00.000", FooString.getTimeElapsedString(-1))

    // toChar

    @Test fun toChar_true() = assertEquals('1', FooString.toChar(true))
    @Test fun toChar_false() = assertEquals('0', FooString.toChar(false))

    // contains

    @Test fun contains_true() = assertTrue(FooString.contains("hello world", "world"))
    @Test fun contains_false() = assertFalse(FooString.contains("hello", "xyz"))
}
