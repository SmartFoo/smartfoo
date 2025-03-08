package com.smartfoo.android.core;

public class FooAssert {
    public static void assertThrows(Runnable runnable, Class<IllegalArgumentException> illegalArgumentExceptionClass) {
        try {
            runnable.run();
            throw new AssertionError("Expected exception not thrown");
        } catch (Exception e) {
            if (illegalArgumentExceptionClass.isInstance(e)) {
                return;
            }
            throw e;
        }
    }
}
