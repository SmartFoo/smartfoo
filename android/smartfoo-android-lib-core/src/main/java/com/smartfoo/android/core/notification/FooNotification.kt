package com.smartfoo.android.core.notification

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.smartfoo.android.core.FooReflection
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.platform.FooPlatformUtils.fromNotificationManager
import kotlin.reflect.KClass

import com.smartfoo.android.core.notification.FooNotificationService.Companion.showNotification

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
        private val TAG = FooLog.TAG(FooNotification::class)

        private val cancelReasonMap by lazy {
            FooReflection.mapConstants(NotificationListenerService::class, "REASON_")
        }

        /**
         * Returns a symbolic name for the given notification cancel reason.
         *
         * @param reason a [android.service.notification.NotificationListenerService].REASON_* constant
         * @return a string such as `"REASON_CLICK(1)"`
         */
        @JvmStatic
        fun notificationCancelReasonToString(reason: Int) =
            FooReflection.toString(cancelReasonMap, reason)

        private val hintsMaps by lazy {
            FooReflection.mapConstants(NotificationListenerService::class, "HINT_")
        }

        /**
         * Returns a flags-style symbolic representation for the given listener hints bitmask.
         *
         * @param hints a bitmask of [android.service.notification.NotificationListenerService].HINT_* constants
         * @return a string such as `"HINT_HOST_DISABLE_CALL_EFFECTS(2)|HINT_HOST_DISABLE_NOTIFICATION_EFFECTS(1)"`
         */
        @JvmStatic
        fun notificationHintsToString(hints: Int) =
            FooReflection.toString(hintsMaps, hints, true)

        private val interruptionFilterMap by lazy {
            FooReflection.mapConstants(NotificationListenerService::class, "INTERRUPTION_FILTER_")
        }

        /**
         * Returns a symbolic name for the given notification interruption filter value.
         *
         * @param filter a [android.service.notification.NotificationListenerService].INTERRUPTION_FILTER_* constant
         * @return a string such as `"INTERRUPTION_FILTER_ALL(1)"`
         */
        @JvmStatic
        fun notificationInterruptionFilterToString(filter: Int) =
            FooReflection.toString(interruptionFilterMap, filter)

        /**
         * Returns an [Intent] that opens the system per-app notification settings page for the
         * calling app's package.
         *
         * @param context the context whose package name is used
         * @return an intent targeting [Settings.ACTION_APP_NOTIFICATION_SETTINGS]
         */
        @JvmStatic
        fun intentAppNotificationSettings(context: Context) =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

        /**
         * Returns true if the [android.Manifest.permission.POST_NOTIFICATIONS] runtime
         * permission has been granted to the calling app.
         *
         * @param context the context
         * @return true if the permission is granted
         */
        @JvmStatic
        fun isPostNotificationsPermissionGranted(context: Context) =
            ContextCompat
                .checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED

        /**
         * Non-hidden duplicate of [android.app.Notification.FLAG_NO_DISMISS]
         */
        @Suppress("KDocUnresolvedReference")
        const val FLAG_NO_DISMISS = 0x00002000

        /**
         * Returns true if all bits in [flags] are set in [notification]'s flags field.
         *
         * @param notification the notification to test; returns false if null
         * @param flags        the bitmask to check
         * @return true if all given flags are set
         */
        @JvmStatic
        fun hasFlags(notification: Notification?, flags: Int) =
            notification != null && (notification.flags and flags) != 0

        /**
         * Similar to [androidx.core.app.NotificationCompat.getOngoing]
         */
        @JvmStatic
        fun getNoDismiss(notification: Notification?) = hasFlags(notification, FLAG_NO_DISMISS)

        /**
         * Searches the calling app's active notifications for one matching [notificationId].
         *
         * @param context        the context
         * @param notificationId the notification ID to look for
         * @return the matching [Notification], or null if not found or the manager is unavailable
         */
        @JvmStatic
        fun findCallingAppNotification(
            context: Context,
            notificationId: Int,
        ): Notification? {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (notificationManager != null) {
                val activeNotifications = notificationManager.activeNotifications
                if (activeNotifications != null) {
                    for (statusBarNotification in activeNotifications) {
                        if (statusBarNotification.id == notificationId) {
                            return statusBarNotification.notification
                        }
                    }
                }
            }
            return null
        }

        /**
         * NOTE: Since Android 14 (API34) [androidx.core.app.NotificationCompat.Builder.setOngoing]
         * notifications **CAN** be dismissed by the user...
         *
         * ...unless...
         *
         * [https://www.reddit.com/r/tasker/comments/1fv9ez4/how_to_enable_nondismissible_persistent/](https://www.reddit.com/r/tasker/comments/1fv9ez4/how_to_enable_nondismissible_persistent/)
         *
         * (There are lots of goodies in this article that might be of some help in the future.)
         *
         * To enable:
         *
         * `adb shell appops set --uid ${packageName} SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS allow`
         *
         * This will add a `android.app.Notification.FLAG_NO_DISMISS` to the notification that can be seen with:
         *
         * `adb shell dumpsys notification --noredact | grep ${packageName}`
         *
         * To disable:
         *
         * `adb shell appops set --uid ${packageName} SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS default`
         */
        @JvmStatic
        fun isCallingAppNotificationNoDismiss(context: Context, notificationId: Int, ) =
            getNoDismiss(findCallingAppNotification(context, notificationId))

        /**
         * Needs to be reasonably longer than the app startup time.
         *
         * NOTE1 that the app startup time can be a few seconds when debugging.
         *
         * NOTE2 that this will time out if paused too long at a debug breakpoint while launching.
         */
        @Suppress("ClassName")
        object NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS {
            const val NORMAL: Int = 1500
            const val SLOW: Int = 6000

            fun getRecommendedTimeout(slow: Boolean): Int = if (slow) SLOW else NORMAL
        }

        /**
         * Per hidden field [Settings.Secure] `ENABLED_NOTIFICATION_LISTENERS`
         */
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

        @JvmStatic
        fun isNotificationListenerEnabled(
            context: Context,
            notificationListenerService: NotificationListenerService,
        ) = isNotificationListenerEnabled(context, notificationListenerService.javaClass)

        @JvmStatic
        fun isNotificationListenerEnabled(
            context: Context,
            notificationListenerServiceClass: KClass<out NotificationListenerService>,
        ) = isNotificationListenerEnabled(context, notificationListenerServiceClass.java)

        /**
         * Similar to API27 [NotificationManager.isNotificationListenerAccessGranted],
         * but not limited to "The listener service must belong to the calling app."
         *
         * Similar to calling [androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages]`.contains(context.packageName)`,
         * but not limited to only package names.
         */
        @JvmStatic
        fun isNotificationListenerEnabled(
            context: Context,
            notificationListenerServiceClass: Class<out NotificationListenerService>,
        ): Boolean {
            val notificationListenerServiceLookingFor =
                ComponentName(context, notificationListenerServiceClass)
            //FooLog.d(TAG, "isNotificationListenerEnabled: notificationListenerServiceLookingFor=$notificationListenerServiceLookingFor")

            val notificationListenersString =
                Settings.Secure.getString(context.contentResolver, ENABLED_NOTIFICATION_LISTENERS)
            if (notificationListenersString != null) {
                val notificationListeners =
                    notificationListenersString.split(':').dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                for (i in notificationListeners.indices) {
                    val notificationListener =
                        ComponentName.unflattenFromString(notificationListeners[i])
                    //FooLog.d(TAG, "isNotificationListenerEnabled: notificationListeners[$i]=$notificationListener")
                    if (notificationListenerServiceLookingFor == notificationListener) {
                        //FooLog.i(TAG, "isNotificationListenerEnabled: found match; return true")
                        return true
                    }
                }
            }

            FooLog.w(TAG, "isNotificationListenerEnabled: found NO match; return false")
            return false
        }


        @JvmStatic
        val intentNotificationListenerSettings
            get() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

        /**
         * Deep-link to the system Notification Listener settings panel.
         * This is the only way to grant/revoke listener access — there is no
         * runtime dialog for it.
         */
        @JvmStatic
        fun startActivityNotificationListenerSettings(context: Context) =
            context.startActivity(
                intentNotificationListenerSettings
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )

        /**
         * Deep-link to the per-app notification settings page.
         * Useful as a secondary action when POST_NOTIFICATIONS is granted
         * but the user has manually disabled notification channels.
         */
        @JvmStatic
        fun startActivityAppNotificationSettings(context: Context) =
            context.startActivity(
                intentAppNotificationSettings(context)
                    .fromNotificationManager()
            )

        @JvmStatic
        fun requestNotificationListenerUnbind(
            context: Context,
            notificationListenerServiceClass: KClass<out NotificationListenerService>,
        ) = requestNotificationListenerUnbind(context, notificationListenerServiceClass.java)

        @JvmStatic
        fun requestNotificationListenerUnbind(
            context: Context,
            notificationListenerServiceClass: Class<out NotificationListenerService>,
        ) {
            runCatching {
                val componentName = ComponentName(context, notificationListenerServiceClass)
                FooLog.v(
                    TAG,
                    "requestNotificationListenerUnbind: +NotificationListenerService.requestUnbind($componentName)"
                )
                NotificationListenerService.requestUnbind(componentName)
                FooLog.v(
                    TAG,
                    "requestNotificationListenerUnbind: -NotificationListenerService.requestUnbind($componentName)"
                )
            }.onFailure { throwable ->
                FooLog.w(TAG, "requestNotificationListenerUnbind: failed", throwable)
            }
        }

        @JvmStatic
        fun requestNotificationListenerRebind(
            context: Context,
            notificationListenerServiceClass: KClass<out NotificationListenerService>,
        ) = requestNotificationListenerRebind(context, notificationListenerServiceClass.java)

        @JvmStatic
        fun requestNotificationListenerRebind(
            context: Context,
            notificationListenerServiceClass: Class<out NotificationListenerService>,
        ) {
            runCatching {
                val componentName = ComponentName(context, notificationListenerServiceClass)
                FooLog.v(
                    TAG,
                    "requestNotificationListenerRebind: +NotificationListenerService.requestRebind($componentName)"
                )
                NotificationListenerService.requestRebind(componentName)
                FooLog.v(
                    TAG,
                    "requestNotificationListenerRebind: -NotificationListenerService.requestRebind($componentName)"
                )
            }.onFailure { throwable ->
                FooLog.w(TAG, "requestNotificationListenerRebind: failed", throwable)
            }
        }

        /**
         * Non-deprecated duplicate of [android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE].
         */
        const val FOREGROUND_SERVICE_TYPE_NONE = 0

        /**
         * Returns the [NotificationManagerCompat] for the given context.
         *
         * @param context the context
         * @return the notification manager compat instance
         */
        fun getNotificationManager(context: Context): NotificationManagerCompat {
            return NotificationManagerCompat.from(context)
        }

        data class ChannelInfo(
            val id: String,
            val name: String,
            val importance: Int,
            val description: String
        )

        /**
         * Creates (or updates) a notification channel described by [channelInfo].
         *
         * @param context     the context
         * @param channelInfo the channel metadata (id, name, importance, description)
         */
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

        /**
         * Returns the [NotificationChannel] associated with [notification]'s channel ID, or null
         * if the notification has no channel or the channel does not exist.
         *
         * @param context      the context
         * @param notification the notification whose channel to look up
         * @return the [NotificationChannel], or null
         */
        @JvmStatic
        fun getNotificationChannel(
            context: Context,
            notification: Notification,
        ): NotificationChannel? {
            val channelId = notification.channelId ?: return null
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.getNotificationChannel(channelId)
        }

        @JvmStatic
        @JvmOverloads
        fun toString(
            sbn: StatusBarNotification?,
            showAllExtras: Boolean = false,
        ): String {
            val notification = sbn?.notification ?: return "null"
            val extras = notification.extras.deepCopy()
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            var text = extras.getCharSequence(Notification.EXTRA_TEXT)
            if (text != null) {
                text =
                    if (text.length > 33) {
                        "(${text.length})${
                            FooString.quote(text.substring(0, 32))
                                .replaceAfterLast("\"", "…\"")
                        }"
                    } else {
                        FooString.quote(text)
                    }
            }
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

            val sb = StringBuilder("{ ")
            sb.append("packageName=${FooString.quote(sbn.packageName)}")
            sb.append(", key=${FooString.quote(sbn.key)}")
            sb.append(", id=${sbn.id}")
            if (showAllExtras) {
                sb.append(", extras={")
                if (title != null) {
                    sb.append("${Notification.EXTRA_TITLE}=${FooString.quote(title)}")
                }
                if (text != null) {
                    sb.append(", ${Notification.EXTRA_TEXT}=${FooString.quote(text)}")
                }
                if (subText != null) {
                    sb.append(", ${Notification.EXTRA_SUB_TEXT}=${FooString.quote(subText)}")
                }
                if (extras != null) {
                    extras.remove(Notification.EXTRA_TITLE)
                    extras.remove(Notification.EXTRA_TEXT)
                    extras.remove(Notification.EXTRA_SUB_TEXT)
                }
                sb.append(FooString.toString(extras))
                sb.append("}")
            }
            sb.append(", notification={ $notification } }")
            return sb.toString()
        }

        @JvmStatic
        fun toString(ranking: Ranking) = "{key=${ranking.key}, rank=${ranking.rank}}"

        @Suppress("KotlinConstantConditions")
        @JvmStatic
        fun toString(rankingMap: RankingMap?): String {
            val level = 0
            if (rankingMap == null) {
                return "null"
            }
            val sb = StringBuilder()
            if (level > 0) {
                sb.append("RankingMap(")
                var first = true
                val ranking = Ranking()
                for (key in rankingMap.orderedKeys) {
                    if (first) {
                        first = false
                    } else {
                        sb.append(", ")
                    }
                    sb.append(FooString.quote(key)).append("=")
                    if (rankingMap.getRanking(key, ranking)) {
                        when (level) {
                            1 -> sb.append("…")
                            2 -> sb.append(ranking.rank)
                        }
                    } else {
                        sb.append("null")
                    }
                }
                sb.append(")")
            } else {
                sb.append("…")
            }
            return sb.toString()
        }

        @JvmStatic
        fun toString(action: Notification.Action?): String {
            if (action == null) {
                return "null"
            }
            val parts = mutableListOf<String>()
            action.title?.let { parts.add("title=${FooString.quote(it)}") }
            action.actionIntent?.let { parts.add("intent=${FooString.repr(it)}") }
            action.getIcon()?.let { parts.add("icon=${it}") }
            action.extras.let { if (!it.isEmpty()) parts.add("extras=${FooString.repr(it)}") }
            action.allowGeneratedReplies.let { if (it) parts.add("allowGeneratingReplies=${it}") }
            action.remoteInputs?.let {
                if (!it.isEmpty()) parts.add(
                    "remoteInputs=${
                        FooString.toString(
                            it
                        )
                    }"
                )
            }
            action.semanticAction.let { if (it > 0) parts.add("semanticAction=${it}") }
            action.isContextual.let { if (it) parts.add("isContextual=${it}") }
            action.dataOnlyRemoteInputs?.let {
                if (!it.isEmpty()) parts.add(
                    "dataOnlyRemoteInputs=${
                        FooString.toString(
                            it
                        )
                    }"
                )
            }
            action.isAuthenticationRequired.let { if (it) parts.add("isAuthenticationRequired=${it}") }
            return "Action(${parts.joinToString(", ")})"
        }

        @JvmStatic
        fun toString(message: NotificationCompat.MessagingStyle.Message?): String {
            if (message == null) {
                return "null"
            }
            val parts = mutableListOf<String>()
            message.timestamp.let { parts.add("timestamp=${it}") }
            message.person?.let { parts.add("person=${toString(it)}") }
            message.dataMimeType?.let { parts.add("dataMimeType=${FooString.quote(it)}") }
            message.dataUri?.let { parts.add("dataUri=${FooString.repr(it)}") }
            message.extras.let { if (!it.isEmpty()) parts.add("extras=${FooString.repr(it)}") }
            message.text?.let { parts.add("text=${FooString.quote(it)}") }
            return "Message(${parts.joinToString(", ")})"
        }

        /**
         * TODO: This is for androidx.core.app.Person; Should we make an overload for android.app.Person?
         */
        @JvmStatic
        fun toString(person: Person?): String {
            if (person == null) {
                return "null"
            }
            val parts = mutableListOf<String>()
            person.name?.let { parts.add("name=${FooString.quote(it)}") }
            person.icon?.let { parts.add("icon=${it}") }
            person.uri?.let { parts.add("uri=${FooString.quote(it)}") }
            person.key?.let { parts.add("key=${FooString.quote(it)}") }
            person.isBot.let { if (it) parts.add("isBot=${it}") }
            person.isImportant.let { if (it) parts.add("isImportant=${it}") }
            return "Person(${parts.joinToString(", ")})"
        }

        fun toString(notification: Notification?) = if (notification == null) "null" else notification.toString()

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
