package com.smartfoo.android.core.notification;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooHandler;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.reflection.FooReflectionUtils;

@TargetApi(19)
public class FooNotificationListener
        extends NotificationListenerService
        implements RemoteController.OnClientUpdateListener
{
    private static final String TAG = FooLog.TAG(FooNotificationListener.class);

    public static final String ACTION_BIND_REMOTE_CONTROLLER =
            FooReflectionUtils.getClassName(FooNotificationListener.class) +
            ".ACTION_BIND_REMOTE_CONTROLLER";

    public class RemoteControllerBinder
            extends Binder
    {
        public FooNotificationListener getService()
        {
            return FooNotificationListener.this;
        }
    }

    private FooNotificationListenerManager mListenerManager;
    private FooHandler                     mHandler;
    private RemoteController               mRemoteController;

    private final IBinder mRemoteControllerBinder = new RemoteControllerBinder();

    public RemoteController getRemoteController()
    {
        return mRemoteController;
    }

    @Override
    public void onCreate()
    {
        FooLog.d(TAG, "+onCreate()");
        super.onCreate();

        mListenerManager = FooNotificationListenerManager.getInstance();

        mHandler = new FooHandler();

        resetInitializeActiveNotifications();

        Context applicationContext = getApplicationContext();

        // TODO:(pv) Investigate what non-deprecated API adds/removes and move to it...
        mRemoteController = new RemoteController(applicationContext, this);

        FooLog.d(TAG, "-onCreate()");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        FooLog.d(TAG, "onBind(intent=" + FooPlatformUtils.toString(intent) + ')');

        if (ACTION_BIND_REMOTE_CONTROLLER.equals(intent.getAction()))
        {
            return mRemoteControllerBinder;
        }

        IBinder result = super.onBind(intent);
        onNotificationListenerBound();
        return result;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        boolean result = super.onUnbind(intent);
        onNotificationListenerUnbound();
        return result;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        onNotificationListenerUnbound();
    }

    private void onNotificationListenerBound()
    {
        mListenerManager.onNotificationListenerBound();
        initializeActiveNotifications();
    }

    private void onNotificationListenerUnbound()
    {
        mListenerManager.onNotificationListenerUnbound();
        resetInitializeActiveNotifications();
    }

    private static final int INITIALIZE_ACTIVE_NOTIFICATIONS_MAX_ATTEMPTS = 10;

    private int     mInitializeActiveNotificationsAttempts;
    private int     mInitializeActiveNotificationsDelay;
    private boolean mInitializeActiveNotificationsSuccess;

    private final Runnable mInitializeActiveNotificationsRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                StatusBarNotification[] activeNotifications = getActiveNotifications();
                //FooLog.e(TAG, "initializeActiveNotifications: activeNotifications=" + FooString.toString(activeNotifications));
                if (activeNotifications != null)
                {
                    for (StatusBarNotification sbn : activeNotifications)
                    {
                        onNotificationPosted(sbn);
                    }
                }

                mInitializeActiveNotificationsSuccess = true;

                FooLog.i(TAG, "initializeActiveNotifications: Success after " +
                              mInitializeActiveNotificationsAttempts + " attempts");
            }
            catch (SecurityException e)
            {
                FooLog.w(TAG, "initializeActiveNotifications: EXCEPTION", e);
                initializeActiveNotifications();
            }
        }
    };

    private boolean isInitializeActiveNotificationsSuccess()
    {
        return mInitializeActiveNotificationsSuccess;
    }

    private void initializeActiveNotifications()
    {
        //
        // Hack required to read active notifications immediately after being bound
        //
        if (mInitializeActiveNotificationsAttempts < INITIALIZE_ACTIVE_NOTIFICATIONS_MAX_ATTEMPTS)
        {
            mInitializeActiveNotificationsAttempts++;
            mInitializeActiveNotificationsDelay += 100;

            mHandler.postDelayed(mInitializeActiveNotificationsRunnable, mInitializeActiveNotificationsDelay);
        }
        else
        {
            FooLog.w(TAG, "initializeActiveNotifications: Maximum number of attempts (" +
                          INITIALIZE_ACTIVE_NOTIFICATIONS_MAX_ATTEMPTS + ") reached");
        }
    }

    private void resetInitializeActiveNotifications()
    {
        mHandler.removeCallbacks(mInitializeActiveNotificationsRunnable);
        mInitializeActiveNotificationsAttempts = 0;
        mInitializeActiveNotificationsDelay = 0;
        mInitializeActiveNotificationsSuccess = false;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        mListenerManager.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        mListenerManager.onNotificationRemoved(sbn);
    }

    //
    //
    //

    @Override
    public void onClientChange(boolean clearing)
    {
    }

    @Override
    public void onClientPlaybackStateUpdate(int state)
    {
    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed)
    {
    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags)
    {
    }

    @Override
    public void onClientMetadataUpdate(MetadataEditor metadataEditor)
    {
    }
}
