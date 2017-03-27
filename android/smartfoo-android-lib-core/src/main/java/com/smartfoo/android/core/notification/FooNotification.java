package com.smartfoo.android.core.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;

/**
 * References:
 * http://developer.android.com/guide/topics/ui/notifiers/notifications.html
 * http://developer.android.com/design/patterns/notifications.html
 */
public class FooNotification
        implements Parcelable
{
    private static final String TAG = FooLog.TAG(FooNotification.class);

    @NonNull
    public static NotificationManagerCompat getNotificationManager(@NonNull Context context)
    {
        return NotificationManagerCompat.from(context);
    }

    @NonNull
    public static PendingIntent createPendingIntent(@NonNull Context context, int requestCode, Class<?> intentClass)
    {
        return createPendingIntent(context, requestCode, intentClass, 0, 0, null);
    }

    @NonNull
    public static PendingIntent createPendingIntent(@NonNull Context context, int requestCode, Class<?> intentClass,
                                                    int intentFlags, int pendingIntentFlags, Bundle extras)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        Intent intent = new Intent(context, intentClass);
        if (intentFlags != 0)
        {
            intent.setFlags(intentFlags);
        }
        if (extras != null)
        {
            intent.putExtras(extras);
        }
        return createPendingIntent(context, requestCode, intent, pendingIntentFlags);
    }

    @NonNull
    public static PendingIntent createPendingIntent(@NonNull Context context, int requestCode, Intent intent)
    {
        return createPendingIntent(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @NonNull
    public static PendingIntent createPendingIntent(@NonNull Context context, int requestCode, Intent intent, int pendingIntentFlags)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(intent, "intent");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntentFlags &= ~PendingIntent.FLAG_NO_CREATE;
        pendingIntentFlags |= PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags);
    }

    @NonNull
    public static Uri createSoundUri(@NonNull Context context, int soundResId)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundResId);
    }

    @NonNull
    public static String toString(Notification notification)
    {
        if (notification == null)
        {
            return "null";
        }

        return notification.toString();
    }

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

    public FooNotification(int requestCode, @NonNull FooNotificationBuilder builder)
    {
        this(requestCode, builder.build());
    }

    public FooNotification(int requestCode, @NonNull Builder builder)
    {
        this(requestCode, builder.build());
    }

    public FooNotification(int requestCode, @NonNull Notification notification)
    {
        mRequestCode = requestCode;
        mNotification = notification;
    }

    private FooNotification(@NonNull Parcel in)
    {
        this(in.readInt(), (Notification) in.readParcelable(Notification.class.getClassLoader()));
    }

    public int getRequestCode()
    {
        return mRequestCode;
    }

    public Notification getNotification()
    {
        return mNotification;
    }

    public boolean isOngoing()
    {
        return (mNotification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT;
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
        return "{ mRequestCode=" + mRequestCode +
               ", mNotification=" + toString(mNotification) +
               " }";
    }

    /**
     * @param context context
     * @return true if the FooNotificationService was started, otherwise false and no notification.
     */
    public boolean show(Context context)
    {
        if (isOngoing())
        {
            FooLog.d(TAG, "show: isOngoing()==true; Sending non-dismissable notification to FooNotificationService: " +
                          mNotification);
            return FooNotificationService.showNotification(context, this);
        }
        else
        {
            FooLog.d(TAG, "show: ongoing==false; Showing dismissable notification: " + mNotification);
            getNotificationManager(context).notify(mRequestCode, mNotification);
            return true;
        }
    }

    /**
     * @param context context
     * @return true if the FooNotificationService was stopped, otherwise false and no notification.
     */
    public boolean cancel(Context context)
    {
        if (isOngoing())
        {
            Intent intent = new Intent(context, FooNotificationService.class);
            //noinspection UnnecessaryLocalVariable
            boolean stopped = context.stopService(intent);
            return stopped;
        }
        else
        {
            getNotificationManager(context).cancel(mRequestCode);
            return true;
        }
    }
}
