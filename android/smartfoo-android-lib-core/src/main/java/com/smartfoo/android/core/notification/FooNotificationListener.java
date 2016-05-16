package com.smartfoo.android.core.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

import java.util.Set;

@TargetApi(19)
public class FooNotificationListener
        extends NotificationListenerService
        implements RemoteController.OnClientUpdateListener
{
    private static final String TAG = FooLog.TAG(FooNotificationListener.class);

    /**
     * Used to force testing of using a specific OS Version #.
     */
    private static final int VERSION_SDK_INT = VERSION.SDK_INT;

    public static boolean supportsNotificationListenerSettings()
    {
        return VERSION_SDK_INT >= 19;
    }

    @SuppressLint("InlinedApi")
    @TargetApi(19)
    @NonNull
    public static Intent getIntentNotificationListenerSettings()
    {
        final String ACTION_NOTIFICATION_LISTENER_SETTINGS;
        if (VERSION_SDK_INT >= 22)
        {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
        }
        else
        {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
        }

        return new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
    }

    public interface FooNotificationListenerCallbacks
    {
        void onNotificationPosted(StatusBarNotification sbn);

        void onNotificationRemoved(StatusBarNotification sbn);
    }

    private static final FooListenerManager<FooNotificationListenerCallbacks> sListenerManager;

    static
    {
        sListenerManager = new FooListenerManager<>();
    }

    public static void addListener(FooNotificationListenerCallbacks listener)
    {
        sListenerManager.attach(listener);
    }

    public static void removeListener(FooNotificationListenerCallbacks listener)
    {
        sListenerManager.detach(listener);
    }

    public static final String ACTION_BIND_REMOTE_CONTROLLER = "com.smartfoo.android.core.notification.FooNotificationListener.ACTION_BIND_REMOTE_CONTROLLER";

    public class RemoteControllerBinder
            extends Binder
    {
        public FooNotificationListener getService()
        {
            return FooNotificationListener.this;
        }
    }

    private IBinder mRemoteControllerBinder = new RemoteControllerBinder();

    private RemoteController mRemoteController;

    @Override
    public void onCreate()
    {
        FooLog.d(TAG, "+onCreate()");
        super.onCreate();

        Context applicationContext = getApplicationContext();

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
        else
        {
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            return super.onBind(intent);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    public RemoteController getRemoteController()
    {
        return mRemoteController;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        Set<FooNotificationListenerCallbacks> callbacks = sListenerManager.beginTraversing();
        for (FooNotificationListenerCallbacks callback : callbacks)
        {
            callback.onNotificationPosted(sbn);
        }
        sListenerManager.endTraversing();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Set<FooNotificationListenerCallbacks> callbacks = sListenerManager.beginTraversing();
        for (FooNotificationListenerCallbacks callback : callbacks)
        {
            callback.onNotificationRemoved(sbn);
        }
        sListenerManager.endTraversing();
    }

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
