package com.smartfoo.android.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class FooMemoryStreamTest {

    private fun stream() = FooMemoryStream()

    // writeInt8 / readInt8

    @Test fun int8_roundTrip_positive() {
        val s = stream()
        s.writeInt8(42)
        s.setPosition(0)
        assertEquals(42.toByte(), s.readInt8())
    }

    @Test fun int8_roundTrip_negative() {
        val s = stream()
        s.writeInt8((-1).toByte())
        s.setPosition(0)
        assertEquals((-1).toByte(), s.readInt8())
    }

    // writeUInt8 / readUInt8

    @Test fun uint8_roundTrip() {
        val s = stream()
        s.writeUInt8(255.toShort())
        s.setPosition(0)
        assertEquals(255.toShort(), s.readUInt8())
    }

    @Test fun uint8_zero() {
        val s = stream()
        s.writeUInt8(0)
        s.setPosition(0)
        assertEquals(0.toShort(), s.readUInt8())
    }

    // writeInt16 / readInt16 (big-endian)

    @Test fun int16_roundTrip() {
        val s = stream()
        s.writeInt16(0x0102.toShort())
        s.setPosition(0)
        assertEquals(0x0102.toShort(), s.readInt16())
    }

    @Test fun int16_negative() {
        val s = stream()
        s.writeInt16((-1).toShort())
        s.setPosition(0)
        assertEquals((-1).toShort(), s.readInt16())
    }

    // writeUInt16 / readUInt16

    @Test fun uint16_roundTrip() {
        val s = stream()
        s.writeUInt16(0xABCD)
        s.setPosition(0)
        assertEquals(0xABCD, s.readUInt16())
    }

    @Test fun uint16_max() {
        val s = stream()
        s.writeUInt16(0xFFFF)
        s.setPosition(0)
        assertEquals(0xFFFF, s.readUInt16())
    }

    // writeInt32 / readInt32

    @Test fun int32_roundTrip() {
        val s = stream()
        s.writeInt32(0x01020304L)
        s.setPosition(0)
        assertEquals(0x01020304, s.readInt32())
    }

    @Test fun int32_negative() {
        val s = stream()
        s.writeInt32(-1L)
        s.setPosition(0)
        assertEquals(-1, s.readInt32())
    }

    // writeUInt32 / readUInt32

    @Test fun uint32_roundTrip() {
        val s = stream()
        s.writeUInt32(0xABCDEF01L)
        s.setPosition(0)
        assertEquals(0xABCDEF01L, s.readUInt32())
    }

    @Test fun uint32_max() {
        val s = stream()
        s.writeUInt32(0xFFFFFFFFL)
        s.setPosition(0)
        assertEquals(0xFFFFFFFFL, s.readUInt32())
    }

    // writeString / readString

    @Test fun string_roundTrip() {
        val s = stream()
        s.writeString("hello")
        s.setPosition(0)
        assertEquals("hello", s.readString())
    }

    @Test fun string_empty() {
        val s = stream()
        s.writeString("")
        s.setPosition(0)
        assertEquals("", s.readString())
    }

    @Test fun string_null() {
        val s = stream()
        s.writeString(null)
        s.setPosition(0)
        assertEquals("", s.readString())
    }

    // position advances after writes

    @Test fun position_advancesAfterWrites() {
        val s = stream()
        assertEquals(0, s.getPosition())
        s.writeInt8(1)
        assertEquals(1, s.getPosition())
        s.writeInt16(2)
        assertEquals(3, s.getPosition())
        s.writeInt32(3L)
        assertEquals(7, s.getPosition())
    }

    // reset() resets position to 0, length to 0 (clears length)

    @Test fun reset_resetsLength() {
        val s = stream()
        s.writeInt32(0L)
        assertEquals(4, s.getLength())
        s.reset()
        assertEquals(0, s.getLength())
        assertEquals(0, s.getPosition())
    }

    // clear()

    @Test fun clear_emptiesBuffer() {
        val s = stream()
        s.writeInt32(0L)
        s.clear()
        assertEquals(0, s.getLength())
        assertEquals(0, s.getPosition())
        assertEquals(FooMemoryStream.EMPTY_BUFFER, s.buffer)
    }

    // setLength truncates

    @Test fun setLength_truncates() {
        val s = stream()
        s.writeInt32(0x01020304L)
        s.setLength(2)
        assertEquals(2, s.getLength())
    }

    // Multiple values sequential round-trip

    @Test fun multipleValues_roundTrip() {
        val s = stream()
        s.writeInt8(10)
        s.writeInt16(300)
        s.writeString("test")
        s.setPosition(0)
        assertEquals(10.toByte(), s.readInt8())
        assertEquals(300.toShort(), s.readInt16())
        assertEquals("test", s.readString())
    }

    // newBytes static methods — little-endian

    @Test fun newBytes_byte() {
        assertArrayEquals(byteArrayOf(0x42), FooMemoryStream.newBytes(0x42.toByte()))
    }

    @Test fun newBytes_short_littleEndian() {
        // 0x0102 → [0x02, 0x01]
        assertArrayEquals(byteArrayOf(0x02, 0x01), FooMemoryStream.newBytes(0x0102.toShort()))
    }

    @Test fun newBytes_int_littleEndian() {
        // 0x01020304 → [0x04, 0x03, 0x02, 0x01]
        assertArrayEquals(byteArrayOf(0x04, 0x03, 0x02, 0x01), FooMemoryStream.newBytes(0x01020304))
    }

    @Test fun newBytes_long_littleEndian() {
        // 0x0102030405060708L → [0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01]
        assertArrayEquals(
            byteArrayOf(0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01),
            FooMemoryStream.newBytes(0x0102030405060708L)
        )
    }
}
