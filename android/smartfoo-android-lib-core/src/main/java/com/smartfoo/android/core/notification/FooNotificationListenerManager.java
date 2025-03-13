package com.smartfoo.android.core.notification;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooHandler;

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(18)
public class FooNotificationListenerManager
{
    private static final String TAG = FooLog.TAG(FooNotificationListenerManager.class);

    /**
     * <p>Needs to be reasonably longer than the app startup time.</p>
     * <p>NOTE1 that the app startup time can be a few seconds when debugging.</p>
     * <p>NOTE2 that this will time out if paused too long at a debug breakpoint while launching.</p>
     */
    public static class NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS
    {
        public static final int NORMAL = 1500;
        public static final int SLOW = 6000;

        public static int getRecommendedTimeout(boolean slow)
        {
            return slow ? SLOW : NORMAL;
        }
    }

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
     * Per hidden field {@link android.provider.Settings.Secure} ENABLED_NOTIFICATION_LISTENERS
     */
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    public static boolean isNotificationAccessSettingConfirmedEnabled(@NonNull Context context)
    {
        return isNotificationAccessSettingConfirmedEnabled(context, FooNotificationListenerService.class);
    }

    public static boolean isNotificationAccessSettingConfirmedEnabled(
            @NonNull Context context,
            @NonNull Class<? extends NotificationListenerService> notificationListenerServiceClass)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(notificationListenerServiceClass, "notificationListenerServiceClass");

