package com.smartfoo.android.core.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
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

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooHandler;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.reflection.FooReflectionUtils;

@TargetApi(18)
public class FooNotificationListenerManager
{
    private static final String TAG = FooLog.TAG(FooNotificationListenerManager.class);

    /**
     * This has been observed to take &gt;500ms when unlocking after a fresh reboot.
     * TODO:(pv) Consider making this two values, one before first unlock, one after first unlock
     */
    public static final int NOTIFICATION_LISTENER_CONNECTED_TIMEOUT_MILLIS = 1000;

    /**
     * Usually {@link VERSION#SDK_INT VERSION.SDK_INT}, but may be used to force a specific OS Version # <b>FOR TESTING
     * PURPOSES ONLY!</b>
     */
    private static final int VERSION_SDK_INT = VERSION.SDK_INT;

    public static boolean supportsNotificationListenerSettings()
    {
        return VERSION_SDK_INT >= 19;
    }

    /**
     * Per hidden field {@link android.provider.Settings.Secure android.provider.Settings.Secure.ENABLED_NOTIFICATION_LISTENERS}
     */
    private static final String ENABLED_NOTIFICATION_LISTENERS = FooReflectionUtils.getFieldValueString(
            android.provider.Settings.Secure.class,
            "ENABLED_NOTIFICATION_LISTENERS");

    public static boolean isNotificationAccessSettingConfirmedNotEnabled(@NonNull Context context)
    {
        return isNotificationAccessSettingConfirmedNotEnabled(context, FooNotificationListener.class);
    }

    public static boolean isNotificationAccessSettingConfirmedNotEnabled(@NonNull Context context,
                                                                         @NonNull Class<? extends NotificationListenerService> notificationListenerServiceClass)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(notificationListenerServiceClass, "notificationListenerServiceClass");

