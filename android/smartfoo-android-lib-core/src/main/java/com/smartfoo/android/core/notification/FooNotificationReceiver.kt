package com.smartfoo.android.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartfoo.android.core.FooListenerManager
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.platform.FooPlatformUtils

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

    interface FooNotificationReceiverCallbacks {
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
