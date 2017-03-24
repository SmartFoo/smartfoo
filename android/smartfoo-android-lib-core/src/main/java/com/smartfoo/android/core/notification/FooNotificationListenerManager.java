package com.smartfoo.android.core.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooHandler;

public class FooNotificationListenerManager
{
    private static final String TAG = FooLog.TAG(FooNotificationListenerManager.class);

    /**
     * Usually VERSION.SDK_INT, but may be used to force a specific OS Version # FOR TESTING!
     */
    private static final int VERSION_SDK_INT = VERSION.SDK_INT;

    public static boolean supportsNotificationListenerSettings()
    {
        return VERSION_SDK_INT >= 19;
    }

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    @TargetApi(21)
    public static boolean isNotificationAccessSettingConfirmedNotEnabled(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");

        String packageName = context.getPackageName();

        ContentResolver contentResolver = context.getContentResolver();

        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS);
        if (enabledNotificationListeners != null)
        {
            for (String enabledNotificationListener : enabledNotificationListeners.split(":"))
            {
                if (enabledNotificationListener.startsWith(packageName))
                {
                    return false;
                }
            }
        }

        return true;
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

    public static void startActivityNotificationListenerSettings(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        context.startActivity(getIntentNotificationListenerSettings());
    }

    public enum DisabledCause
    {
        ConfirmedNotEnabled,
        BindTimeout,
        Unbind,
    }

    public interface FooNotificationListenerManagerCallbacks
    {
        /**
         * @return true to prevent {@link #initializeActiveNotifications()} from being automatically called
         */
        boolean onNotificationAccessSettingConfirmedEnabled();

        void onNotificationAccessSettingDisabled(DisabledCause disabledCause);

        void onNotificationPosted(StatusBarNotification sbn);

        void onNotificationRemoved(StatusBarNotification sbn);
    }

    public static FooNotificationListenerManager getInstance()
    {
        return sInstance;
    }

    private static FooNotificationListenerManager sInstance = new FooNotificationListenerManager();

    private final FooListenerManager<FooNotificationListenerManagerCallbacks> mListenerManager;
    private final FooHandler                                                  mHandler;

    private FooNotificationListener mNotificationListener;
    private boolean                 mIsWaitingForNotificationListenerBindTimeout;

    private FooNotificationListenerManager()
    {
        mListenerManager = new FooListenerManager<>();
        mHandler = new FooHandler();
    }

    /**
     * @return true if Notification Access is confirmed enabled (ie: FooNotificationListener successfully bound)
     */
    public boolean isNotificationAccessSettingConfirmedEnabled()
    {
        return mNotificationListener != null;
    }

    public void attach(@NonNull Context context,
                       @NonNull FooNotificationListenerManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        mListenerManager.attach(callbacks);

        if (isNotificationAccessSettingConfirmedNotEnabled(context))
        {
            callbacks.onNotificationAccessSettingDisabled(DisabledCause.ConfirmedNotEnabled);
        }
        else
        {
            if (mListenerManager.size() == 1)
            {
                if (!mIsWaitingForNotificationListenerBindTimeout)
                {
                    notificationListenerBindTimeoutStart(100);
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
            notificationListenerBindTimeoutStop();
        }
    }

    public boolean initializeActiveNotifications()
    {
        return attemptInitializeActiveNotifications(true);
    }

    private boolean attemptInitializeActiveNotifications(boolean reset)
    {
        if (mNotificationListener == null)
        {
            return false;
        }

        if (reset)
        {
            resetAttemptInitializeActiveNotifications();
        }

        try
        {
            StatusBarNotification[] activeNotifications = mNotificationListener.getActiveNotifications();
            //FooLog.e(TAG, "initializeActiveNotifications: activeNotifications=" + FooString.toString(activeNotifications));
            if (activeNotifications != null)
            {
                for (StatusBarNotification sbn : activeNotifications)
                {
                    onNotificationPosted(mNotificationListener, sbn);
                }
            }

            resetAttemptInitializeActiveNotifications();

            mAttemptInitializeActiveNotificationsSuccess = true;

            FooLog.i(TAG, "initializeActiveNotifications: Success after " +
                          mAttemptInitializeActiveNotificationsAttempts + " attempts");

            return true;
        }
        catch (SecurityException e)
        {
            FooLog.w(TAG, "initializeActiveNotifications: EXCEPTION", e);

            FooLog.v(TAG, "initializeActiveNotifications: mAttemptInitializeActiveNotificationsAttempts == " +
                          mAttemptInitializeActiveNotificationsAttempts);

            //
            // Hack required to read active notifications immediately after being bound
            //
            if (mAttemptInitializeActiveNotificationsAttempts < ATTEMPT_INITIALIZE_ACTIVE_NOTIFICATIONS_MAX)
            {
                mAttemptInitializeActiveNotificationsAttempts++;
                mAttemptInitializeActiveNotificationsDelay += 100; // linear backoff

                mHandler.postDelayed(mAttemptInitializeActiveNotificationsRunnable, mAttemptInitializeActiveNotificationsDelay);
            }
            else
            {
                FooLog.w(TAG, "initializeActiveNotifications: Maximum number of attempts (" +
                              ATTEMPT_INITIALIZE_ACTIVE_NOTIFICATIONS_MAX + ") reached");
            }
        }

        return false;
    }

    private static final int ATTEMPT_INITIALIZE_ACTIVE_NOTIFICATIONS_MAX = 10;

    private int     mAttemptInitializeActiveNotificationsAttempts;
    private int     mAttemptInitializeActiveNotificationsDelay;
    private boolean mAttemptInitializeActiveNotificationsSuccess;

    private final Runnable mAttemptInitializeActiveNotificationsRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            attemptInitializeActiveNotifications(false);
        }
    };

