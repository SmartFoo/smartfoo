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

    private FooNotificationListenerManager mNotificationListenerManager;
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

        mNotificationListenerManager = FooNotificationListenerManager.getInstance();

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
        onNotificationAccessSettingConfirmedEnabled();
        return result;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        boolean result = super.onUnbind(intent);
        onNotificationAccessSettingConfirmedDisabled();
        return result;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        onNotificationAccessSettingConfirmedDisabled();
    }

    private void onNotificationAccessSettingConfirmedEnabled()
    {
        mNotificationListenerManager.onNotificationAccessSettingConfirmedEnabled(this);
    }

    private void onNotificationAccessSettingConfirmedDisabled()
    {
        mNotificationListenerManager.onNotificationAccessSettingDisabled(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        mNotificationListenerManager.onNotificationPosted(this, sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        mNotificationListenerManager.onNotificationRemoved(this, sbn);
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
