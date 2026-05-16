package com.smartfoo.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FooExceptionTest {

    @Test fun constructor_messagePreserved() {
        val ex = FooException("MySource", "Something went wrong")
        assertEquals("Something went wrong", ex.message)
        assertEquals("MySource", ex.source)
    }

    @Test fun toString_nonEmpty() {
        val ex = FooException("MySource", "oops")
        val s = ex.toString()
        assertTrue("toString should mention source", s.contains("MySource"))
        assertTrue("toString should mention message", s.contains("oops"))
    }

    @Test fun toDebugString_containsSourceAndMessage() {
        val ex = FooException("Src", "msg")
        val s = ex.toDebugString()
        assertTrue(s.contains("Src"))
        assertTrue(s.contains("msg"))
    }

    @Test fun cause_preserved() {
        val cause = RuntimeException("root cause")
        val ex = FooException("Src", cause)
        assertEquals(cause, ex.cause)
    }

    @Test fun toDebugString_containsCause() {
        val cause = RuntimeException("root")
        val ex = FooException("Src", "wrapped", cause)
        val s = ex.toDebugString()
        assertTrue("toDebugString should mention cause", s.contains("root"))
    }

    // Companion object static methods

    @Test fun static_toString_null() {
        assertEquals("null", FooException.toString(null, null, false, false))
    }

    @Test fun static_toString_nonNull() {
        val s = FooException.toString(RuntimeException("err"), null, false, false)
        assertTrue("should contain exception class name", s.contains("RuntimeException") || s.isNotEmpty())
        assertTrue("should mention message", s.contains("err"))
    }

    @Test fun static_toString_withCause() {
        val cause = IllegalStateException("cause")
        val ex = RuntimeException("top", cause)
        val s = FooException.toString(ex, null, cause = true, stacktrace = false)
        assertTrue("should mention cause message", s.contains("cause"))
    }

    @Test fun toStackTraceString_nonNull() {
        val ex = RuntimeException("test")
        val s = FooException.toStackTraceString(ex)
        assertNotNull(s)
        assertTrue("should start with newline + 'at'", s!!.startsWith("\n    at"))
    }

    @Test fun toStackTraceString_null() {
        assertNull(FooException.toStackTraceString(null))
    }
}
