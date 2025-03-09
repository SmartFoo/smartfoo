package com.smartfoo.android.core.notification;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
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
 * @noinspection unused
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
     * Non-deprecated duplicate of {@link android.content.pm.ServiceInfo#FOREGROUND_SERVICE_TYPE_NONE}.
     */
    public static int FOREGROUND_SERVICE_TYPE_NONE = 0;

    public static class ChannelInfo {
        public final @NonNull String id;
        public final @NonNull String name;
        public final int importance;
        public final @NonNull String description;

        public ChannelInfo(
                @NonNull String id,
                @NonNull String name,
                int importance,
                @NonNull String description) {
            this.id = id;
            this.name = name;
            this.importance = importance;
            this.description = description;
        }
    }

    public static void createNotificationChannel(
            @NonNull Context context,
            @NonNull ChannelInfo channelInfo) {
        NotificationChannel channel = new NotificationChannel(channelInfo.id, channelInfo.name, channelInfo.importance);
        channel.setDescription(channelInfo.description);
        getNotificationManager(context).createNotificationChannel(channel);
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
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
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

    public static final Creator<FooNotification> CREATOR = new Creator<>()
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

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mRequestCode);
        dest.writeInt(mForegroundServiceType);
        mNotification.writeToParcel(dest, flags);
    }

    private FooNotification(@NonNull Parcel in)
    {
        this(in.readInt(),
                in.readInt(),
                Notification.CREATOR.createFromParcel(in));
    }

    private final int          mRequestCode;
    private final int          mForegroundServiceType;
    private final Notification mNotification;

    public FooNotification(int requestCode, @NonNull FooNotificationBuilder builder)
    {
        this(requestCode, FOREGROUND_SERVICE_TYPE_NONE, builder);
    }

    public FooNotification(int requestCode, int foregroundServiceType, @NonNull FooNotificationBuilder builder)
    {
        this(requestCode, foregroundServiceType, builder.build());
    }

    public FooNotification(int requestCode, @NonNull Builder builder)
    {
        this(requestCode, FOREGROUND_SERVICE_TYPE_NONE, builder.build());
    }

    public FooNotification(int requestCode, int foregroundServiceType, @NonNull Builder builder)
    {
        this(requestCode, foregroundServiceType, builder.build());
    }

    public FooNotification(int requestCode, @NonNull Notification notification)
    {
        this(requestCode, FOREGROUND_SERVICE_TYPE_NONE, notification);
    }

    public FooNotification(int requestCode, int foregroundServiceType, @NonNull Notification notification)
    {
        mRequestCode = requestCode;
        mForegroundServiceType = foregroundServiceType;
        mNotification = notification;
    }

    public int getRequestCode()
    {
        return mRequestCode;
    }

    public int getForegroundServiceType()
    {
        return mForegroundServiceType;
    }

    public Notification getNotification()
    {
        return mNotification;
    }

    public boolean isOngoing()
    {
        return (mNotification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT;
    }

    @NonNull
    @Override
    public String toString()
    {
        return "{ mRequestCode=" + mRequestCode +
               ", mForegroundServiceType=" + mForegroundServiceType +
               ", mNotification=" + toString(mNotification) +
               " }";
    }

    /**
     * @param context context
     * @return true if the FooNotificationService was started, otherwise false and no notification.
     * @noinspection UnusedReturnValue
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public boolean show(Context context)
    {
        if (isOngoing())
        {
            FooLog.d(TAG, "show: isOngoing()==true; Sending non-dismissible notification to FooNotificationService: " +
                          mNotification);
            return FooNotificationService.showNotification(context, this);
        }
        else
        {
            FooLog.d(TAG, "show: ongoing==false; Showing dismissible notification: " + mNotification);
            getNotificationManager(context).notify(mRequestCode, mNotification);
            return true;
        }
    }

    /**
     * @param context context
     * @return true if the FooNotificationService was stopped, otherwise false and no notification.
     * @noinspection UnusedReturnValue
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
