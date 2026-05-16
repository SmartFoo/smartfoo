package com.smartfoo.android.core

import com.smartfoo.android.core.logging.FooLog
import java.util.Collections

/**
 * A thread-safe set of typed listeners supporting safe addition and removal during traversal.
 *
 * Listeners added or removed while [beginTraversing]/[endTraversing] is active are deferred
 * until [endTraversing] is called, preventing [ConcurrentModificationException]. Override
 * [onListenersUpdated] to react when the listener count changes.
 *
 * @param T the listener type; may be nullable
 * @param name a descriptive name used in debug logging
 */
@Suppress("unused")
open class FooListenerManager<T>(name: String) {
    companion object {
        private val TAG = FooLog.TAG(FooListenerManager::class)

        @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions", "RedundantSuppression", "UNREACHABLE_CODE")
        private val VERBOSE_LOG = false && BuildConfig.DEBUG
    }

    constructor(name: Any) : this(FooReflection.getShortClassName(name))

    private val name = FooString.quote(name.trim())
    private val listeners = mutableSetOf<T>()
    private val listenersToAdd = mutableSetOf<T>()
    private val listenersToRemove = mutableSetOf<T>()

    private var isTraversingListeners = false

    override fun toString() = "{ name=$name, size()=${size()} }"

    /**
     * Returns the effective number of listeners, accounting for any pending additions and removals.
     *
     * @return the current listener count
     */
    fun size(): Int {
        val size: Int
        synchronized(listeners) {
            val consolidated: MutableSet<T> = LinkedHashSet(listeners)
            consolidated.addAll(listenersToAdd)
            consolidated.removeAll(listenersToRemove)
            size = consolidated.size
        }
        /*
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " size() == " + size);
        }
        */
        return size
    }

    val isEmpty: Boolean
        get() = size() == 0

    /**
     * Returns true if [listener] is currently registered, pending addition, or pending removal.
     *
     * @param listener the listener to look up
     * @return true if the listener is known to this manager
     */
    fun hasListener(listener: T): Boolean {
        synchronized(listeners) {
            return listenersToAdd.contains(listener) ||
                listeners.contains(listener) ||
                listenersToRemove.contains(listener)
        }
    }

    /**
     * Adds [listener] to the set. If a traversal is in progress the addition is deferred until
     * [endTraversing] is called. Null listeners and duplicate registrations are silently ignored.
     *
     * @param listener the listener to add; no-op if null or already registered
     */
    fun attach(listener: T?) {
        if (VERBOSE_LOG) {
            FooLog.v(TAG, "$name attach(...)")
        }

        if (listener == null) {
            return
        }

        synchronized(listeners) {
            if (hasListener(listener)) {
                return
            }
            if (isTraversingListeners) {
                listenersToAdd.add(listener)
            } else {
                listeners.add(listener)
                updateListeners()
            }
        }
    }

    /**
     * Removes [listener] from the set. If a traversal is in progress the removal is deferred
     * until [endTraversing] is called. Null listeners and unknown listeners are silently ignored.
     *
     * @param listener the listener to remove; no-op if null
     */
    fun detach(listener: T?) {
        if (VERBOSE_LOG) {
            FooLog.v(TAG, "$name detach(...)")
        }

        if (listener == null) {
            return
        }

        synchronized(listeners) {
            if (isTraversingListeners) {
                listenersToRemove.add(listener)
            } else {
                listeners.remove(listener)
                updateListeners()
            }
        }
    }

    /**
     * Removes all listeners. If a traversal is in progress, current listeners are queued for
     * removal and applied when [endTraversing] is called.
     */
    fun clear() {
        if (VERBOSE_LOG) {
            FooLog.v(TAG, "$name clear()")
        }
        synchronized(listeners) {
            listenersToAdd.clear()
            if (isTraversingListeners) {
                listenersToRemove.addAll(listeners)
            } else {
                listeners.clear()
                listenersToRemove.clear()
            }
        }
    }

    /**
     * Marks the start of a traversal and returns an unmodifiable snapshot of the current
     * listener set. Any [attach] or [detach] calls made during traversal are deferred.
     *
     * Must be paired with a corresponding call to [endTraversing].
     *
     * @return an unmodifiable view of the current listeners
     */
    fun beginTraversing(): Set<T> {
        if (VERBOSE_LOG) {
            FooLog.v(TAG, "$name beginTraversing()")
        }
        synchronized(listeners) {
            isTraversingListeners = true
            return Collections.unmodifiableSet(listeners)
        }
    }

    /**
     * Marks the end of a traversal and applies any deferred [attach] or [detach] operations.
     *
     * Must be called after every [beginTraversing] call.
     */
    fun endTraversing() {
        if (VERBOSE_LOG) {
            FooLog.v(TAG, "$name endTraversing()")
        }
        synchronized(listeners) {
            updateListeners()
            isTraversingListeners = false
        }
    }

    private fun updateListeners() {
        if (VERBOSE_LOG) {
            FooLog.v(TAG, "$name updateListeners()")
        }
        synchronized(listeners) {
            var it = listenersToAdd.iterator()
            while (it.hasNext()) {
                listeners.add(it.next())
                it.remove()
            }
            it = listenersToRemove.iterator()
            while (it.hasNext()) {
                listeners.remove(it.next())
                it.remove()
            }
            onListenersUpdated(listeners.size)
        }
    }

    /**
     * Called after the listener set changes (addition, removal, or clear).
     *
     * Override to react to transitions such as the set becoming empty or non-empty.
     *
     * @param listenersSize the new size of the listener set
     */
    protected open fun onListenersUpdated(listenersSize: Int) {
    }
}
