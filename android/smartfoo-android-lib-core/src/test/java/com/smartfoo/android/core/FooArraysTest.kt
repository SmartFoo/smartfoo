package com.smartfoo.android.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FooArraysTest {

    // isNullOrEmpty — accepts Array<Any>?

    @Test fun isNullOrEmpty_null() = assertTrue(FooArrays.isNullOrEmpty(null))
    @Test fun isNullOrEmpty_empty() = assertTrue(FooArrays.isNullOrEmpty(emptyArray()))
    @Test fun isNullOrEmpty_nonEmpty() = assertFalse(FooArrays.isNullOrEmpty(arrayOf("a")))

    // equals(ByteArray?, ByteArray?)

    @Test fun equals_bothNull() = assertTrue(FooArrays.equals(null, null))
    @Test fun equals_firstNull() = assertFalse(FooArrays.equals(null, byteArrayOf(1)))
    @Test fun equals_secondNull() = assertFalse(FooArrays.equals(byteArrayOf(1), null))
    @Test fun equals_identical() = assertTrue(FooArrays.equals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
    @Test fun equals_different() = assertFalse(FooArrays.equals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
    @Test fun equals_differentLength() = assertFalse(FooArrays.equals(byteArrayOf(1, 2), byteArrayOf(1, 2, 3)))
    @Test fun equals_bothEmpty() = assertTrue(FooArrays.equals(byteArrayOf(), byteArrayOf()))

    // copy(source, sourceOffset, destination, destinationOffset, count)

    @Test fun copy_fullArray() {
        val src = byteArrayOf(1, 2, 3, 4, 5)
        val dst = ByteArray(5)
        val result = FooArrays.copy(src, 0, dst, 0, 5)
        assertArrayEquals(src, result)
        assertNotSame(src, result)
    }

    @Test fun copy_subrange() {
        val src = byteArrayOf(10, 20, 30, 40, 50)
        val dst = ByteArray(3)
        FooArrays.copy(src, 1, dst, 0, 3)
        assertArrayEquals(byteArrayOf(20, 30, 40), dst)
    }

    @Test fun copy_withOffset() {
        val src = byteArrayOf(1, 2, 3)
        val dst = ByteArray(5)
        FooArrays.copy(src, 0, dst, 2, 3)
        assertArrayEquals(byteArrayOf(0, 0, 1, 2, 3), dst)
    }

    // copy(source, offset, count) — allocates new array

    @Test fun copy_allocating() {
        val src = byteArrayOf(10, 20, 30, 40, 50)
        val result = FooArrays.copy(src, 1, 3)
        assertArrayEquals(byteArrayOf(20, 30, 40), result)
        assertNotSame(src, result)
    }

    // fill — NOTE: the 'length' param is actually toIndex (exclusive end), matching Arrays.fill semantics

    @Test fun fill_wholeArray() {
        val array = ByteArray(5)
        FooArrays.fill(array, 0xFF.toByte(), 0, 5)
        assertArrayEquals(byteArrayOf(-1, -1, -1, -1, -1), array)
    }

    @Test fun fill_subrange() {
        val array = byteArrayOf(0, 0, 0, 0, 0)
        FooArrays.fill(array, 9.toByte(), 1, 4) // fills indices 1, 2, 3
        assertArrayEquals(byteArrayOf(0, 9, 9, 9, 0), array)
    }

    @Test fun fill_zeroRange() {
        val array = byteArrayOf(1, 2, 3)
        FooArrays.fill(array, 0xFF.toByte(), 1, 1) // empty range
        assertArrayEquals(byteArrayOf(1, 2, 3), array)
    }
}
