package com.smartfoo.android.core.notification;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.NotificationCompat.Extender;
import androidx.core.app.NotificationCompat.Style;

import com.smartfoo.android.core.FooRun;

public class FooNotificationBuilder
{
    private final Context mContext;
    private final Builder mBuilder;

    public FooNotificationBuilder(@NonNull Context context)
    {
        mContext = FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mBuilder = new Builder(context);
    }

    public FooNotificationBuilder setWhen(long when)
    {
        mBuilder.setWhen(when);
        return this;
    }

    public FooNotificationBuilder setShowWhen(boolean show)
    {
        mBuilder.setShowWhen(show);
        return this;
    }

    public FooNotificationBuilder setUsesChronometer(boolean b)
    {
        mBuilder.setUsesChronometer(b);
        return this;
    }

    public FooNotificationBuilder setSmallIcon(@DrawableRes int icon)
    {
        mBuilder.setSmallIcon(icon);
        return this;
    }

    public FooNotificationBuilder setSmallIcon(@DrawableRes int icon, int level)
    {
        mBuilder.setSmallIcon(icon, level);
        return this;
    }

    public FooNotificationBuilder setContentTitle(CharSequence title)
    {
        mBuilder.setContentTitle(title);
        return this;
    }

    public FooNotificationBuilder setContentTitle(@StringRes int title)
    {
        return setContentTitle(getString(title));
    }

    public FooNotificationBuilder setContentText(CharSequence text)
    {
        mBuilder.setContentText(text);
        return this;
    }

    public FooNotificationBuilder setContentText(@StringRes int text)
    {
        return setContentText(getString(text));
    }

    public FooNotificationBuilder setSubText(CharSequence text)
    {
        mBuilder.setSubText(text);
        return this;
    }

    public FooNotificationBuilder setSubText(@StringRes int text)
    {
        return setSubText(getString(text));
    }

    public FooNotificationBuilder setNumber(int number)
    {
        mBuilder.setNumber(number);
        return this;
    }

    public FooNotificationBuilder setContentInfo(CharSequence info)
    {
        mBuilder.setContentInfo(info);
        return this;
    }

    public FooNotificationBuilder setProgress(int max, int progress, boolean indeterminate)
    {
        mBuilder.setProgress(max, progress, indeterminate);
        return this;
    }

    public FooNotificationBuilder setContent(RemoteViews views)
    {
        mBuilder.setContent(views);
        return this;
    }

    public FooNotificationBuilder setContentIntent(PendingIntent pendingIntent)
    {
        mBuilder.setContentIntent(pendingIntent);
        return this;
    }

    public FooNotificationBuilder setContentIntentActivity(int requestCode, Class<? extends Activity> activity)
    {
        return setContentIntent(FooNotification.createPendingIntentForActivity(mContext, requestCode, activity));
    }

    public FooNotificationBuilder setContentIntentBroadcast(int requestCode, Intent intent, int pendingIntentFlags)
    {
        return setContentIntent(PendingIntent.getBroadcast(mContext, requestCode, intent, pendingIntentFlags));
    }

    public FooNotificationBuilder setContentIntentService(int requestCode, Intent intent, int pendingIntentFlags)
    {
        return setContentIntent(PendingIntent.getService(mContext, requestCode, intent, pendingIntentFlags));
    }

    public FooNotificationBuilder setDeleteIntent(PendingIntent pendingIntent)
    {
        mBuilder.setDeleteIntent(pendingIntent);
        return this;
    }

    public FooNotificationBuilder setDeleteIntentActivity(int requestCode, Class<? extends Activity> activity)
    {
        return setDeleteIntent(FooNotification.createPendingIntentForActivity(mContext, requestCode, activity));
    }

    public FooNotificationBuilder setDeleteIntentBroadcast(int requestCode, Intent intent, int pendingIntentFlags)
    {
        return setDeleteIntent(PendingIntent.getBroadcast(mContext, requestCode, intent, pendingIntentFlags));
    }

    public FooNotificationBuilder setDeleteIntentService(int requestCode, Intent intent, int pendingIntentFlags)
    {
        return setDeleteIntent(PendingIntent.getService(mContext, requestCode, intent, pendingIntentFlags));
    }

    private String getString(@StringRes int resId, Object... formatArgs)
    {
        return mContext.getString(resId, formatArgs);
    }

    public FooNotificationBuilder setFullScreenIntent(PendingIntent intent, boolean highPriority)
    {
        mBuilder.setFullScreenIntent(intent, highPriority);
        return this;
    }

    public FooNotificationBuilder setTicker(CharSequence tickerText)
    {
        mBuilder.setTicker(tickerText);
        return this;
    }

    public FooNotificationBuilder setTicker(CharSequence tickerText, RemoteViews views)
    {
        mBuilder.setTicker(tickerText, views);
        return this;
    }

    public FooNotificationBuilder setLargeIcon(Bitmap icon)
    {
        mBuilder.setLargeIcon(icon);
        return this;
    }

    public FooNotificationBuilder setSound(Uri sound)
    {
        mBuilder.setSound(sound);
        return this;
    }

    public FooNotificationBuilder setSound(Uri sound, int streamType)
    {
        mBuilder.setSound(sound, streamType);
        return this;
    }