        if (supportsNotificationListenerSettings())
        {
            ComponentName notificationListenerServiceLookingFor = new ComponentName(context, notificationListenerServiceClass);
            FooLog.d(TAG, "isNotificationAccessSettingConfirmedEnabled: notificationListenerServiceLookingFor=" + notificationListenerServiceLookingFor);

            ContentResolver contentResolver = context.getContentResolver();
            String notificationListenersString = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS);
            if (notificationListenersString != null)
            {
                String[] notificationListeners = notificationListenersString.split(":");
                for (int i = 0; i < notificationListeners.length; i++)
                {
                    ComponentName notificationListener = ComponentName.unflattenFromString(notificationListeners[i]);
                    FooLog.d(TAG, "isNotificationAccessSettingConfirmedEnabled: notificationListeners[" + i + "]=" + notificationListener);
                    if (notificationListenerServiceLookingFor.equals(notificationListener))
                    {
                        FooLog.i(TAG, "isNotificationAccessSettingConfirmedEnabled: found match; return true");
                        return true;
                    }
                }
            }
        }

        FooLog.w(TAG, "isNotificationAccessSettingConfirmedEnabled: found NO match; return false");
        return false;
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
        boolean onNotificationListenerServiceConnected(@NonNull StatusBarNotification[] activeNotifications);

        void onNotificationListenerServiceNotConnected(@NonNull NotConnectedReason reason, long elapsedMillis);

        void onNotificationPosted(@NonNull StatusBarNotification sbn);

        void onNotificationRemoved(@NonNull StatusBarNotification sbn);
    }

    private static FooNotificationListenerManager sInstance;

    public static FooNotificationListenerManager getInstance()
    {
        if (sInstance == null)
        {
            sInstance = new FooNotificationListenerManager();
        }
        return sInstance;
    }

    private final Object                                                      mSyncLock;
    /**
     * NOTE: **Purposefully not using FooListenerAutoStartManager due to incompatible logic in {@link #attach(Context, FooNotificationListenerManagerCallbacks)}
     */
    private final FooListenerManager<FooNotificationListenerManagerCallbacks> mListenerManager;
    private final FooHandler                                                  mHandler;

    private FooNotificationListenerService mNotificationListenerService;

    private long mNotificationListenerServiceConnectedTimeoutMillis = NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.NORMAL;
    private long mNotificationListenerServiceConnectedTimeoutStartMillis = -1;

    private FooNotificationListenerManager()
    {
        mSyncLock = new Object();
        mListenerManager = new FooListenerManager<>(this);
        mHandler = new FooHandler();
    }

    /**
     * <p><b>Set to slow mode for debug builds.</b></p>
     * Sets timeout based on {@link NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS#getRecommendedTimeout(boolean)}<br>
     * To set a more precise timeout, use {@link #setTimeout(long)}
     */
    public void setSlowMode(boolean value)
    {
        long timeoutMillis = NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.getRecommendedTimeout(value);
        setTimeout(timeoutMillis);
    }

    public void setTimeout(long timeoutMillis)
    {
        mNotificationListenerServiceConnectedTimeoutMillis = timeoutMillis;
    }

    public boolean isNotificationListenerServiceConnected()
    {
        return mNotificationListenerService != null;
    }

    public void attach(@NonNull Context context,
                       @NonNull FooNotificationListenerManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        mListenerManager.attach(callbacks);

        //
        // NOTE: **Purposefully not using FooListenerAutoStartManager due to the following incompatible logic**...
        //
        if (isNotificationAccessSettingConfirmedEnabled(context))
        {
            if (mListenerManager.size() == 1)
            {
                if (mNotificationListenerServiceConnectedTimeoutStartMillis == -1)
                {
                    notificationListenerServiceConnectedTimeoutStart(mNotificationListenerServiceConnectedTimeoutMillis);
                }
            }
        }
        else
        {
            callbacks.onNotificationListenerServiceNotConnected(NotConnectedReason.ConfirmedNotEnabled, 0);
        }
    }

    public void detach(@NonNull FooNotificationListenerManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        mListenerManager.detach(callbacks);

        if (mListenerManager.isEmpty())
        {
            notificationListenerServiceConnectedTimeoutStop();
        }
    }

    public StatusBarNotification[] getActiveNotifications()
    {
        synchronized (mSyncLock)
        {
            return mNotificationListenerService != null ? mNotificationListenerService.getActiveNotifications() : null;
        }
    }

    /** @noinspection unused*/
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
            if (mNotificationListenerService == null)
            {
                return;
            }

            for (StatusBarNotification sbn : activeNotifications)
            {
                onNotificationPosted(mNotificationListenerService, sbn);
            }
        }
    }

    /**
     * <p>
     * HACK required to detect any {@link NotificationListenerService} <b>NOT</b> calling
     * {@link NotificationListenerService#onListenerConnected()} <b>**after updating/re-installing app
     * <i>EVEN IF NOTIFICATION ACCESS SAYS/SHOWS IT IS ENABLED!</i>**</b>:<br>
     * <a href="http://stackoverflow.com/a/37081128/252308">http://stackoverflow.com/a/37081128/252308</a><br>
     * Comment is from 2016; page has some updates since.
     * </p>
     * <p>
     * <b>Background:</b><br>
     * After an Application [that requires `BIND_NOTIFICATION_LISTENER_SERVICE` permission] is
     * installed, the user needs to configure it to have Notification Access.<br>
     * A user would rarely know to do this on their own, so usually the newly installed app would
     * test that it does not have Notification Access and prompt the user to enable it.<br>
     * When enabled, the OS will start the app's NotificationListenerService.<br>
     * When disabled, the OS will stop the app's NotificationListenerService.
     * </p>
     * <p>
     * In a perfect world this may seem all well and good, but Android messed up the implementation
     * <b>making it harder for a developer to develop the code</b> (and indirectly making things
     * worse for the user).
     * </p>
     * <p>
     * A developer regularly tests their code changes by pushing app updates to the device.
     * </p>
     * <p>
     * The problem is that when a developer updates their app <b>the OS kills the app's
     * NotificationListenerService <i>BUT DOES NOT RE-START IT!</i></b>
     * </p>
     * <p>
     * When the developer launches their updated app, the OS did NOT restart the app's
     * NotificationListenerService and the app will not function as expected.
     * </p>
     * <p>
     * In order to get the app's NotificationListenerService working again <b>the developer has to
     * remember to "turn it off back back on again"...<i>every single time they update their code!</i></b>
     * :/
     * </p>
     * <p>
     * <b>WORKAROUND:</b>
     * <ol>
     *   <li>Launch Device Settings -> ... -> Notification Access</li>
     *   <li>Disable Notification Access</li>
     *   <li>Enable Notification Access</li>
     * </ol>
     * </p>
     * <p>
     * <i>(NOTE: It is currently unknown if this problem is limited only to app updates during
     * development, or if it also affects user app updates through Google Play.)</i>
     * </p>
     * <p>
     * Testing for isNotificationAccessSettingEnabled will only tell us if the app has Notification
     * Access enabled; it will not tell the app if its NotificationListenerService is connected.<br>
     * The code could test for running services, but testing for a running service does not
     * guarantee that NotificationListenerService onListenerConnected has been called.<br>
     * The best option unfortunately seems to be to timeout if NotificationListenerService
     * onListenerConnected is not called within a small amount of time (recommend <1.5s).<br>
     * If FooNotificationListenerManager does not call onNotificationListenerServiceBound() within
     * that time then the app should prompt the [developer] user to disable and re-enable
     * Notification Access.
     * </p>
     */
    private void notificationListenerServiceConnectedTimeoutStart(long timeoutMillis)
    {
        FooLog.v(TAG, "+notificationListenerServiceConnectedTimeoutStart(timeoutMillis=" + timeoutMillis + ')');
        if (mNotificationListenerServiceConnectedTimeoutStartMillis != -1)
        {
            notificationListenerServiceConnectedTimeoutStop();
        }
        mNotificationListenerServiceConnectedTimeoutStartMillis = System.currentTimeMillis();
        mHandler.postDelayed(mNotificationListenerServiceConnectedTimeoutRunnable, timeoutMillis);
        FooLog.v(TAG, "-notificationListenerServiceConnectedTimeoutStart(timeoutMillis=" + timeoutMillis + ')');
    }

    private void notificationListenerServiceConnectedTimeoutStop()
    {
        FooLog.v(TAG, "+notificationListenerServiceConnectedTimeoutStop()");
        mNotificationListenerServiceConnectedTimeoutStartMillis = -1;
        mHandler.removeCallbacks(mNotificationListenerServiceConnectedTimeoutRunnable);
        FooLog.v(TAG, "-notificationListenerServiceConnectedTimeoutStop()");
    }

    private final Runnable mNotificationListenerServiceConnectedTimeoutRunnable = () -> {
        FooLog.v(TAG, "+mNotificationListenerServiceConnectedTimeoutRunnable.run()");
        long elapsedMillis = System.currentTimeMillis() - mNotificationListenerServiceConnectedTimeoutStartMillis;
        onNotificationListenerNotConnected(NotConnectedReason.ConnectedTimeout, elapsedMillis);
        FooLog.v(TAG, "-mNotificationListenerServiceConnectedTimeoutRunnable.run()");
    };

    private void onNotificationListenerConnected(
            @NonNull FooNotificationListenerService notificationListenerService,
            @NonNull StatusBarNotification[] activeNotifications)
    {
        synchronized (mSyncLock)
        {
            notificationListenerServiceConnectedTimeoutStop();

            mNotificationListenerService = notificationListenerService;

            boolean initializeActiveNotifications = true;
            for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                initializeActiveNotifications &= !callbacks.onNotificationListenerServiceConnected(activeNotifications);
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
            notificationListenerServiceConnectedTimeoutStop();

            if (reason == NotConnectedReason.ConnectedTimeout && mNotificationListenerService != null)
            {
                return;
            }

            mNotificationListenerService = null;

            for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onNotificationListenerServiceNotConnected(reason, elapsedMillis);
            }
            mListenerManager.endTraversing();
        }
        FooLog.v(TAG, "-onNotificationListenerNotConnected(reason=" + reason + ')');
    }

    private void onNotificationPosted(
            @NonNull FooNotificationListenerService notificationListenerService,
            @NonNull StatusBarNotification sbn)
    {
        synchronized (mSyncLock)
        {
            if (mNotificationListenerService != notificationListenerService)
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
            @NonNull FooNotificationListenerService notificationListenerService,
            @NonNull StatusBarNotification sbn)
    {
        synchronized (mSyncLock)
        {
            if (mNotificationListenerService != notificationListenerService)
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

    /** @noinspection CommentedOutCode*/
    @RequiresApi(18)
    public static class FooNotificationListenerService
            extends NotificationListenerService
            //implements RemoteController.OnClientUpdateListener
    {
        private static final String TAG = FooLog.TAG(FooNotificationListenerService.class);

        private long                           mOnListenerConnectedStartMillis;
        private FooNotificationListenerManager mNotificationListenerManager;

        @Override
        public void onCreate()
        {
            FooLog.v(TAG, "+onCreate()");
            super.onCreate();

            mNotificationListenerManager = FooNotificationListenerManager.getInstance();

            /*
            Context applicationContext = getApplicationContext();
            mRemoteController = new RemoteController(applicationContext, this);
            */

            FooLog.v(TAG, "-onCreate()");
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

        /*

        //
        // RemoteController
        //

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

        private RemoteController mRemoteController;

        private final IBinder mRemoteControllerBinder = new RemoteControllerBinder();

        public RemoteController getRemoteController()
        {
            return mRemoteController;
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
        */
    }
}
