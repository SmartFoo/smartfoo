package com.smartfoo.android.core.collections

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FooCollectionsEquivalentTest {

    // equivalent — order-independent, duplicate-independent comparison

    @Test fun equivalent_sameElements_sameOrder() {
        assertTrue(FooCollections.equivalent(listOf(1, 2, 3), listOf(1, 2, 3)))
    }

    @Test fun equivalent_sameElements_differentOrder() {
        assertTrue(FooCollections.equivalent(listOf(1, 2, 3), listOf(3, 1, 2)))
    }

    @Test fun equivalent_bothEmpty() {
        assertTrue(FooCollections.equivalent(emptyList<Int>(), emptyList<Int>()))
    }

    @Test fun equivalent_differentElements() {
        assertFalse(FooCollections.equivalent(listOf(1, 2, 3), listOf(1, 2, 4)))
    }

    @Test fun equivalent_differentSizes() {
        assertFalse(FooCollections.equivalent(listOf(1, 2), listOf(1, 2, 3)))
    }

    @Test fun equivalent_subsetNotEquivalent() {
        assertFalse(FooCollections.equivalent(listOf(1, 2, 3), listOf(1, 2)))
    }

    // hashCode

    @Test fun hashCode_emptyCollection_isZero() {
        val result = FooCollections.hashCode(emptyList<Int>())
        assertTrue(result == 0)
    }

    @Test fun hashCode_sameCollections_sameHash() {
        val h1 = FooCollections.hashCode(listOf(1, 2, 3))
        val h2 = FooCollections.hashCode(listOf(1, 2, 3))
        assertTrue(h1 == h2)
    }

    @Test fun hashCode_differentOrder_differentHash() {
        val h1 = FooCollections.hashCode(listOf(1, 2, 3))
        val h2 = FooCollections.hashCode(listOf(3, 2, 1))
        assertNotEquals(h1, h2)
    }
}
