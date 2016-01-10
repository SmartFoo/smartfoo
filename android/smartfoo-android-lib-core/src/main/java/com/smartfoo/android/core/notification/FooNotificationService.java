package com.smartfoo.android.core.notification;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.smartfoo.android.core.logging.FooLog;

/**
 * NOTE:(pv) I originally thought that an IntentService might be appropriate here.
 * It is not. An IntentService automatically calls onDestroy after onHandleIntent is finished.
 * This is inappropriate for a service that calls {@link Service#startForeground(int, Notification)}.
 */
public class FooNotificationService
        extends Service
{
    private static final String TAG = FooLog.TAG("FooNotificationService");

    private static final String EXTRA_NOTIFICATION_REQUEST_CODE = "EXTRA_NOTIFICATION_REQUEST_CODE";
    private static final String EXTRA_NOTIFICATION              = "EXTRA_NOTIFICATION";

    public static boolean showNotification(Context context, FooNotification notification)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        if (notification == null)
        {
            throw new IllegalArgumentException("notification must not be null");
        }

        Intent intent = new Intent(context, FooNotificationService.class);
        intent.putExtra(EXTRA_NOTIFICATION, notification);

        return startService(context, intent);
    }

    public static boolean showNotification(Context context, int requestCode, Notification notification)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        if (notification == null)
        {
            throw new IllegalArgumentException("notification must not be null");
        }

        Intent intent = new Intent(context, FooNotificationService.class);
        intent.putExtra(EXTRA_NOTIFICATION_REQUEST_CODE, requestCode);
        intent.putExtra(EXTRA_NOTIFICATION, notification);

        return startService(context, intent);
    }

    private static boolean startService(Context context, Intent intent)
    {
        ComponentName componentName = context.startService(intent);

        //noinspection UnnecessaryLocalVariable
        boolean started = (componentName != null);

        return started;
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
            FooLog.d(TAG, "+onStartCommand(intent=" + PlatformUtils.toString(intent) + ", flags=" + flags +
                          ", startId=" + startId + ")");
            //FooLog.s(TAG, FooString.separateCamelCaseWords("onStartCommand"));
            if (intent != null)
            {
                Bundle extras = intent.getExtras();
                if (extras != null)
                {
                    if (extras.containsKey(EXTRA_NOTIFICATION))
                    {
                        Object temp = extras.getParcelable(EXTRA_NOTIFICATION);
                        if (temp instanceof FooNotification)
                        {
                            FooNotification notification = (FooNotification) temp;
                            startForeground(notification.getRequestCode(), notification.getNotification());
                        }
                        else if (temp instanceof Notification)
                        {
                            Notification notification = (Notification) temp;

                            if (extras.containsKey(EXTRA_NOTIFICATION_REQUEST_CODE))
                            {
                                int requestCode = extras.getInt(EXTRA_NOTIFICATION_REQUEST_CODE);
                                startForeground(requestCode, notification);
                            }
                        }
                    }
                }
            }
            // NOTE:(pv) I am making the tough choice here of *NOT* wanting the app to restart if it crashes.
            //  Restarting after a crash might seem desirable, but it causes more problems than it solves.
            return START_NOT_STICKY;
        }
        finally
        {
            FooLog.d(TAG, "-onStartCommand(intent=" + PlatformUtils.toString(intent) + ", flags=" + flags +
                          ", startId=" + startId + ")");
        }
    }

    @Override
    public void onDestroy()
    {
        FooLog.d(TAG, "+onDestroy()");
        //FooLog.s(TAG, FooString.separateCamelCaseWords("onDestroy"));
        super.onDestroy();
        stopForeground(true);
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
