package com.smartfoo.android.core.platform

import android.app.Service
import android.content.Context
import android.content.Intent
import com.smartfoo.android.core.FooRun

object FooService {
    fun startService(context: Context, intent: Intent): Boolean {
        val componentName = context.startService(intent)
        val started = (componentName != null)
        return started
    }

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
