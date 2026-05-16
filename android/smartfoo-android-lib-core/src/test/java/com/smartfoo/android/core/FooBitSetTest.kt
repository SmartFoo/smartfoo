package com.smartfoo.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooBitSetTest {

    // FooBitSet(Byte) — bit j corresponds to (value and (1 shl j)) != 0

    @Test fun byte_allZeros() {
        val bs = FooBitSet(0x00.toByte())
        for (i in 0 until 8) assertFalse("bit $i should be 0", bs.get(i))
    }

    @Test fun byte_allOnes() {
        val bs = FooBitSet(0xFF.toByte())
        for (i in 0 until 8) assertTrue("bit $i should be 1", bs.get(i))
    }

    @Test fun byte_knownPattern() {
        // 0xB4 = 0b10110100 — bits 2, 4, 5, 7 are set
        val bs = FooBitSet(0xB4.toByte())
        assertFalse("bit 0 (LSB)", bs.get(0))
        assertFalse("bit 1",       bs.get(1))
        assertTrue ("bit 2",       bs.get(2))
        assertFalse("bit 3",       bs.get(3))
        assertTrue ("bit 4",       bs.get(4))
        assertTrue ("bit 5",       bs.get(5))
        assertFalse("bit 6",       bs.get(6))
        assertTrue ("bit 7 (MSB)", bs.get(7))
    }

    @Test fun byte_lsb_only() {
        val bs = FooBitSet(0x01.toByte())
        assertTrue("bit 0", bs.get(0))
        for (i in 1 until 8) assertFalse("bit $i", bs.get(i))
    }

    @Test fun byte_msb_only() {
        val bs = FooBitSet(0x80.toByte())
        assertTrue("bit 7", bs.get(7))
        for (i in 0 until 7) assertFalse("bit $i", bs.get(i))
    }

    // FooBitSet(ByteArray) — i*8 + j for byte i, bit j

    @Test fun byteArray_twoBytes() {
        // byte[0] = 0x01 (bit 0 set), byte[1] = 0x80 (bit 7 set → position 15)
        val bs = FooBitSet(byteArrayOf(0x01, 0x80.toByte()))
        assertTrue ("position 0",  bs.get(0))
        assertFalse("position 1",  bs.get(1))
        assertFalse("position 14", bs.get(14))
        assertTrue ("position 15", bs.get(15))
    }

    @Test fun byteArray_allZeros() {
        val bs = FooBitSet(byteArrayOf(0x00, 0x00))
        for (i in 0 until 16) assertFalse("position $i", bs.get(i))
    }

    @Test fun byteArray_singleByte_matchesByteConstructor() {
        val b = 0xB4.toByte()
        val bsByte = FooBitSet(b)
        val bsArray = FooBitSet(byteArrayOf(b))
        for (i in 0 until 8) {
            assertEquals("bit $i", bsArray.get(i), bsByte.get(i))
        }
    }

    // length property

    @Test fun length_singleByte() = assertEquals(8, FooBitSet(0x00.toByte()).length)
    @Test fun length_threeBytes() = assertEquals(24, FooBitSet(ByteArray(3)).length)
}
