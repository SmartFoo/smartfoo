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
     * Used to force a specific OS Version # FOR TESTING!
     */
    private static final int VERSION_SDK_INT = VERSION.SDK_INT;

    public static boolean supportsNotificationListenerSettings()
    {
        return VERSION_SDK_INT >= 19;
    }

    @TargetApi(21) // TODO:(pv) Does this work on API19-20?
    public static boolean isNotificationAccessSettingEnabled(Context context)
    {
        String packageName = context.getPackageName();

        ContentResolver contentResolver = context.getContentResolver();

        final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS);
        if (enabledNotificationListeners != null)
        {
            for (String enabledNotificationListener : enabledNotificationListeners.split(":"))
            {
                if (enabledNotificationListener.startsWith(packageName))
                {
                    return true;
                }
            }
        }

        return false;
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

    public interface FooNotificationListenerManagerCallbacks
    {
        void onNotificationListenerBound();

        void onNotificationListenerUnbound();

        void onNotificationPosted(StatusBarNotification sbn);

        void onNotificationRemoved(StatusBarNotification sbn);
    }

    public static FooNotificationListenerManager getInstance()
    {
        return sInstance;
    }

    private static FooNotificationListenerManager sInstance;

    static
    {
        sInstance = new FooNotificationListenerManager();
    }

    private final FooListenerManager<FooNotificationListenerManagerCallbacks> mListenerManager;
    private final Runnable                                                    mRunIfNotBound;

    private FooHandler mHandler;
    private Boolean    mIsNotificationListenerBound;

    private FooNotificationListenerManager()
    {
        mListenerManager = new FooListenerManager<>();

        mRunIfNotBound = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    FooLog.v(TAG, "+mRunIfNotBound.run()");
                    if (isNotificationListenerBound())
                    {
                        return;
                    }

                    onNotificationListenerUnbound();
                }
                finally
                {
                    FooLog.v(TAG, "-mRunIfNotBound.run()");
                }
            }
        };
    }

    public void startActivityNotificationListenerSettings(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        context.startActivity(getIntentNotificationListenerSettings());
    }

    public void attach(FooNotificationListenerManagerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);

        if (mHandler == null && mListenerManager.size() == 1)
        {
            //
            // HACK required to detect non-binding when re-installing app even if notification access is enabled:
            // http://stackoverflow.com/a/37081128/252308
            //
            // Even if Notification Access is enabled, the Application always starts first, before FooNotificationListener has any chance to bind.
            // After the Application start, if FooNotificationListener binds then it will call onNotificationListenerBound().
            // On first run it will not bind because the user has not enabled the settings.
            // Normally we would just directly call FooNotificationListener.isNotificationAccessSettingEnabled(Context context).
            // Unfortunately, sometimes NotificationAccess is configured to be enabled, but FooNotificationListener never binds.
            // This almost always happens when re-installing the app between development builds.
            // NOTE:(pv) It is unknown if this is also an issue when the app does production updates through Google Play.
            // Since we cannot reliably test for isNotificationAccessSettingEnabled, the next best thing is to timeout if
            // FooNotificationListener does not bind within a small amount of time (we are using 250ms).
            // If FooNotificationListener does not bind and call onNotificationListenerBound() within 250ms then we need to
            // prompt the user to enable Notification Access.
            //
            mHandler = new FooHandler();
            mHandler.postDelayed(mRunIfNotBound, 250);
        }
    }

    public void detach(FooNotificationListenerManagerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);

        // TODO:(pv) Anything to do if mListenerManager.size() changed to 0?
    }

    public boolean isNotificationListenerBound()
    {
        return mIsNotificationListenerBound != null && mIsNotificationListenerBound;
    }

    void onNotificationListenerBound()
    {
        mHandler.removeCallbacks(mRunIfNotBound);

        mIsNotificationListenerBound = true;

        for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationListenerBound();
        }
        mListenerManager.endTraversing();
    }

    void onNotificationPosted(StatusBarNotification sbn)
    {
        for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationPosted(sbn);
        }
        mListenerManager.endTraversing();
    }

    void onNotificationRemoved(StatusBarNotification sbn)
    {
        for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationRemoved(sbn);
        }
        mListenerManager.endTraversing();
    }

    void onNotificationListenerUnbound()
    {
        if (mIsNotificationListenerBound != null && !mIsNotificationListenerBound)
        {
            return;
        }

        mIsNotificationListenerBound = false;

        for (FooNotificationListenerManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationListenerUnbound();
        }
        mListenerManager.endTraversing();
    }
}
