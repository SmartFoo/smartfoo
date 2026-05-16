package com.smartfoo.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooBooleanTest {

    // toBoolean(Boolean?, defaultValue)

    @Test fun toBoolean_boolNull_defaultFalse() {
        val b: Boolean? = null
        assertFalse(FooBoolean.toBoolean(b))
    }
    @Test fun toBoolean_boolNull_defaultTrue() {
        val b: Boolean? = null
        assertTrue(FooBoolean.toBoolean(b, defaultValue = true))
    }
    @Test fun toBoolean_boolTrue() = assertTrue(FooBoolean.toBoolean(true))
    @Test fun toBoolean_boolFalse() = assertFalse(FooBoolean.toBoolean(false))

    // toBoolean(Number?, defaultValue)

    @Test fun toBoolean_numNull_defaultFalse() {
        val n: Number? = null
        assertFalse(FooBoolean.toBoolean(n))
    }
    @Test fun toBoolean_numNull_defaultTrue() {
        val n: Number? = null
        assertTrue(FooBoolean.toBoolean(n, defaultValue = true))
    }
    @Test fun toBoolean_numZero() = assertFalse(FooBoolean.toBoolean(0))
    @Test fun toBoolean_numOne() = assertTrue(FooBoolean.toBoolean(1))
    @Test fun toBoolean_numMinusOne() = assertTrue(FooBoolean.toBoolean(-1))

    // toBoolean(String?, defaultValue) — requires explicit default

    @Test fun toBoolean_stringTrue_ignoreCase() = assertTrue(FooBoolean.toBoolean("true", false))
    @Test fun toBoolean_stringTRUE() = assertTrue(FooBoolean.toBoolean("TRUE", false))
    @Test fun toBoolean_stringFalse_ignoreCase() = assertFalse(FooBoolean.toBoolean("false", true))
    @Test fun toBoolean_stringFALSE() = assertFalse(FooBoolean.toBoolean("FALSE", true))
    @Test fun toBoolean_stringYes() = assertTrue(FooBoolean.toBoolean("yes", false))
    @Test fun toBoolean_stringNo() = assertFalse(FooBoolean.toBoolean("no", true))
    @Test fun toBoolean_stringY() = assertTrue(FooBoolean.toBoolean("y", false))
    @Test fun toBoolean_stringN() = assertFalse(FooBoolean.toBoolean("n", true))
    @Test fun toBoolean_string1() = assertTrue(FooBoolean.toBoolean("1", false))
    @Test fun toBoolean_string0() = assertFalse(FooBoolean.toBoolean("0", true))

    @Test(expected = IllegalArgumentException::class)
    fun toBoolean_stringGarbage() {
        FooBoolean.toBoolean("garbage", false)
    }

    // toByte(Boolean?)

    @Test fun toByte_nullBoolean() {
        val b: Boolean? = null
        assertEquals(0.toByte(), FooBoolean.toByte(b))
    }

    // toByte(Boolean)

    @Test fun toByte_true() = assertEquals(1.toByte(), FooBoolean.toByte(true))
    @Test fun toByte_false() = assertEquals(0.toByte(), FooBoolean.toByte(false))

    // toString(Boolean)

    @Test fun toString_true() = assertEquals("true", FooBoolean.toString(true))
    @Test fun toString_false() = assertEquals("false", FooBoolean.toString(false))
}