    public FooNotificationBuilder setVibrate(long[] pattern)
    {
        mBuilder.setVibrate(pattern);
        return this;
    }

    public FooNotificationBuilder setLights(@ColorInt int argb, int onMs, int offMs)
    {
        mBuilder.setLights(argb, onMs, offMs);
        return this;
    }

    public FooNotificationBuilder setOngoing(boolean ongoing)
    {
        mBuilder.setOngoing(ongoing);
        return this;
    }

    public FooNotificationBuilder setOnlyAlertOnce(boolean onlyAlertOnce)
    {
        mBuilder.setOnlyAlertOnce(onlyAlertOnce);
        return this;
    }

    public FooNotificationBuilder setAutoCancel(boolean autoCancel)
    {
        mBuilder.setAutoCancel(autoCancel);
        return this;
    }

    public FooNotificationBuilder setLocalOnly(boolean b)
    {
        mBuilder.setLocalOnly(b);
        return this;
    }

    public FooNotificationBuilder setCategory(String category)
    {
        mBuilder.setCategory(category);
        return this;
    }

    public FooNotificationBuilder setDefaults(int defaults)
    {
        mBuilder.setDefaults(defaults);
        return this;
    }

    public FooNotificationBuilder setPriority(int pri)
    {
        mBuilder.setPriority(pri);
        return this;
    }

    public FooNotificationBuilder addPerson(String uri)
    {
        mBuilder.addPerson(uri);
        return this;
    }

    public FooNotificationBuilder setGroup(String groupKey)
    {
        mBuilder.setGroup(groupKey);
        return this;
    }

    public FooNotificationBuilder setGroupSummary(boolean isGroupSummary)
    {
        mBuilder.setGroupSummary(isGroupSummary);
        return this;
    }

    public FooNotificationBuilder setSortKey(String sortKey)
    {
        mBuilder.setSortKey(sortKey);
        return this;
    }

    public FooNotificationBuilder addExtras(Bundle extras)
    {
        mBuilder.addExtras(extras);
        return this;
    }

    public FooNotificationBuilder setExtras(Bundle extras)
    {
        mBuilder.setExtras(extras);
        return this;
    }

    public Bundle getExtras()
    {
        return mBuilder.getExtras();
    }

    public FooNotificationBuilder addActionActivity(@DrawableRes int icon, @StringRes int title, int requestCode, @NonNull Intent intent, int flags)
    {
        return addActionActivity(icon, getString(title), requestCode, intent, flags);
    }

    public FooNotificationBuilder addActionActivity(@DrawableRes int icon, @NonNull CharSequence title, int requestCode, @NonNull Intent intent, int flags)
    {
        return addAction(icon, title, PendingIntent.getActivity(mContext, requestCode, intent, flags));
    }

    public FooNotificationBuilder addActionBroadcast(@DrawableRes int icon, @StringRes int title, int requestCode, @NonNull Intent intent, int flags)
    {
        return addActionBroadcast(icon, getString(title), requestCode, intent, flags);
    }

    public FooNotificationBuilder addActionBroadcast(@DrawableRes int icon, @NonNull CharSequence title, int requestCode, @NonNull Intent intent, int flags)
    {
        return addAction(icon, title, PendingIntent.getBroadcast(mContext, requestCode, intent, flags));
    }

    public FooNotificationBuilder addActionService(@DrawableRes int icon, @StringRes int title, int requestCode, @NonNull Intent intent, int flags)
    {
        return addActionService(icon, getString(title), requestCode, intent, flags);
    }

    public FooNotificationBuilder addActionService(@DrawableRes int icon, @NonNull CharSequence title, int requestCode, @NonNull Intent intent, int flags)
    {
        return addAction(icon, title, PendingIntent.getService(mContext, requestCode, intent, flags));
    }

    public FooNotificationBuilder addAction(@DrawableRes int icon, @StringRes int title, @NonNull PendingIntent intent)
    {
        return addAction(icon, getString(title), intent);
    }

    public FooNotificationBuilder addAction(@DrawableRes int icon, @NonNull CharSequence title, @NonNull PendingIntent intent)
    {
        mBuilder.addAction(icon, title, intent);
        return this;
    }

    public FooNotificationBuilder addAction(@NonNull Action action)
    {
        mBuilder.addAction(action);
        return this;
    }

    public FooNotificationBuilder setStyle(Style style)
    {
        mBuilder.setStyle(style);
        return this;
    }

    public FooNotificationBuilder setColor(@ColorInt int argb)
    {
        mBuilder.setColor(argb);
        return this;
    }

    public FooNotificationBuilder setVisibility(int visibility)
    {
        mBuilder.setVisibility(visibility);
        return this;
    }

    public FooNotificationBuilder setPublicVersion(Notification n)
    {
        mBuilder.setPublicVersion(n);
        return this;
    }

    public FooNotificationBuilder extend(Extender extender)
    {
        mBuilder.extend(extender);
        return this;
    }

    public Notification build()
    {
        return mBuilder.build();
    }

    /*
    protected BuilderExtender getExtender()
    {
        mBuilder.getExtender();
    }
    */
}
