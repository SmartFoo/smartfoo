package com.smartfoo.android.core.logging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartfoo.android.core.FooTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FooLogTest {

    private lateinit var recorder: RecordingPrinter

    private class RecordingPrinter : FooLogPrinter() {
        data class Entry(val tag: String?, val level: Int, val msg: String?, val e: Throwable?)

        val entries = mutableListOf<Entry>()

        override fun printlnInternal(tag: String?, level: Int, msg: String?, e: Throwable?): Boolean {
            entries.add(Entry(tag, level, msg, e))
            return true
        }

        override fun clear() = entries.clear()
    }

    @Before
    fun setup() {
        FooTest.initialize(FooTest.TestType.Android)
        recorder = RecordingPrinter()
        FooLog.clearPrinters()
        FooLog.addPrinter(recorder)
    }

    @After
    fun teardown() {
        FooLog.removePrinter(recorder)
    }

    // Printer registration

    @Test fun addPrinter_isAdded() {
        assertTrue(FooLog.isAdded(recorder))
    }

    @Test fun removePrinter_notAdded() {
        FooLog.removePrinter(recorder)
        assertFalse(FooLog.isAdded(recorder))
    }

    @Test fun clearPrinters_removesAll() {
        FooLog.clearPrinters()
        assertFalse(FooLog.isAdded(recorder))
    }

    @Test fun addPrinter_null_noOp() {
        val countBefore = recorder.entries.size
        FooLog.addPrinter(null) // should not crash
        FooLog.d("TAG", "msg")
        assertEquals(countBefore + 1, recorder.entries.size)
    }

    // Log dispatch — each level reaches the printer with the correct level code

    @Test fun verbose_dispatchedToRecorder() {
        FooLog.v("TAG", "msg")
        assertEquals(1, recorder.entries.size)
        assertEquals(FooLog.FooLogLevel.Verbose, recorder.entries[0].level)
        assertEquals("TAG", recorder.entries[0].tag)
        assertEquals("msg", recorder.entries[0].msg)
    }

    @Test fun debug_dispatchedToRecorder() {
        FooLog.d("TAG", "msg")
        assertEquals(1, recorder.entries.size)
        assertEquals(FooLog.FooLogLevel.Debug, recorder.entries[0].level)
    }

    @Test fun info_dispatchedToRecorder() {
        FooLog.i("TAG", "msg")
        assertEquals(1, recorder.entries.size)
        assertEquals(FooLog.FooLogLevel.Info, recorder.entries[0].level)
    }

    @Test fun warn_dispatchedToRecorder() {
        FooLog.w("TAG", "msg")
        assertEquals(1, recorder.entries.size)
        assertEquals(FooLog.FooLogLevel.Warn, recorder.entries[0].level)
    }

    @Test fun error_dispatchedToRecorder() {
        FooLog.e("TAG", "msg")
        assertEquals(1, recorder.entries.size)
        assertEquals(FooLog.FooLogLevel.Error, recorder.entries[0].level)
    }

    @Test fun fatal_dispatchedToRecorder() {
        FooLog.f("TAG", "msg")
        assertEquals(1, recorder.entries.size)
        assertEquals(FooLog.FooLogLevel.Fatal, recorder.entries[0].level)
    }

    @Test fun log_withThrowable() {
        val ex = RuntimeException("test error")
        FooLog.e("TAG", "oops", ex)
        assertEquals(1, recorder.entries.size)
        assertEquals(ex, recorder.entries[0].e)
    }

    // Multiple printers

    @Test fun twoPrinters_bothReceive() {
        val second = RecordingPrinter()
        FooLog.addPrinter(second)
        FooLog.d("TAG", "msg")
        assertEquals(1, recorder.entries.size)
        assertEquals(1, second.entries.size)
        FooLog.removePrinter(second)
    }

    @Test fun removePrinter_stopsReceiving() {
        FooLog.removePrinter(recorder)
        FooLog.d("TAG", "msg")
        assertEquals(0, recorder.entries.size)
    }

    @Test fun clearPrinters_stopsAllReceiving() {
        FooLog.clearPrinters()
        FooLog.d("TAG", "msg")
        assertEquals(0, recorder.entries.size)
    }

    // TAG length limiting

    @Test fun tag_shortName_unchanged() {
        val short = "ShortName"
        assertEquals(short, FooLog.TAG(short))
    }

    @Test fun tag_exactLimit_unchanged() {
        val exactly23 = "A".repeat(FooLog.LOG_TAG_LENGTH_LIMIT)
        assertEquals(exactly23, FooLog.TAG(exactly23))
    }

    @Test fun tag_tooLong_truncated() {
        val long = "A".repeat(50)
        val result = FooLog.TAG(long)
        assertEquals(FooLog.LOG_TAG_LENGTH_LIMIT, result.length)
        assertTrue("should contain ellipsis", result.contains('…'))
    }

    @Test fun tag_tooLong_preservesStartAndEnd() {
        val long = "StartXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXEnd"
        val result = FooLog.TAG(long)
        assertTrue("should start with beginning of name", result.startsWith("Start"))
        assertTrue("should end with end of name", result.endsWith("End"))
    }
}
