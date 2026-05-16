package com.smartfoo.android.core

/**
 * A [FooListenerManager] that automatically notifies registered
 * [FooListenerAutoStartManagerCallbacks] when the first listener is attached or the last listener
 * is detached.
 *
 * Use this class when a resource or service should start on the first subscriber and stop when
 * there are no more subscribers. Register a [FooListenerAutoStartManagerCallbacks] to receive
 * those lifecycle events and optionally self-detach when the last listener leaves.
 *
 * @param T the type of listener managed by this class
 * @param name a descriptive name used in logging
 */
@Suppress("unused")
class FooListenerAutoStartManager<T>(name: String)
    : FooListenerManager<T?>(name) {
    /**
     * Lifecycle callbacks for auto-start/stop behaviour.
     *
     * Implement this interface and attach it via [FooListenerAutoStartManager.attach] to be
     * notified when the managed listener set transitions from empty to non-empty and back.
     */
    interface FooListenerAutoStartManagerCallbacks {
        /** Called when the first listener is attached to the manager. */
        fun onFirstAttach()

        /**
         * Called when the last listener is detached from the manager.
         *
         * @return true to automatically detach this [FooListenerAutoStartManagerCallbacks] from [FooListenerAutoStartManager.autoStartListeners]
         */
        fun onLastDetach(): Boolean
    }

    private val autoStartListeners = FooListenerManager<FooListenerAutoStartManagerCallbacks>(this)

    private var isStarted = false

    constructor(name: Any) : this(FooReflection.getShortClassName(name))

    /**
     * Registers [callbacks] to receive auto-start/stop lifecycle events.
     *
     * @param callbacks the callbacks to register; no-op if already registered
     */
    fun attach(callbacks: FooListenerAutoStartManagerCallbacks) {
        autoStartListeners.attach(callbacks)
    }

    /**
     * Unregisters previously registered [callbacks].
     *
     * @param callbacks the callbacks to remove; no-op if not registered
     */
    fun detach(callbacks: FooListenerAutoStartManagerCallbacks) {
        autoStartListeners.detach(callbacks)
    }

    override fun onListenersUpdated(listenersSize: Int) {
        super.onListenersUpdated(listenersSize)
        if (isStarted) {
            if (listenersSize == 0) {
                isStarted = false
                for (callbacks in autoStartListeners.beginTraversing()) {
                    @Suppress("ControlFlowWithEmptyBody")
                    if (callbacks.onLastDetach()) {
                        //autoStartListeners.detach(callbacks);
                    }
                }
                autoStartListeners.endTraversing()
            }
        } else {
            if (listenersSize > 0) {
                isStarted = true
                for (callbacks in autoStartListeners.beginTraversing()) {
                    callbacks.onFirstAttach()
                }
                autoStartListeners.endTraversing()
            }
        }
    }
}
