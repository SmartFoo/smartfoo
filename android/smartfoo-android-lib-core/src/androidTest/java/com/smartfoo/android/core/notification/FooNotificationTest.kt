package com.smartfoo.android.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FooNotificationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val channelId = "com.smartfoo.android.core.test.channel"

    @Before
    fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)!!
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Test", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(block: NotificationCompat.Builder.() -> Unit = {}): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .apply(block)
            .build()
    }

    // hasFlags

    @Test fun hasFlags_null_false() {
        assertFalse(FooNotification.hasFlags(null, Notification.FLAG_ONGOING_EVENT))
    }

    @Test fun hasFlags_flagNotSet_false() {
        val n = buildNotification()
        assertFalse(FooNotification.hasFlags(n, FooNotification.FLAG_NO_DISMISS))
    }

    @Test fun hasFlags_ongoingSet_true() {
        val n = buildNotification { setOngoing(true) }
        assertTrue(FooNotification.hasFlags(n, Notification.FLAG_ONGOING_EVENT))
    }

    @Test fun hasFlags_multipleFlagsMatched() {
        val n = buildNotification { setOngoing(true) }
        assertTrue(FooNotification.hasFlags(n, Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR))
    }

    // getNoDismiss

    @Test fun getNoDismiss_null_false() {
        assertFalse(FooNotification.getNoDismiss(null))
    }

    @Test fun getNoDismiss_withoutFlag_false() {
        val n = buildNotification()
        assertFalse(FooNotification.getNoDismiss(n))
    }

    @Test fun getNoDismiss_withFlag_true() {
        val n = buildNotification()
        n.flags = n.flags or FooNotification.FLAG_NO_DISMISS
        assertTrue(FooNotification.getNoDismiss(n))
    }

    // FooNotification instance — isOngoing via NotificationCompat

    @Test fun instance_ongoing_true() {
        val n = buildNotification { setOngoing(true) }
        val fooN = FooNotification(requestCode = 1, notification = n)
        assertTrue(NotificationCompat.getOngoing(fooN.notification))
    }

    @Test fun instance_ongoing_false() {
        val n = buildNotification()
        val fooN = FooNotification(requestCode = 1, notification = n)
        assertFalse(NotificationCompat.getOngoing(fooN.notification))
    }

    // FooNotification.FLAG_NO_DISMISS constant sanity check

    @Test fun flagNoDismiss_nonZero() {
        assertTrue(FooNotification.FLAG_NO_DISMISS != 0)
    }
}
