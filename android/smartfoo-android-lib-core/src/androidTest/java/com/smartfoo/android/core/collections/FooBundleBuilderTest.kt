package com.smartfoo.android.core.collections

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FooBundleBuilderTest {

    // Primitive scalars

    @Test fun putBoolean_roundTrip() {
        val bundle = FooBundleBuilder().putBoolean("k", true).build()
        assertTrue(bundle.getBoolean("k"))
    }

    @Test fun putBoolean_false() {
        val bundle = FooBundleBuilder().putBoolean("k", false).build()
        assertFalse(bundle.getBoolean("k"))
    }

    @Test fun putByte_roundTrip() {
        val bundle = FooBundleBuilder().putByte("k", 42).build()
        assertEquals(42.toByte(), bundle.getByte("k"))
    }

    @Test fun putChar_roundTrip() {
        val bundle = FooBundleBuilder().putChar("k", 'Z').build()
        assertEquals('Z', bundle.getChar("k"))
    }

    @Test fun putShort_roundTrip() {
        val bundle = FooBundleBuilder().putShort("k", 1234).build()
        assertEquals(1234.toShort(), bundle.getShort("k"))
    }

    @Test fun putInt_roundTrip() {
        val bundle = FooBundleBuilder().putInt("k", 99).build()
        assertEquals(99, bundle.getInt("k"))
    }

    @Test fun putLong_roundTrip() {
        val bundle = FooBundleBuilder().putLong("k", Long.MAX_VALUE).build()
        assertEquals(Long.MAX_VALUE, bundle.getLong("k"))
    }

    @Test fun putFloat_roundTrip() {
        val bundle = FooBundleBuilder().putFloat("k", 3.14f).build()
        assertEquals(3.14f, bundle.getFloat("k"))
    }

    @Test fun putDouble_roundTrip() {
        val bundle = FooBundleBuilder().putDouble("k", Math.PI).build()
        assertEquals(Math.PI, bundle.getDouble("k"), 0.0)
    }

    @Test fun putString_roundTrip() {
        val bundle = FooBundleBuilder().putString("k", "hello").build()
        assertEquals("hello", bundle.getString("k"))
    }

    @Test fun putString_null() {
        val bundle = FooBundleBuilder().putString("k", null).build()
        assertNull(bundle.getString("k"))
    }

    // Arrays

    @Test fun putBooleanArray_roundTrip() {
        val arr = booleanArrayOf(true, false, true)
        val bundle = FooBundleBuilder().putBooleanArray("k", arr).build()
        assertArrayEquals(arr, bundle.getBooleanArray("k"))
    }

    @Test fun putByteArray_roundTrip() {
        val arr = byteArrayOf(1, 2, 3)
        val bundle = FooBundleBuilder().putByteArray("k", arr).build()
        assertArrayEquals(arr, bundle.getByteArray("k"))
    }

    @Test fun putIntArray_roundTrip() {
        val arr = intArrayOf(10, 20, 30)
        val bundle = FooBundleBuilder().putIntArray("k", arr).build()
        assertArrayEquals(arr, bundle.getIntArray("k"))
    }

    @Test fun putStringArray_roundTrip() {
        val arr = arrayOf<String?>("a", "b", "c")
        val bundle = FooBundleBuilder().putStringArray("k", arr).build()
        assertArrayEquals(arr, bundle.getStringArray("k"))
    }

    // ArrayList variants

    @Test fun putStringArrayList_roundTrip() {
        val list = arrayListOf<String?>("x", "y", "z")
        val bundle = FooBundleBuilder().putStringArrayList("k", list).build()
        assertEquals(list, bundle.getStringArrayList("k"))
    }

    @Test fun putIntegerArrayList_roundTrip() {
        val list = arrayListOf<Int?>(1, 2, 3)
        val bundle = FooBundleBuilder().putIntegerArrayList("k", list).build()
        assertEquals(list, bundle.getIntegerArrayList("k"))
    }

    // Nested Bundle

    @Test fun putBundle_roundTrip() {
        val inner = FooBundleBuilder().putString("inner_key", "inner_value").build()
        val outer = FooBundleBuilder().putBundle("nested", inner).build()
        val retrieved = outer.getBundle("nested")!!
        assertEquals("inner_value", retrieved.getString("inner_key"))
    }

    // Multiple values and chaining

    @Test fun chain_multipleValues() {
        val bundle = FooBundleBuilder()
            .putBoolean("bool", true)
            .putInt("int", 42)
            .putString("str", "hello")
            .putFloat("float", 1.5f)
            .build()
        assertTrue(bundle.getBoolean("bool"))
        assertEquals(42, bundle.getInt("int"))
        assertEquals("hello", bundle.getString("str"))
        assertEquals(1.5f, bundle.getFloat("float"))
    }

    // Empty builder

    @Test fun emptyBuilder_emptyBundle() {
        val bundle = FooBundleBuilder().build()
        assertEquals(0, bundle.size())
    }
}
