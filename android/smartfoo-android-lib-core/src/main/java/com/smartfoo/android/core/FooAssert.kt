package com.smartfoo.android.core

/**
 * Lightweight assertion helpers for use in tests and debug validation.
 */
object FooAssert {
    /**
     * Asserts that executing [runnable] throws an exception that is an instance of
     * [illegalArgumentExceptionClass]. Throws [AssertionError] if no exception is thrown, or
     * rethrows the exception if it is not of the expected type.
     *
     * @param runnable the code block expected to throw
     * @param illegalArgumentExceptionClass the expected exception class
     * @throws AssertionError if [runnable] completes without throwing
     */
    fun assertThrows(
        runnable: Runnable,
        illegalArgumentExceptionClass: Class<IllegalArgumentException?>
    ) {
        try {
            runnable.run()
            throw AssertionError("Expected exception not thrown")
        } catch (e: Exception) {
            if (illegalArgumentExceptionClass.isInstance(e)) {
                return
            }
            throw e
        }
    }
}
