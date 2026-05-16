package com.smartfoo.android.core

import com.smartfoo.android.core.logging.FooLog.addPrinter
import com.smartfoo.android.core.logging.FooLog.clearPrinters
import com.smartfoo.android.core.logging.FooLog.isEnabled
import com.smartfoo.android.core.logging.FooLogAdbPrinter
import com.smartfoo.android.core.logging.FooLogConsolePrinter
import com.smartfoo.android.core.logging.FooLogUnixJavaFormatter

/**
 * Test bootstrapping utilities for unit and instrumentation tests.
 *
 * Call [initialize] at the start of a test suite to configure logging output appropriate for
 * the chosen [TestType].
 */
object FooTest {
    /**
     * Configures the logging system for the given [testType].
     *
     * Enables logging, clears any existing printers, and attaches the appropriate printer:
     * [FooLogAdbPrinter] for Android instrumentation tests, or [FooLogConsolePrinter] for
     * standard JUnit tests.
     *
     * @param testType the environment in which the tests are running
     * @throws IllegalArgumentException if [testType] is not a recognised [TestType] value
     */
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

    /**
     * Identifies the test execution environment so that the appropriate log printer can be chosen.
     */
    enum class TestType {
        /** Standard JVM JUnit test; uses console output. */
        Junit,
        /** Android instrumentation test; uses ADB logcat output. */
        Android
    }
}
