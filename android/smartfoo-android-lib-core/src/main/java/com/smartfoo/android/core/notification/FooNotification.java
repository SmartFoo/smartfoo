package com.smartfoo.android.core.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.Style;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

/**
 * References:
 * http://developer.android.com/guide/topics/ui/notifiers/notifications.html
 * http://developer.android.com/design/patterns/notifications.html
 */
public class FooNotification
        implements Parcelable
{
    private static final String TAG = FooLog.TAG("FooNotification");

    public static final Creator<FooNotification> CREATOR = new Creator<FooNotification>()
    {
        public FooNotification createFromParcel(Parcel in)
        {
            return new FooNotification(in);
        }

        public FooNotification[] newArray(int size)
        {
            return new FooNotification[size];
        }
    };

    private int          mRequestCode;
    private Notification mNotification;

    public int getRequestCode()
    {
        return mRequestCode;
    }

    public Notification getNotification()
    {
        return mNotification;
    }

    public FooNotification(Context context, int requestCode,
                           String contentTitle, String contentText,
                           int iconSmall,
                           boolean ongoing,
                           Class<?> intentClass)
    {
        Intent intent = new Intent(context, intentClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent, 0);

        initialize(context, requestCode,
                contentTitle, contentText,
                null,
                iconSmall, null,
                ongoing, true,
                false, -1, false,
                -1, -1,
                pendingIntent, null);
    }

    public FooNotification(Context context, int requestCode,
                           String contentTitle, String contentText,
                           int iconSmall,
                           boolean ongoing,
                           int soundResId, int soundStreamType,
                           Class<?> intentClass)
    {
        Intent intent = new Intent(context, intentClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent, 0);

        initialize(context, requestCode,
                contentTitle, contentText,
                null,
                iconSmall, null,
                ongoing, true,
                false, -1, false,
                soundResId, soundStreamType,
                pendingIntent, null);
    }

    /**
     * @param context       required
     * @param requestCode   required
     * @param contentTitle  required
     * @param contentText   required
     * @param tickerText    null to ignore
     * @param iconSmall     -1 to ignore
     * @param iconLarge     null to ignore
     * @param ongoing       false to make dismissable
     * @param autoCancel    true to cancel the notification if pressed, false to leave the notification if pressed
     * @param indeterminate
     * @param when          -1 to ignore
     * @param chronometer   false to ignore
     * @param pendingIntent null to ignore
     * @param style         null to ignore
     */
    public FooNotification(Context context, int requestCode, //
                           String contentTitle, String contentText, //
                           String tickerText, //
                           int iconSmall, Bitmap iconLarge,
                           boolean ongoing, boolean autoCancel, //
                           boolean indeterminate, long when, boolean chronometer, //
                           int soundResId, int soundStreamType,
                           PendingIntent pendingIntent, Style style)
    {
        initialize(context, requestCode,
                contentTitle, contentText,
                tickerText,
                iconSmall, iconLarge,
                ongoing, autoCancel,
                indeterminate, when, chronometer,
                soundResId, soundStreamType,
                pendingIntent, style);
    }

    private void initialize(Context context, int requestCode, //
                            String contentTitle, String contentText, //
                            String tickerText, //
                            int iconSmall, Bitmap iconLarge,
                            boolean ongoing, boolean autoCancel, //
                            boolean indeterminate, long when, boolean chronometer, //
                            int soundResId, int soundStreamType,
                            PendingIntent pendingIntent, Style style)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context cannot be null");
        }

        Builder builder = new Builder(context) //
                .setContentTitle(contentTitle) //
                .setContentText(contentText) //
                .setOngoing(ongoing) //
                .setAutoCancel(autoCancel);

        if (indeterminate)
        {
            builder.setProgress(0, 0, true);
        }
        if (when != -1)
        {
            builder.setWhen(when);
        }
        builder.setUsesChronometer(chronometer);

        if (!FooString.isNullOrEmpty(tickerText))
        {
            builder.setTicker(tickerText);
        }
        if (iconSmall != -1)
        {
            builder.setSmallIcon(iconSmall);
        }
        if (iconLarge != null)
        {
            builder.setLargeIcon(iconLarge);
        }
        if (soundResId != -1)
        {
            Uri sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundResId);
            if (soundStreamType != -1)
            {
                builder.setSound(sound, soundStreamType);
            }
            else
            {
                builder.setSound(sound);
            }
        }
        if (pendingIntent != null)
        {
            builder.setContentIntent(pendingIntent);
        }
        if (style != null)
        {
            builder.setStyle(style);
        }

        initialize(requestCode, builder);
    }

    private void initialize(int requestCode, Builder builder)
    {
        initialize(requestCode, builder.build());
    }

    private void initialize(int requestCode, Notification notification)
    {
        mRequestCode = requestCode;
        mNotification = notification;
    }

    public boolean isOngoing()
    {
        return (mNotification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT;
    }

    public FooNotification(Parcel in)
    {
        mRequestCode = in.readInt();
        mNotification = in.readParcelable(Notification.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mRequestCode);
        dest.writeParcelable(mNotification, 0);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "{mRequestCode=" + mRequestCode //
               + ", mNotification.tickerText=" + FooString.quote(mNotification.tickerText) //
               + "}";
    }

    /**
     * @return true if the FooNotificationService was started, otherwise false and no notification.
     */
    @SuppressWarnings({ "UnnecessaryLocalVariable", "unused" })
    public boolean show(Context context)
    {
        if (isOngoing())
        {
            FooLog.d(TAG, "show: isOngoing()==true; Sending non-dismissable notification to FooNotificationService: " +
                          mNotification);
            Intent intentService = new Intent(context, FooNotificationService.class);
            intentService.putExtra(FooNotificationService.EXTRA_NOTIFICATION, this);

            ComponentName componentName = context.startService(intentService);
            boolean started = (componentName != null);
            return started;
        }
        else
        {
            FooLog.d(TAG, "showNotification: ongoing==false; Showing dismissable notification: " + mNotification);
            FooNotificationService.getNotificationManager(context).notify(mRequestCode, mNotification);
            return true;
        }
    }

    /**
     * @return true if the FooNotificationService was stopped, otherwise false and no notification.
     */
    @SuppressWarnings({ "UnnecessaryLocalVariable", "unused" })
    public boolean cancel(Context context)
    {
        if (isOngoing())
        {
            Intent intent = new Intent(context, FooNotificationService.class);
            boolean stopped = context.stopService(intent);
            return stopped;
        }
        else
        {
            FooNotificationService.getNotificationManager(context).cancel(mRequestCode);
            return true;
        }
    }
}
