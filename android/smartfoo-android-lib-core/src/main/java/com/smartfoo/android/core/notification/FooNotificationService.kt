package com.smartfoo.android.core.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.platform.FooPlatformUtils
import com.smartfoo.android.core.platform.FooService

/**
 * NOTE:(pv) I originally thought that an IntentService might be appropriate here.
 * It is not. An IntentService automatically calls onDestroy after onHandleIntent is finished.
 * This is inappropriate for a service that calls [Service.startForeground].
 * @noinspection unused
 */
class FooNotificationService
    : Service() {
    companion object {
        private val TAG: String = FooLog.TAG(FooNotificationService::class.java)

        private const val EXTRA_NOTIFICATION = "EXTRA_NOTIFICATION"
        private const val EXTRA_NOTIFICATION_REQUEST_CODE = "EXTRA_NOTIFICATION_REQUEST_CODE"
        private const val EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE =
            "EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE"

        @JvmStatic
        fun showNotification(
            context: Context,
            notification: FooNotification
        ): Boolean {
            val intent = Intent(
                context,
                FooNotificationService::class.java
            )
            intent.putExtra(EXTRA_NOTIFICATION, notification)

            return FooService.startService(context, intent)
        }

        @Suppress("unused")
        @JvmStatic
        fun showNotification(
            context: Context,
            notification: Notification,
            requestCode: Int,
            foregroundServiceType: Int
        ): Boolean {
            val intent = Intent(
                context,
                FooNotificationService::class.java
            )
            intent.putExtra(EXTRA_NOTIFICATION, notification)
            intent.putExtra(EXTRA_NOTIFICATION_REQUEST_CODE, requestCode)
            intent.putExtra(EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE, foregroundServiceType)

            return FooService.startService(context, intent)
        }
    }

    override fun onCreate() {
        FooLog.d(TAG, "+onCreate()")
        //FooLog.s(TAG, FooString.separateCamelCaseWords("onCreate"));
        super.onCreate()
        FooLog.d(TAG, "-onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            FooLog.d(
                TAG, "+onStartCommand(intent=" + FooPlatformUtils.toString(intent) +
                        ", flags=" + flags +
                        ", startId=" + startId + ')'
            )

            //FooLog.s(TAG, FooString.separateCamelCaseWords("onStartCommand"));
            var notification: Notification? = null
            var requestCode = -1
            var foregroundServiceType = -1

            if (intent != null) {
                val extras = intent.extras
                if (extras != null && extras.containsKey(EXTRA_NOTIFICATION)) {
                    val fooNotification = extras.getParcelable(
                        EXTRA_NOTIFICATION,
                        FooNotification::class.java
                    )
                    if (fooNotification != null) {
                        notification = fooNotification.notification
                        requestCode = fooNotification.requestCode
                        foregroundServiceType = fooNotification.foregroundServiceType
                    } else {
                        notification = extras.getParcelable(
                            EXTRA_NOTIFICATION,
                            Notification::class.java
                        )
                        if (notification != null) {
                            requestCode = extras.getInt(EXTRA_NOTIFICATION_REQUEST_CODE)
                            foregroundServiceType = extras.getInt(
                                EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE
                            )
                        }
                    }
                }
            }

            if (notification != null && requestCode != -1 && foregroundServiceType != -1) {
                FooLog.d(
                    TAG,
                    "startForeground(requestCode=$requestCode, notification=$notification, foregroundServiceType=$foregroundServiceType)"
                )
                startForeground(
                    requestCode,
                    notification,
                    foregroundServiceType
                )
            }

            // NOTE:(pv) I am making the tough choice here of *NOT* wanting the app to restart if it crashes.
            //  Restarting after a crash might seem desirable, but it causes more problems than it solves.
            return START_NOT_STICKY
        } finally {
            FooLog.d(
                TAG, "-onStartCommand(intent=" + FooPlatformUtils.toString(intent) +
                        ", flags=" + flags +
                        ", startId=" + startId + ')'
            )
        }
    }

    override fun onDestroy() {
        FooLog.d(TAG, "+onDestroy()")
        //FooLog.s(TAG, FooString.separateCamelCaseWords("onDestroy"));
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_DETACH)
        FooLog.d(TAG, "-onDestroy()")
    }

    override fun onBind(intent: Intent): IBinder? {
        try {
            FooLog.d(TAG, "+onBind(...)")
            return null
        } finally {
            FooLog.d(TAG, "-onBind(...)")
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        try {
            FooLog.d(TAG, "+onUnbind(...)")
            return true
        } finally {
            FooLog.d(TAG, "-onUnbind(...)")
        }
    }
}
