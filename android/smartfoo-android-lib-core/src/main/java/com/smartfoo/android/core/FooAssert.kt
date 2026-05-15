package com.smartfoo.android.core

object FooAssert {
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
