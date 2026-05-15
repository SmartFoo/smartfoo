package com.smartfoo.android.core

import com.smartfoo.android.core.logging.FooLog.addPrinter
import com.smartfoo.android.core.logging.FooLog.clearPrinters
import com.smartfoo.android.core.logging.FooLog.isEnabled
import com.smartfoo.android.core.logging.FooLogAdbPrinter
import com.smartfoo.android.core.logging.FooLogConsolePrinter
import com.smartfoo.android.core.logging.FooLogUnixJavaFormatter

object FooTest {
    fun initialize(testType: TestType) {
        isEnabled = true
        clearPrinters()
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val logPrinter = when (testType) {
            TestType.Android -> FooLogAdbPrinter.getInstance()
            TestType.Junit -> FooLogConsolePrinter.getInstance(FooLogUnixJavaFormatter())
            else -> throw IllegalArgumentException("testType must be one of TestType.*")
        }
        addPrinter(logPrinter)
    }

    enum class TestType {
        Junit,
        Android
    }
}
