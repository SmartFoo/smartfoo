package com.smartfoo.android.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooObjectsTest {

    @Test fun equals_bothNull() = assertTrue(FooObjects.equals(null, null))
    @Test fun equals_firstNull() = assertFalse(FooObjects.equals(null, "x"))
    @Test fun equals_secondNull() = assertFalse(FooObjects.equals("x", null))
    @Test fun equals_sameString() = assertTrue(FooObjects.equals("abc", "abc"))
    @Test fun equals_differentStrings() = assertFalse(FooObjects.equals("abc", "xyz"))
    @Test fun equals_sameInt() = assertTrue(FooObjects.equals(42, 42))
    @Test fun equals_differentInt() = assertFalse(FooObjects.equals(1, 2))
    @Test fun equals_sameReference() {
        val obj = Any()
        assertTrue(FooObjects.equals(obj, obj))
    }
}
