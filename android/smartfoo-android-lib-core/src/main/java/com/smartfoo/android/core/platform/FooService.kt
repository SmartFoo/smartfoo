package com.smartfoo.android.core.platform

import android.app.Service
import android.content.Context
import android.content.Intent
import com.smartfoo.android.core.FooRun

/**
 * Utility functions for working with Android [android.app.Service].
 *
 * Provides a null-safe [startService] wrapper and a human-readable [startToString]
 * converter for the `START_*` return codes from [android.app.Service.onStartCommand].
 */
object FooService {
    /**
     * Starts a service and returns whether the system accepted the request.
     *
     * @param context the context from which to start the service
     * @param intent the intent describing the service to start
     * @return true if the system returned a non-null [android.content.ComponentName]
     */
    fun startService(context: Context, intent: Intent): Boolean {
        val componentName = context.startService(intent)
        val started = (componentName != null)
        return started
    }

    /**
     * Returns a human-readable label for a [android.app.Service.onStartCommand] return value.
     *
     * @param start one of the `Service.START_*` constants
     * @return a descriptive name including the numeric value, e.g. `"START_STICKY(2)"`
     */
    fun startToString(start: Int): String {
        return when (start) {
            Service.START_STICKY_COMPATIBILITY -> "START_STICKY_COMPATIBILITY"
            Service.START_STICKY -> "START_STICKY"
            Service.START_NOT_STICKY -> "START_NOT_STICKY"
            Service.START_REDELIVER_INTENT -> "START_REDELIVER_INTENT"
            else -> "UNKNOWN"
        } + '(' + start + ')'
    }
}