    private void resetAttemptInitializeActiveNotifications()
    {
        mHandler.removeCallbacks(mAttemptInitializeActiveNotificationsRunnable);
        mAttemptInitializeActiveNotificationsAttempts = 0;
        mAttemptInitializeActiveNotificationsDelay = 0;
        mAttemptInitializeActiveNotificationsSuccess = false;
    }

    public boolean isAttemptInitializeActiveNotificationsSuccess()
    {
        return mAttemptInitializeActiveNotificationsSuccess;
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
    private void notificationListenerBindTimeoutStart(long timeoutMillis)
    {
        mIsWaitingForNotificationListenerBindTimeout = true;
        mHandler.postDelayed(mNotificationListenerBindTimeout, timeoutMillis);
    }

    private void notificationListenerBindTimeoutStop()
    {
        mIsWaitingForNotificationListenerBindTimeout = false;
        mHandler.removeCallbacks(mNotificationListenerBindTimeout);
    }

    private final Runnable mNotificationListenerBindTimeout = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                FooLog.v(TAG, "+mNotificationListenerBindTimeout.run()");
                if (isNotificationAccessSettingConfirmedEnabled())
                {
                    return;
                }

                onNotificationAccessSettingDisabled(null);
            }
            finally
            {
                FooLog.v(TAG, "-mNotificationListenerBindTimeout.run()");
            }
        }
    };

    void onNotificationAccessSettingConfirmedEnabled(FooNotificationListener notificationListener)
    {
        notificationListenerBindTimeoutStop();

        mNotificationListener = notificationListener;

        resetAttemptInitializeActiveNotifications();

        boolean initialize = true;
        for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            initialize &= !callbacks.onNotificationAccessSettingConfirmedEnabled();
        }
        mListenerManager.endTraversing();

        if (initialize)
        {
            attemptInitializeActiveNotifications(false);
        }
    }

    void onNotificationAccessSettingDisabled(FooNotificationListener notificationListener)
    {
        notificationListenerBindTimeoutStop();

        mNotificationListener = null;

        resetAttemptInitializeActiveNotifications();

        DisabledCause disabledCause = notificationListener == null ? DisabledCause.BindTimeout : DisabledCause.Unbind;

        for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationAccessSettingDisabled(disabledCause);
        }
        mListenerManager.endTraversing();
    }

    void onNotificationPosted(FooNotificationListener notificationListener, StatusBarNotification sbn)
    {
        if (mNotificationListener == null || mNotificationListener != notificationListener)
        {
            return;
        }

        for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationPosted(sbn);
        }
        mListenerManager.endTraversing();
    }

    void onNotificationRemoved(FooNotificationListener notificationListener, StatusBarNotification sbn)
    {
        if (mNotificationListener == null || mNotificationListener != notificationListener)
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
