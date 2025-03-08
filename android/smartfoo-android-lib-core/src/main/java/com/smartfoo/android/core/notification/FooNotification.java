package com.smartfoo.android.core.notification;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

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

    /**
     * The activity should also be android:launchMode="singleTask" in AndroidManifest.xml. I'd love to set this
     * programmatically, but I cannot find a way to do it without causing a flicker side-effect.
     *
     * @param context       context
     * @param requestCode   requestCode
     * @param activityClass activityClass
     * @return PendingIntent
     */
    @NonNull
    public static PendingIntent createPendingIntentForActivity(@NonNull Context context, int requestCode,
                                                               @NonNull Class<? extends Activity> activityClass)
    {
        return createPendingIntentForActivity(context, requestCode, activityClass, null);
    }

    /**
     * @param context       context
     * @param requestCode   requestCode
     * @param activityClass activityClass
     * @param extras        extras
     * @return PendingIntent
     */
    @NonNull
    public static PendingIntent createPendingIntentForActivity(@NonNull Context context, int requestCode,
                                                               @NonNull Class<? extends Activity> activityClass,
                                                               Bundle extras)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(activityClass, "activityClass");
        Intent activityIntent = new Intent(context, activityClass);
        if (extras != null)
        {
            activityIntent.putExtras(extras);
        }
        return createPendingIntentForActivity(context, requestCode, activityIntent);
    }

    /**
     * The activity should also be android:launchMode="singleTask" in AndroidManifest.xml. I'd love to set this
     * programmatically, but I cannot find a way to do it without causing a flicker side-effect.
     *
     * @param context        context
     * @param requestCode    requestCode
     * @param activityIntent activityIntent
     * @return PendingIntent
     */
    @NonNull
    public static PendingIntent createPendingIntentForActivity(@NonNull Context context, int requestCode,
                                                               @NonNull Intent activityIntent)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(activityIntent, "activityIntent");
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getActivity(context, requestCode, activityIntent, pendingIntentFlags);
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
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
