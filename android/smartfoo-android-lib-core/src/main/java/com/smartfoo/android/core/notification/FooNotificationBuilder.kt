package com.smartfoo.android.core.notification

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.smartfoo.android.core.notification.FooNotification.Companion.createPendingIntentForActivity

@Suppress("unused", "MemberVisibilityCanBePrivate")
class FooNotificationBuilder
    (context: Context, channelId: String) {
    private val mContext = context
    private val mBuilder = NotificationCompat.Builder(context, channelId)

    fun setWhen(`when`: Long): FooNotificationBuilder {
        mBuilder.setWhen(`when`)
        return this
    }

    fun setShowWhen(show: Boolean): FooNotificationBuilder {
        mBuilder.setShowWhen(show)
        return this
    }

    fun setUsesChronometer(b: Boolean): FooNotificationBuilder {
        mBuilder.setUsesChronometer(b)
        return this
    }

    fun setSmallIcon(@DrawableRes icon: Int): FooNotificationBuilder {
        mBuilder.setSmallIcon(icon)
        return this
    }

    fun setSmallIcon(@DrawableRes icon: Int, level: Int): FooNotificationBuilder {
        mBuilder.setSmallIcon(icon, level)
        return this
    }

    fun setContentTitle(title: CharSequence?): FooNotificationBuilder {
        mBuilder.setContentTitle(title)
        return this
    }

    fun setContentTitle(@StringRes title: Int): FooNotificationBuilder {
        return setContentTitle(getString(title))
    }

    fun setContentText(text: CharSequence?): FooNotificationBuilder {
        mBuilder.setContentText(text)
        return this
    }

    fun setContentText(@StringRes text: Int): FooNotificationBuilder {
        return setContentText(getString(text))
    }

    fun setSubText(text: CharSequence?): FooNotificationBuilder {
        mBuilder.setSubText(text)
        return this
    }

    fun setSubText(@StringRes text: Int): FooNotificationBuilder {
        return setSubText(getString(text))
    }

    fun setNumber(number: Int): FooNotificationBuilder {
        mBuilder.setNumber(number)
        return this
    }

    fun setContentInfo(info: CharSequence?): FooNotificationBuilder {
        mBuilder.setContentInfo(info)
        return this
    }

    fun setProgress(max: Int, progress: Int, indeterminate: Boolean): FooNotificationBuilder {
        mBuilder.setProgress(max, progress, indeterminate)
        return this
    }

    fun setContent(views: RemoteViews?): FooNotificationBuilder {
        mBuilder.setContent(views)
        return this
    }

    fun setContentIntent(pendingIntent: PendingIntent?): FooNotificationBuilder {
        mBuilder.setContentIntent(pendingIntent)
        return this
    }

    fun setContentIntentActivity(
        requestCode: Int,
        activity: Class<out Activity?>
    ): FooNotificationBuilder {
        return setContentIntent(createPendingIntentForActivity(mContext, requestCode, activity))
    }

    fun setContentIntentBroadcast(
        requestCode: Int,
        intent: Intent,
        pendingIntentFlags: Int
    ): FooNotificationBuilder {
        return setContentIntent(
            PendingIntent.getBroadcast(
                mContext,
                requestCode,
                intent,
                pendingIntentFlags
            )
        )
    }

    fun setContentIntentService(
        requestCode: Int,
        intent: Intent,
        pendingIntentFlags: Int
    ): FooNotificationBuilder {
        return setContentIntent(
            PendingIntent.getService(
                mContext,
                requestCode,
                intent,
                pendingIntentFlags
            )
        )
    }

    fun setDeleteIntent(pendingIntent: PendingIntent?): FooNotificationBuilder {
        mBuilder.setDeleteIntent(pendingIntent)
        return this
    }

    fun setDeleteIntentActivity(
        requestCode: Int,
        activity: Class<out Activity?>
    ): FooNotificationBuilder {
        return setDeleteIntent(createPendingIntentForActivity(mContext, requestCode, activity))
    }

    fun setDeleteIntentBroadcast(
        requestCode: Int,
        intent: Intent,
        pendingIntentFlags: Int
    ): FooNotificationBuilder {
        return setDeleteIntent(
            PendingIntent.getBroadcast(
                mContext,
                requestCode,
                intent,
                pendingIntentFlags
            )
        )
    }

    fun setDeleteIntentService(
        requestCode: Int,
        intent: Intent,
        pendingIntentFlags: Int
    ): FooNotificationBuilder {
        return setDeleteIntent(
            PendingIntent.getService(
                mContext,
                requestCode,
                intent,
                pendingIntentFlags
            )
        )
    }

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return mContext.getString(resId, *formatArgs)
    }

    fun setFullScreenIntent(intent: PendingIntent?, highPriority: Boolean): FooNotificationBuilder {
        mBuilder.setFullScreenIntent(intent, highPriority)
        return this
    }

    fun setTicker(tickerText: CharSequence?): FooNotificationBuilder {
        mBuilder.setTicker(tickerText)
        return this
    }

    @Deprecated("use {@link #setTicker(CharSequence)}",
        ReplaceWith("setTicker(tickerText, views)",
            "androidx.core.app.NotificationCompat"))
    fun setTicker(tickerText: CharSequence?, views: RemoteViews?): FooNotificationBuilder {
        @Suppress("DEPRECATION")
        mBuilder.setTicker(tickerText, views)
        return this
    }

    fun setLargeIcon(icon: Bitmap?): FooNotificationBuilder {
        mBuilder.setLargeIcon(icon)
        return this
    }

    fun setSound(sound: Uri?): FooNotificationBuilder {
        mBuilder.setSound(sound)
        return this
    }

    fun setSound(sound: Uri?, streamType: Int): FooNotificationBuilder {
        mBuilder.setSound(sound, streamType)
        return this
    }

    fun setVibrate(pattern: LongArray?): FooNotificationBuilder {
        mBuilder.setVibrate(pattern)
        return this
    }

    fun setLights(@ColorInt argb: Int, onMs: Int, offMs: Int): FooNotificationBuilder {
        mBuilder.setLights(argb, onMs, offMs)
        return this
    }

    fun setOngoing(ongoing: Boolean): FooNotificationBuilder {
        mBuilder.setOngoing(ongoing)
        return this
    }

    fun setOnlyAlertOnce(onlyAlertOnce: Boolean): FooNotificationBuilder {
        mBuilder.setOnlyAlertOnce(onlyAlertOnce)
        return this
    }

    fun setAutoCancel(autoCancel: Boolean): FooNotificationBuilder {
        mBuilder.setAutoCancel(autoCancel)
        return this
    }

    fun setLocalOnly(b: Boolean): FooNotificationBuilder {
        mBuilder.setLocalOnly(b)
        return this
    }

    fun setCategory(category: String?): FooNotificationBuilder {
        mBuilder.setCategory(category)
        return this
    }

    fun setDefaults(defaults: Int): FooNotificationBuilder {
        mBuilder.setDefaults(defaults)
        return this
    }

    fun setPriority(pri: Int): FooNotificationBuilder {
        mBuilder.setPriority(pri)
        return this
    }

    @Deprecated("use {@link #addPerson(Person)}",
        ReplaceWith("addPerson(person)",
            "androidx.core.app.Person"))
    fun addPerson(uri: String?): FooNotificationBuilder {
        @Suppress("DEPRECATION")
        mBuilder.addPerson(uri)
        return this
    }

    fun addPerson(person: Person?): FooNotificationBuilder {
        mBuilder.addPerson(person)
        return this
    }

    fun setGroup(groupKey: String?): FooNotificationBuilder {
        mBuilder.setGroup(groupKey)
        return this
    }

    fun setGroupSummary(isGroupSummary: Boolean): FooNotificationBuilder {
        mBuilder.setGroupSummary(isGroupSummary)
        return this
    }

    fun setSortKey(sortKey: String?): FooNotificationBuilder {
        mBuilder.setSortKey(sortKey)
        return this
    }

    fun addExtras(extras: Bundle?): FooNotificationBuilder {
        mBuilder.addExtras(extras)
        return this
    }

    fun setExtras(extras: Bundle?): FooNotificationBuilder {
        mBuilder.setExtras(extras)
        return this
    }

    val extras: Bundle
        get() = mBuilder.extras

    fun addActionActivity(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): FooNotificationBuilder {
        return addActionActivity(icon, getString(title), requestCode, intent, flags)
    }

    fun addActionActivity(
        @DrawableRes icon: Int,
        title: CharSequence,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): FooNotificationBuilder {
        return addAction(
            icon,
            title,
            PendingIntent.getActivity(mContext, requestCode, intent, flags)
        )
    }

    fun addActionBroadcast(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): FooNotificationBuilder {
        return addActionBroadcast(icon, getString(title), requestCode, intent, flags)
    }

    fun addActionBroadcast(
        @DrawableRes icon: Int,
        title: CharSequence,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): FooNotificationBuilder {
        return addAction(
            icon,
            title,
            PendingIntent.getBroadcast(mContext, requestCode, intent, flags)
        )
    }

    fun addActionService(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): FooNotificationBuilder {
        return addActionService(icon, getString(title), requestCode, intent, flags)
    }

    fun addActionService(
        @DrawableRes icon: Int,
        title: CharSequence,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): FooNotificationBuilder {
        return addAction(
            icon,
            title,
            PendingIntent.getService(mContext, requestCode, intent, flags)
        )
    }

    fun addAction(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        intent: PendingIntent
    ): FooNotificationBuilder {
        return addAction(icon, getString(title), intent)
    }

    fun addAction(
        @DrawableRes icon: Int,
        title: CharSequence,
        intent: PendingIntent
    ): FooNotificationBuilder {
        mBuilder.addAction(icon, title, intent)
        return this
    }

    fun addAction(action: NotificationCompat.Action): FooNotificationBuilder {
        mBuilder.addAction(action)
        return this
    }

    fun setStyle(style: NotificationCompat.Style?): FooNotificationBuilder {
        mBuilder.setStyle(style)
        return this
    }

    fun setColor(@ColorInt argb: Int): FooNotificationBuilder {
        mBuilder.setColor(argb)
        return this
    }

    fun setVisibility(visibility: Int): FooNotificationBuilder {
        mBuilder.setVisibility(visibility)
        return this
    }

    fun setPublicVersion(n: Notification?): FooNotificationBuilder {
        mBuilder.setPublicVersion(n)
        return this
    }

    fun extend(extender: NotificationCompat.Extender): FooNotificationBuilder {
        mBuilder.extend(extender)
        return this
    }

    fun build(): Notification {
        return mBuilder.build()
    }

    /*
    protected BuilderExtender getExtender() {
        mBuilder.getExtender();
    }
    */
}
