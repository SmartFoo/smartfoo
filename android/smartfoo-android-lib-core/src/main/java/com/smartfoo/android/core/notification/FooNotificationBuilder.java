package com.smartfoo.android.core.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.Extender;
import android.support.v4.app.NotificationCompat.Style;
import android.widget.RemoteViews;

public class FooNotificationBuilder
        extends Builder
{
    private final Context mContext;

    public FooNotificationBuilder(Context context)
    {
        super(context);
        mContext = context;
    }

    @Override
    public FooNotificationBuilder setWhen(long when)
    {
        return (FooNotificationBuilder) super.setWhen(when);
    }

    @Override
    public FooNotificationBuilder setShowWhen(boolean show)
    {
        return (FooNotificationBuilder) super.setShowWhen(show);
    }

    @Override
    public FooNotificationBuilder setUsesChronometer(boolean b)
    {
        return (FooNotificationBuilder) super.setUsesChronometer(b);
    }

    @Override
    public FooNotificationBuilder setSmallIcon(int icon)
    {
        return (FooNotificationBuilder) super.setSmallIcon(icon);
    }

    @Override
    public FooNotificationBuilder setSmallIcon(int icon, int level)
    {
        return (FooNotificationBuilder) super.setSmallIcon(icon, level);
    }

    @Override
    public FooNotificationBuilder setContentTitle(CharSequence title)
    {
        return (FooNotificationBuilder) super.setContentTitle(title);
    }

    public FooNotificationBuilder setContentTitle(int title)
    {
        return setContentTitle(mContext.getString(title));
    }

    @Override
    public FooNotificationBuilder setContentText(CharSequence text)
    {
        return (FooNotificationBuilder) super.setContentText(text);
    }

    public FooNotificationBuilder setContentText(int text)
    {
        return setContentText(mContext.getString(text));
    }

    @Override
    public FooNotificationBuilder setSubText(CharSequence text)
    {
        return (FooNotificationBuilder) super.setSubText(text);
    }

    public FooNotificationBuilder setSubText(int text)
    {
        return setSubText(mContext.getString(text));
    }

    @Override
    public FooNotificationBuilder setNumber(int number)
    {
        return (FooNotificationBuilder) super.setNumber(number);
    }

    @Override
    public FooNotificationBuilder setContentInfo(CharSequence info)
    {
        return (FooNotificationBuilder) super.setContentInfo(info);
    }

    @Override
    public FooNotificationBuilder setProgress(int max, int progress, boolean indeterminate)
    {
        return (FooNotificationBuilder) super.setProgress(max, progress, indeterminate);
    }

    @Override
    public FooNotificationBuilder setContent(RemoteViews views)
    {
        return (FooNotificationBuilder) super.setContent(views);
    }

    @Override
    public FooNotificationBuilder setContentIntent(PendingIntent intent)
    {
        return (FooNotificationBuilder) super.setContentIntent(intent);
    }

    public FooNotificationBuilder setContentIntent(int requestCode, Intent contentIntent, int flags)
    {
        setContentIntent(PendingIntent.getActivity(mContext, requestCode, contentIntent, flags));
        return this;
    }

    @Override
    public FooNotificationBuilder setDeleteIntent(PendingIntent intent)
    {
        return (FooNotificationBuilder) super.setDeleteIntent(intent);
    }

    public FooNotificationBuilder setDeleteIntent(int requestCode, Intent contentIntent, int flags)
    {
        setDeleteIntent(PendingIntent.getActivity(mContext, requestCode, contentIntent, flags));
        return this;
    }

    @Override
    public FooNotificationBuilder setFullScreenIntent(PendingIntent intent, boolean highPriority)
    {
        return (FooNotificationBuilder) super.setFullScreenIntent(intent, highPriority);
    }

    @Override
    public FooNotificationBuilder setTicker(CharSequence tickerText)
    {
        return (FooNotificationBuilder) super.setTicker(tickerText);
    }

    @Override
    public FooNotificationBuilder setTicker(CharSequence tickerText, RemoteViews views)
    {
        return (FooNotificationBuilder) super.setTicker(tickerText, views);
    }

    @Override
    public FooNotificationBuilder setLargeIcon(Bitmap icon)
    {
        return (FooNotificationBuilder) super.setLargeIcon(icon);
    }

    @Override
    public FooNotificationBuilder setSound(Uri sound)
    {
        return (FooNotificationBuilder) super.setSound(sound);
    }

    @Override
    public FooNotificationBuilder setSound(Uri sound, int streamType)
    {
        return (FooNotificationBuilder) super.setSound(sound, streamType);
    }

    @Override
    public FooNotificationBuilder setVibrate(long[] pattern)
    {
        return (FooNotificationBuilder) super.setVibrate(pattern);
    }

    @Override
    public FooNotificationBuilder setLights(
            @ColorInt
            int argb, int onMs, int offMs)
    {
        return (FooNotificationBuilder) super.setLights(argb, onMs, offMs);
    }

    @Override
    public FooNotificationBuilder setOngoing(boolean ongoing)
    {
        return (FooNotificationBuilder) super.setOngoing(ongoing);
    }

    @Override
    public FooNotificationBuilder setOnlyAlertOnce(boolean onlyAlertOnce)
    {
        return (FooNotificationBuilder) super.setOnlyAlertOnce(onlyAlertOnce);
    }

    @Override
    public FooNotificationBuilder setAutoCancel(boolean autoCancel)
    {
        return (FooNotificationBuilder) super.setAutoCancel(autoCancel);
    }

    @Override
    public FooNotificationBuilder setLocalOnly(boolean b)
    {
        return (FooNotificationBuilder) super.setLocalOnly(b);
    }

    @Override
    public FooNotificationBuilder setCategory(String category)
    {
        return (FooNotificationBuilder) super.setCategory(category);
    }

    @Override
    public FooNotificationBuilder setDefaults(int defaults)
    {
        return (FooNotificationBuilder) super.setDefaults(defaults);
    }

    @Override
    public FooNotificationBuilder setPriority(int pri)
    {
        return (FooNotificationBuilder) super.setPriority(pri);
    }

    @Override
    public FooNotificationBuilder addPerson(String uri)
    {
        return (FooNotificationBuilder) super.addPerson(uri);
    }

    @Override
    public FooNotificationBuilder setGroup(String groupKey)
    {
        return (FooNotificationBuilder) super.setGroup(groupKey);
    }

    @Override
    public FooNotificationBuilder setGroupSummary(boolean isGroupSummary)
    {
        return (FooNotificationBuilder) super.setGroupSummary(isGroupSummary);
    }

    @Override
    public FooNotificationBuilder setSortKey(String sortKey)
    {
        return (FooNotificationBuilder) super.setSortKey(sortKey);
    }

    @Override
    public FooNotificationBuilder addExtras(Bundle extras)
    {
        return (FooNotificationBuilder) super.addExtras(extras);
    }

    @Override
    public FooNotificationBuilder setExtras(Bundle extras)
    {
        return (FooNotificationBuilder) super.setExtras(extras);
    }

    @Override
    public Bundle getExtras()
    {
        return super.getExtras();
    }

    @Override
    public FooNotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent)
    {
        return (FooNotificationBuilder) super.addAction(icon, title, intent);
    }

    @Override
    public FooNotificationBuilder addAction(Action action)
    {
        return (FooNotificationBuilder) super.addAction(action);
    }

    @Override
    public FooNotificationBuilder setStyle(Style style)
    {
        return (FooNotificationBuilder) super.setStyle(style);
    }

    @Override
    public FooNotificationBuilder setColor(
            @ColorInt
            int argb)
    {
        return (FooNotificationBuilder) super.setColor(argb);
    }

    @Override
    public FooNotificationBuilder setVisibility(int visibility)
    {
        return (FooNotificationBuilder) super.setVisibility(visibility);
    }

    @Override
    public FooNotificationBuilder setPublicVersion(Notification n)
    {
        return (FooNotificationBuilder) super.setPublicVersion(n);
    }

    @Override
    public FooNotificationBuilder extend(Extender extender)
    {
        return (FooNotificationBuilder) super.extend(extender);
    }

    @Override
    public Notification build()
    {
        return super.build();
    }

    /*
    @Override
    protected BuilderExtender getExtender()
    {
        return (FooNotificationBuilder) super.getExtender();
    }
    */
}
