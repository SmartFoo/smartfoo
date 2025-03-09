package com.smartfoo.android.core.notification;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.platform.FooService;

/**
 * NOTE:(pv) I originally thought that an IntentService might be appropriate here.
 * It is not. An IntentService automatically calls onDestroy after onHandleIntent is finished.
 * This is inappropriate for a service that calls {@link Service#startForeground(int, Notification)}.
 * @noinspection unused
 */
public class FooNotificationService
        extends Service
{
    private static final String TAG = FooLog.TAG(FooNotificationService.class);

    private static final String EXTRA_NOTIFICATION                         = "EXTRA_NOTIFICATION";
    private static final String EXTRA_NOTIFICATION_REQUEST_CODE            = "EXTRA_NOTIFICATION_REQUEST_CODE";
    private static final String EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE = "EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE";

    public static boolean showNotification(
            @NonNull Context context,
            @NonNull FooNotification notification)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(notification, "notification");

        Intent intent = new Intent(context, FooNotificationService.class);
        intent.putExtra(EXTRA_NOTIFICATION, notification);

        return FooService.startService(context, intent);
    }

    public static boolean showNotification(
            @NonNull Context context,
            @NonNull Notification notification,
            int requestCode,
            int foregroundServiceType)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(notification, "notification");

        Intent intent = new Intent(context, FooNotificationService.class);
        intent.putExtra(EXTRA_NOTIFICATION, notification);
        intent.putExtra(EXTRA_NOTIFICATION_REQUEST_CODE, requestCode);
        intent.putExtra(EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE, foregroundServiceType);

        return FooService.startService(context, intent);
    }

    @Override
    public void onCreate()
    {
        FooLog.d(TAG, "+onCreate()");
        //FooLog.s(TAG, FooString.separateCamelCaseWords("onCreate"));
        super.onCreate();
        FooLog.d(TAG, "-onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        try
        {
            FooLog.d(TAG, "+onStartCommand(intent=" + FooPlatformUtils.toString(intent) +
                          ", flags=" + flags +
                          ", startId=" + startId + ')');
            //FooLog.s(TAG, FooString.separateCamelCaseWords("onStartCommand"));

            Notification notification = null;
            int requestCode = -1;
            int foregroundServiceType = -1;

            if (intent != null)
            {
                Bundle extras = intent.getExtras();
                if (extras != null && extras.containsKey(EXTRA_NOTIFICATION))
                {
                    FooNotification fooNotification = extras.getParcelable(EXTRA_NOTIFICATION, FooNotification.class);
                    if (fooNotification != null)
                    {
                        notification = fooNotification.getNotification();
                        requestCode = fooNotification.getRequestCode();
                        foregroundServiceType = fooNotification.getForegroundServiceType();
                    }
                    else
                    {
                        notification = extras.getParcelable(EXTRA_NOTIFICATION, Notification.class);
                        if (notification != null) {
                            requestCode = extras.getInt(EXTRA_NOTIFICATION_REQUEST_CODE);
                            foregroundServiceType = extras.getInt(EXTRA_NOTIFICATION_FOREGROUND_SERVICE_TYPE);
                        }
                    }
                }
            }

            if (notification != null && requestCode != -1 && foregroundServiceType != -1) {
                FooLog.d(TAG, "startForeground(requestCode=" + requestCode + ", notification=" + notification + ", foregroundServiceType=" + foregroundServiceType + ")");
                startForeground(requestCode,
                        notification,
                        foregroundServiceType);
            }

            // NOTE:(pv) I am making the tough choice here of *NOT* wanting the app to restart if it crashes.
            //  Restarting after a crash might seem desirable, but it causes more problems than it solves.
            return START_NOT_STICKY;
        }
        finally
        {
            FooLog.d(TAG, "-onStartCommand(intent=" + FooPlatformUtils.toString(intent) +
                          ", flags=" + flags +
                          ", startId=" + startId + ')');
        }
    }

    @Override
    public void onDestroy()
    {
        FooLog.d(TAG, "+onDestroy()");
        //FooLog.s(TAG, FooString.separateCamelCaseWords("onDestroy"));
        super.onDestroy();
        stopForeground(STOP_FOREGROUND_DETACH);
        FooLog.d(TAG, "-onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        try
        {
            FooLog.d(TAG, "+onBind(...)");
            return null;
        }
        finally
        {
            FooLog.d(TAG, "-onBind(...)");
        }
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        try
        {
            FooLog.d(TAG, "+onUnbind(...)");
            return true;
        }
        finally
        {
            FooLog.d(TAG, "-onUnbind(...)");
        }
    }
}
