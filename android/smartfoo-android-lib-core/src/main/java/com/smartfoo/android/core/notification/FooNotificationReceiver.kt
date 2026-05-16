package com.smartfoo.android.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartfoo.android.core.FooListenerManager
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.platform.FooPlatformUtils

/**
 * [BroadcastReceiver] that dispatches received intents to registered [FooNotificationReceiverCallbacks] listeners.
 *
 * Register this receiver in the manifest for the desired intent actions, then call
 * [attach] / [detach] to subscribe listener objects. All matched listeners are
 * notified via [FooNotificationReceiverCallbacks.onFooNotificationReceived] on the
 * main thread when a broadcast is received.
 */
class FooNotificationReceiver
    : BroadcastReceiver() {
    companion object {
        private val TAG: String = FooLog.TAG(FooNotificationReceiver::class.java)

        private val mListenerManager =
            FooListenerManager<FooNotificationReceiverCallbacks>(
                FooNotificationReceiver::class.java
            )

        fun attach(callbacks: FooNotificationReceiverCallbacks) {
            mListenerManager.attach(callbacks)
        }

        fun detach(callbacks: FooNotificationReceiverCallbacks) {
            mListenerManager.detach(callbacks)
        }
    }

    /** Callback interface for [FooNotificationReceiver] broadcast events. */
    interface FooNotificationReceiverCallbacks {
        /**
         * Called when the receiver handles a broadcast intent.
         *
         * @param intent the intent delivered by the system broadcast
         */
        fun onFooNotificationReceived(intent: Intent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            FooLog.i(TAG, "+onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')')
            for (callbacks in mListenerManager.beginTraversing()) {
                callbacks.onFooNotificationReceived(intent)
            }
            mListenerManager.endTraversing()
        } finally {
            FooLog.i(TAG, "-onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')')
        }
    }
}