        if (supportsNotificationListenerSettings())
        {
            String packageName = context.getPackageName();
            String notificationListenerServiceClassName = notificationListenerServiceClass.getName();
            String packageNameNotificationListenerServiceClassName =
                    packageName + '/' + notificationListenerServiceClassName;

            ContentResolver contentResolver = context.getContentResolver();

            String notificationListenersString = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS);
            if (notificationListenersString != null)
            {
                String[] notificationListeners = notificationListenersString.split(":");
                for (String notificationListener : notificationListeners)
                {
                    FooLog.v(TAG, "isNotificationAccessSettingConfirmedNotEnabled: notificationListener == " +
                                  FooString.quote(notificationListener));
                    if (notificationListener.equals(packageNameNotificationListenerServiceClassName))
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @SuppressLint("InlinedApi")
    public static Intent getIntentNotificationListenerSettings()
    {
        Intent intent = null;
        if (supportsNotificationListenerSettings())
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
            intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    public static void startActivityNotificationListenerSettings(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        context.startActivity(getIntentNotificationListenerSettings());
    }

    public enum NotConnectedReason
    {
        ConfirmedNotEnabled,
        ConnectedTimeout,
        Disconnected,
    }

    public interface FooNotificationListenerManagerCallbacks
    {
        /**
         * @param activeNotifications Active StatusBar Notifications
         * @return true to prevent {@link #initializeActiveNotifications()} from being automatically called
         */
        boolean onNotificationListenerConnected(@NonNull StatusBarNotification[] activeNotifications);

        void onNotificationListenerNotConnected(@NonNull NotConnectedReason reason, long elapsedMillis);

        void onNotificationPosted(@NonNull StatusBarNotification sbn);

        void onNotificationRemoved(@NonNull StatusBarNotification sbn);
    }

    public static FooNotificationListenerManager getInstance()
    {
        return sInstance;
    }

    private static FooNotificationListenerManager sInstance = new FooNotificationListenerManager();

    private final Object                                                      mSyncLock;
    private final FooListenerManager<FooNotificationListenerManagerCallbacks> mListenerManager;
    private final FooHandler                                                  mHandler;

    private FooNotificationListener mNotificationListener;

    private FooNotificationListenerManager()
    {
        mSyncLock = new Object();
        mListenerManager = new FooListenerManager<>(this);
        mHandler = new FooHandler();
    }

    public boolean isNotificationListenerConnected()
    {
        return mNotificationListener != null;
    }

    public void attach(@NonNull Context context,
                       @NonNull FooNotificationListenerManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        mListenerManager.attach(callbacks);

        //
        // Purposefully not using FooListenerAutoStartManager due to the following incompatible logic...
        //
        if (isNotificationAccessSettingConfirmedNotEnabled(context))
        {
            callbacks.onNotificationListenerNotConnected(NotConnectedReason.ConfirmedNotEnabled, 0);
        }
        else
        {
            if (mListenerManager.size() == 1)
            {
                if (mNotificationListenerBindTimeoutStartMillis == -1)
                {
                    notificationListenerConnectedTimeoutStart(NOTIFICATION_LISTENER_CONNECTED_TIMEOUT_MILLIS);
                }
            }
        }
    }

    public void detach(@NonNull FooNotificationListenerManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        mListenerManager.detach(callbacks);

        if (mListenerManager.size() == 0)
        {
            notificationListenerConnectedTimeoutStop();
        }
    }

    public StatusBarNotification[] getActiveNotifications()
    {
        synchronized (mSyncLock)
        {
            return mNotificationListener != null ? mNotificationListener.getActiveNotifications() : null;
        }
    }

    public void initializeActiveNotifications()
    {
        initializeActiveNotifications(getActiveNotifications());
    }

    private void initializeActiveNotifications(StatusBarNotification[] activeNotifications)
    {
        if (activeNotifications == null)
        {
            return;
        }

        synchronized (mSyncLock)
        {
            if (mNotificationListener == null)
            {
                return;
            }

            for (StatusBarNotification sbn : activeNotifications)
            {
                onNotificationPosted(mNotificationListener, sbn);
            }
        }
    }

    /**
     * HACK required to detect non-binding when re-installing app even if notification access says/shows it is enabled:
     * http://stackoverflow.com/a/37081128/252308
     * <p>
     * Even if Notification Access is enabled, the Application always starts first, before FooNotificationListener has
     * any chance to bind.
     * After the Application start, if FooNotificationListener binds then it will call onNotificationListenerBound().
     * On first run it will not bind because the user has not enabled the settings.
     * Normally we would just directly call FooNotificationListener.isNotificationAccessSettingEnabled(Context
     * context).
     * Unfortunately, sometimes NotificationAccess is configured to be enabled, but FooNotificationListener never
     * binds.
     * This almost always happens when re-installing the app between development builds.
     * NOTE:(pv) It is unknown if this is also an issue when the app does production updates through Google Play.
     * Since we cannot reliably test for isNotificationAccessSettingEnabled, the next best thing is to timeout if
     * FooNotificationListener does not bind within a small amount of time (we are using 100ms).
     * If FooNotificationListener does not bind and call onNotificationListenerBound() within 250ms then we need to
     * prompt the user to enable Notification Access.
     *
     * @param timeoutMillis
     */
    private void notificationListenerConnectedTimeoutStart(long timeoutMillis)
    {
        FooLog.v(TAG, "+notificationListenerConnectedTimeoutStart(timeoutMillis=" + timeoutMillis + ')');
        if (mNotificationListenerBindTimeoutStartMillis != -1)
        {
            notificationListenerConnectedTimeoutStop();
        }
        mNotificationListenerBindTimeoutStartMillis = System.currentTimeMillis();
        mHandler.postDelayed(mNotificationListenerBindTimeout, timeoutMillis);
        FooLog.v(TAG, "-notificationListenerConnectedTimeoutStart(timeoutMillis=" + timeoutMillis + ')');
    }

    private void notificationListenerConnectedTimeoutStop()
    {
        FooLog.v(TAG, "+notificationListenerConnectedTimeoutStop()");
        mNotificationListenerBindTimeoutStartMillis = -1;
        mHandler.removeCallbacks(mNotificationListenerBindTimeout);
        FooLog.v(TAG, "-notificationListenerConnectedTimeoutStop()");
    }

    private long mNotificationListenerBindTimeoutStartMillis = -1;

    private final Runnable mNotificationListenerBindTimeout = new Runnable()
    {
        @Override
        public void run()
        {
            FooLog.v(TAG, "+mNotificationListenerBindTimeout.run()");
            long elapsedMillis = System.currentTimeMillis() - mNotificationListenerBindTimeoutStartMillis;
            onNotificationListenerNotConnected(NotConnectedReason.ConnectedTimeout, elapsedMillis);
            FooLog.v(TAG, "-mNotificationListenerBindTimeout.run()");
        }
    };

    private void onNotificationListenerConnected(
            @NonNull FooNotificationListener notificationListener,
            @NonNull StatusBarNotification[] activeNotifications)
    {
        synchronized (mSyncLock)
        {
            notificationListenerConnectedTimeoutStop();

            mNotificationListener = notificationListener;

            boolean initializeActiveNotifications = true;
            for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                initializeActiveNotifications &= !callbacks.onNotificationListenerConnected(activeNotifications);
            }
            mListenerManager.endTraversing();

            if (initializeActiveNotifications)
            {
                initializeActiveNotifications(activeNotifications);
            }
        }
    }

    private void onNotificationListenerNotConnected(
            @NonNull NotConnectedReason reason,
            long elapsedMillis)
    {
        FooLog.v(TAG, "+onNotificationListenerNotConnected(reason=" + reason + ')');
        synchronized (mSyncLock)
        {
            notificationListenerConnectedTimeoutStop();

            if (reason == NotConnectedReason.ConnectedTimeout && mNotificationListener != null)
            {
                return;
            }

            mNotificationListener = null;

            for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onNotificationListenerNotConnected(reason, elapsedMillis);
            }
            mListenerManager.endTraversing();
        }
        FooLog.v(TAG, "-onNotificationListenerNotConnected(reason=" + reason + ')');
    }

    private void onNotificationPosted(
            @NonNull FooNotificationListener notificationListener,
            @NonNull StatusBarNotification sbn)
    {
        synchronized (mSyncLock)
        {
            if (mNotificationListener != notificationListener)
            {
                return;
            }

            for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onNotificationPosted(sbn);
            }
            mListenerManager.endTraversing();
        }
    }

