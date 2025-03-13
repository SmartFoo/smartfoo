package com.smartfoo.android.core.notification

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotificationService.Companion.showNotification
import androidx.core.net.toUri

/**
 * References:
 * http://developer.android.com/guide/topics/ui/notifiers/notifications.html
 * http://developer.android.com/design/patterns/notifications.html
 * @noinspection unused
 */
class FooNotification(
    val requestCode: Int,
    val foregroundServiceType: Int = FOREGROUND_SERVICE_TYPE_NONE,
    val notification: Notification)
    : Parcelable {
    companion object {
        private val TAG: String = FooLog.TAG(FooNotification::class.java)

        /**
         * Non-deprecated duplicate of [android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE].
         */
        @JvmField
        var FOREGROUND_SERVICE_TYPE_NONE: Int = 0

        fun getNotificationManager(context: Context): NotificationManagerCompat {
            return NotificationManagerCompat.from(context)
        }

        class ChannelInfo(
            @JvmField val id: String,
            val name: String,
            val importance: Int,
            val description: String
        )

        @JvmStatic
        fun createNotificationChannel(
            context: Context,
            channelInfo: ChannelInfo
        ) {
            val channel =
                NotificationChannel(channelInfo.id, channelInfo.name, channelInfo.importance)
            channel.description = channelInfo.description
            getNotificationManager(context).createNotificationChannel(channel)
        }

        /**
         * The activity should also be android:launchMode="singleTask" in AndroidManifest.xml.
         * I'd love to set this programmatically, but I cannot find a way to do it without causing
         * a flicker side-effect.
         *
         * @param context       context
         * @param requestCode   requestCode
         * @param activityClass activityClass
         * @param extras        extras
         * @return PendingIntent
         */
        @JvmOverloads
        @JvmStatic
        fun createPendingIntentForActivity(
            context: Context, requestCode: Int,
            activityClass: Class<out Activity>,
            extras: Bundle? = null
        ): PendingIntent {
            val activityIntent = Intent(context, activityClass)
            if (extras != null) {
                activityIntent.putExtras(extras)
            }
            return createPendingIntentForActivity(context, requestCode, activityIntent)
        }

        /**
         * The activity should also be android:launchMode="singleTask" in AndroidManifest.xml.
         * I'd love to set this programmatically, but I cannot find a way to do it without causing
         * a flicker side-effect.
         *
         * @param context        context
         * @param requestCode    requestCode
         * @param activityIntent activityIntent
         * @return PendingIntent
         */
        @JvmStatic
        fun createPendingIntentForActivity(
            context: Context, requestCode: Int,
            activityIntent: Intent
        ): PendingIntent {
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntentFlags =
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getActivity(
                context,
                requestCode,
                activityIntent,
                pendingIntentFlags
            )
        }

        @Suppress("unused")
        fun createSoundUri(context: Context, soundResId: Int): Uri {
            return ("android.resource://${context.packageName}/$soundResId").toUri()
        }

        fun toString(notification: Notification?): String {
            if (notification == null) {
                return "null"
            }

            return notification.toString()
        }

        @Suppress("unused")
        @JvmField
        val CREATOR : Parcelable.Creator<FooNotification> =
            object : Parcelable.Creator<FooNotification> {
                override fun createFromParcel(parcel: Parcel): FooNotification {
                    return FooNotification(parcel)
                }

                override fun newArray(size: Int): Array<FooNotification?> {
                    return arrayOfNulls(size)
                }
            }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(requestCode)
        dest.writeInt(foregroundServiceType)
        notification.writeToParcel(dest, flags)
    }

    private constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        Notification.CREATOR.createFromParcel(parcel)
    )

    constructor(
        requestCode: Int,
        foregroundServiceType: Int = FOREGROUND_SERVICE_TYPE_NONE,
        builder: FooNotificationBuilder
    ) : this(requestCode, foregroundServiceType, builder.build())

    @Suppress("unused")
    constructor(
        requestCode: Int,
        foregroundServiceType: Int = FOREGROUND_SERVICE_TYPE_NONE,
        builder: NotificationCompat.Builder
    ) : this(requestCode, foregroundServiceType, builder.build())

    @Suppress("MemberVisibilityCanBePrivate")
    val isOngoing: Boolean
        get() = (notification.flags and Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT

    override fun toString(): String {
        return "{ mRequestCode=" + requestCode +
                ", mForegroundServiceType=" + foregroundServiceType +
                ", mNotification=" + toString(notification) +
                " }"
    }

    /**
     * @param context context
     * @return true if the FooNotificationService was started, otherwise false and no notification.
     * @noinspection UnusedReturnValue
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(context: Context): Boolean {
        if (isOngoing) {
            FooLog.d(
                TAG,
                "show: isOngoing()==true; Sending non-dismissible notification to FooNotificationService: " +
                        notification
            )
            return showNotification(context, this)
        } else {
            FooLog.d(TAG, "show: ongoing==false; Showing dismissible notification: $notification")
            getNotificationManager(context).notify(
                requestCode,
                notification
            )
            return true
        }
    }

    /**
     * @param context context
     * @return true if the FooNotificationService was stopped, otherwise false and no notification.
     * @noinspection UnusedReturnValue
     */
    fun cancel(context: Context): Boolean {
        if (isOngoing) {
            val intent = Intent(
                context,
                FooNotificationService::class.java
            )
            val stopped = context.stopService(intent)
            return stopped
        } else {
            getNotificationManager(context).cancel(requestCode)
            return true
        }
    }
}
