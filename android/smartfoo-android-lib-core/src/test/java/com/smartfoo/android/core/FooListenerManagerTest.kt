package com.smartfoo.android.core

import com.smartfoo.android.core.FooTest.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FooListenerManagerTest {

    interface TestListener

    private lateinit var manager: FooListenerManager<TestListener>

    @Before
    fun setup() {
        FooTest.initialize(TestType.Junit)
        manager = FooListenerManager("test")
    }

    @Test fun initiallyEmpty() {
        assertTrue(manager.isEmpty)
        assertEquals(0, manager.size())
    }

    @Test fun attach_increasesSize() {
        val listener = object : TestListener {}
        manager.attach(listener)
        assertEquals(1, manager.size())
        assertFalse(manager.isEmpty)
    }

    @Test fun attach_hasListener() {
        val listener = object : TestListener {}
        manager.attach(listener)
        assertTrue(manager.hasListener(listener))
    }

    @Test fun attach_null_noOp() {
        manager.attach(null)
        assertEquals(0, manager.size())
    }

    @Test fun detach_decreasesSize() {
        val listener = object : TestListener {}
        manager.attach(listener)
        manager.detach(listener)
        assertEquals(0, manager.size())
        assertTrue(manager.isEmpty)
    }

    @Test fun detach_removesFromHasListener() {
        val listener = object : TestListener {}
        manager.attach(listener)
        manager.detach(listener)
        assertFalse(manager.hasListener(listener))
    }

    @Test fun detach_notAttached_noOp() {
        val listener = object : TestListener {}
        manager.detach(listener) // should not throw
        assertEquals(0, manager.size())
    }

    @Test fun detach_null_noOp() {
        manager.detach(null)
        assertEquals(0, manager.size())
    }

    @Test fun attach_sameTwice_notDuplicated() {
        val listener = object : TestListener {}
        manager.attach(listener)
        manager.attach(listener)
        assertEquals(1, manager.size())
    }

    @Test fun clear_emptiesAll() {
        manager.attach(object : TestListener {})
        manager.attach(object : TestListener {})
        manager.clear()
        assertTrue(manager.isEmpty)
        assertEquals(0, manager.size())
    }

    @Test fun beginEndTraversing_visitsAllListeners() {
        val visited = mutableListOf<TestListener>()
        val l1 = object : TestListener {}
        val l2 = object : TestListener {}
        manager.attach(l1)
        manager.attach(l2)

        val snapshot = manager.beginTraversing()
        for (l in snapshot) visited.add(l)
        manager.endTraversing()

        assertEquals(2, visited.size)
        assertTrue(visited.contains(l1))
        assertTrue(visited.contains(l2))
    }

    @Test fun beginEndTraversing_attachDuringTraversal_appliedAfter() {
        val l1 = object : TestListener {}
        val l2 = object : TestListener {}
        manager.attach(l1)

        val snapshot = manager.beginTraversing()
        manager.attach(l2) // deferred until endTraversing
        assertEquals("l2 not yet visible during traversal", 1, snapshot.size)
        manager.endTraversing()

        assertEquals("l2 now applied", 2, manager.size())
        assertTrue(manager.hasListener(l2))
    }

    @Test fun beginEndTraversing_detachDuringTraversal_appliedAfter() {
        val l1 = object : TestListener {}
        manager.attach(l1)

        val snapshot = manager.beginTraversing()
        manager.detach(l1)
        assertTrue("l1 still visible during traversal", snapshot.contains(l1))
        manager.endTraversing()

        assertFalse("l1 removed after endTraversing", manager.hasListener(l1))
        assertEquals(0, manager.size())
    }
}