    private void onNotificationRemoved(
            @NonNull FooNotificationListener notificationListener,
            @NonNull StatusBarNotification sbn)
    {
        synchronized (mSyncLock)
        {
            if (mNotificationListener != notificationListener)
            {
                return;
            }

            for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onNotificationRemoved(sbn);
            }
            mListenerManager.endTraversing();
        }
    }

    @TargetApi(18)
    public static class FooNotificationListener
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

        private long                           mOnListenerConnectedStartMillis;
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
            FooLog.v(TAG, "+onCreate()");
            super.onCreate();

            mNotificationListenerManager = FooNotificationListenerManager.getInstance();

            Context applicationContext = getApplicationContext();

            mRemoteController = new RemoteController(applicationContext, this);

            FooLog.v(TAG, "-onCreate()");
        }

        @Override
        public IBinder onBind(Intent intent)
        {
            FooLog.v(TAG, "onBind(intent=" + FooPlatformUtils.toString(intent) + ')');

            if (ACTION_BIND_REMOTE_CONTROLLER.equals(intent.getAction()))
            {
                return mRemoteControllerBinder;
            }

            return super.onBind(intent);
        }

        @Override
        public void onListenerConnected()
        {
            FooLog.v(TAG, "onListenerConnected()");
            super.onListenerConnected();
            mOnListenerConnectedStartMillis = System.currentTimeMillis();
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            mNotificationListenerManager.onNotificationListenerConnected(this, activeNotifications);
        }

        @Override
        public void onNotificationPosted(StatusBarNotification sbn)
        {
            onNotificationPosted(sbn, null);
        }

        @Override
        public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap)
        {
            FooLog.v(TAG, "onNotificationPosted(...)");
            mNotificationListenerManager.onNotificationPosted(this, sbn);
        }

        @Override
        public void onNotificationRankingUpdate(RankingMap rankingMap)
        {
            FooLog.v(TAG, "onNotificationRankingUpdate(...)");
            super.onNotificationRankingUpdate(rankingMap);
        }

        @Override
        public void onListenerHintsChanged(int hints)
        {
            FooLog.v(TAG, "onListenerHintsChanged(...)");
            super.onListenerHintsChanged(hints);
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter)
        {
            FooLog.v(TAG, "onInterruptionFilterChanged(...)");
            super.onInterruptionFilterChanged(interruptionFilter);
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn)
        {
            onNotificationRemoved(sbn, null);
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap)
        {
            FooLog.v(TAG, "onNotificationRemoved(...)");
            mNotificationListenerManager.onNotificationRemoved(this, sbn);
        }

        @Override
        public void onListenerDisconnected()
        {
            FooLog.v(TAG, "onListenerDisconnected()");
            super.onListenerDisconnected();
            long elapsedMillis = System.currentTimeMillis() - mOnListenerConnectedStartMillis;
            mNotificationListenerManager.onNotificationListenerNotConnected(NotConnectedReason.Disconnected, elapsedMillis);
        }

        //
        // RemoteController.OnClientUpdateListener
        //

        @Override
        public void onClientChange(boolean clearing)
        {
            FooLog.v(TAG, "onClientChange(...)");
        }

        @Override
        public void onClientPlaybackStateUpdate(int state)
        {
            FooLog.v(TAG, "onClientPlaybackStateUpdate(...)");
        }

        @Override
        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed)
        {
            FooLog.v(TAG, "onClientPlaybackStateUpdate(...)");
        }

        @Override
        public void onClientTransportControlUpdate(int transportControlFlags)
        {
            FooLog.v(TAG, "onClientTransportControlUpdate(...)");
        }

        @Override
        public void onClientMetadataUpdate(MetadataEditor metadataEditor)
        {
            FooLog.v(TAG, "onClientMetadataUpdate(...)");
        }
    }
}
