package com.smartfoo.android.core.platform;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.os.UserManager;
import android.view.Display;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;

/**
 * Observes screen-on, screen-off, and user-unlock broadcast events.
 *
 * <p>Uses {@link FooListenerAutoStartManager} so the underlying
 * {@link BroadcastReceiver} is registered only while at least one
 * {@link FooScreenListenerCallbacks} is attached, avoiding unnecessary wake-locks.
 * Query the current display state at any time via {@link #isScreenOn()} or
 * {@link #isUserUnlocked()}.</p>
 */
public class FooScreenListener
{
    private static final String TAG = FooLog.TAG(FooScreenListener.class);

    /** Callback interface for screen state changes. */
    public interface FooScreenListenerCallbacks
    {
        /** Called when all displays have turned off. */
        void onScreenOff();

        /** Called when at least one display has turned on. */
        void onScreenOn();

        /** Called when the user has unlocked the device after boot. */
        void onUserUnlocked();
    }

    private final FooListenerAutoStartManager<FooScreenListenerCallbacks> mListenerManager;
    private final FooScreenBroadcastReceiver                              mScreenBroadcastReceiver;
    private final PowerManager                                            mPowerManager;
    private final KeyguardManager                                         mKeyguardManager;
    private final UserManager                                             mUserManager;

    /**
     * Constructs a new listener bound to the given context.
     *
     * @param context the application context used to query display state and to
     *                register/unregister the broadcast receiver
     * @throws IllegalArgumentException if {@code context} is null
     */
    public FooScreenListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
            /** Starts the screen broadcast receiver when the first external callback is attached. */
            @Override
            public void onFirstAttach()
            {
                mScreenBroadcastReceiver.start(new FooScreenListenerCallbacks()
                {
                    /** Delegates to all attached {@link FooScreenListenerCallbacks#onScreenOff()} listeners. */
                    @Override
                    public void onScreenOff()
                    {
                        for (FooScreenListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onScreenOff();
                        }
                        mListenerManager.endTraversing();
                    }

                    /** Delegates to all attached {@link FooScreenListenerCallbacks#onScreenOn()} listeners. */
                    @Override
                    public void onScreenOn()
                    {
                        for (FooScreenListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onScreenOn();
                        }
                        mListenerManager.endTraversing();
                    }

                    /** Delegates to all attached {@link FooScreenListenerCallbacks#onUserUnlocked()} listeners. */
                    @Override
                    public void onUserUnlocked()
                    {
                        for (FooScreenListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onUserUnlocked();
                        }
                        mListenerManager.endTraversing();
                    }
                });
            }

            /**
             * Stops the screen broadcast receiver when the last external callback is detached.
             *
             * @return {@code false} to allow the listener manager to remove the internal callback entry
             */
            @Override
            public boolean onLastDetach()
            {
                mScreenBroadcastReceiver.stop();
                return false;
            }
        });
        mScreenBroadcastReceiver = new FooScreenBroadcastReceiver(context);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    /**
     * Returns true if at least one display is currently in a non-off state.
     *
     * <p>Queries {@link android.hardware.display.DisplayManager} at call time; does not require
     * the receiver to be started.</p>
     *
     * @return true if any display is on
     */
    public boolean isScreenOn()
    {
        return mScreenBroadcastReceiver.isScreenOn();
    }

    /**
     * Returns true if the current user has unlocked the device after boot.
     *
     * <p>Delegates to {@link android.os.UserManager#isUserUnlocked()}.</p>
     *
     * @return true if the user is unlocked
     */
    public boolean isUserUnlocked()
    {
        boolean isUserUnlocked = mUserManager.isUserUnlocked();
        FooLog.e(TAG, "isUserUnlocked: isUserUnlocked == " + isUserUnlocked);
        return isUserUnlocked;
    }

    /**
     * Registers {@code callbacks} to receive screen state events.
     *
     * <p>The underlying broadcast receiver is registered automatically when the first callbacks
     * instance is attached.</p>
     *
     * @param callbacks the listener to register; no-op if already registered
     */
    public void attach(FooScreenListenerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
    }

    /**
     * Unregisters previously registered {@code callbacks}.
     *
     * <p>The underlying broadcast receiver is unregistered automatically when the last callbacks
     * instance is detached.</p>
     *
     * @param callbacks the listener to remove; no-op if not registered
     */
    public void detach(FooScreenListenerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    private static class FooScreenBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooScreenBroadcastReceiver.class);

        private final Context        mContext;
        private final Object         mSyncLock;
        private final DisplayManager mDisplayManager;

        private boolean                    mIsStarted;
        private FooScreenListenerCallbacks mCallbacks;

        private FooScreenBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
            mSyncLock = new Object();
            mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        }

        /**
         * Returns {@code true} if at least one display is currently in a non-off state.
         *
         * @return {@code true} if any display is on
         */
        public boolean isScreenOn()
        {
            boolean isScreenOn = false;
            for (Display display : mDisplayManager.getDisplays())
            {
                if (display.getState() != Display.STATE_OFF)
                {
                    isScreenOn = true;
                    break;
                }
            }
            return isScreenOn;
        }

        /**
         * Returns {@code true} if this receiver is currently registered with the system.
         *
         * @return {@code true} if started
         */
        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

        /**
         * Registers this receiver to listen for screen-on, screen-off, and user-unlock broadcasts.
         * Does nothing if already started.
         *
         * @param callbacks the callbacks to notify on screen state changes; must not be null
         */
        public void start(@NonNull FooScreenListenerCallbacks callbacks)
        {
            FooLog.v(TAG, "+start(...)");
            synchronized (mSyncLock)
            {
                if (!mIsStarted)
                {
                    mIsStarted = true;

                    mCallbacks = callbacks;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Intent.ACTION_SCREEN_OFF); // API 1
                    intentFilter.addAction(Intent.ACTION_SCREEN_ON); // API 1
                    mContext.registerReceiver(this, intentFilter);
                }
            }
            FooLog.v(TAG, "-start(...)");
        }

        /**
         * Unregisters this receiver from the system. Does nothing if not currently started.
         */
        public void stop()
        {
            FooLog.v(TAG, "+stop()");
            synchronized (mSyncLock)
            {
                if (mIsStarted)
                {
                    mIsStarted = false;

                    mContext.unregisterReceiver(this);
                }
            }
            FooLog.v(TAG, "-stop()");
        }

        /**
         * Dispatches the received screen-on, screen-off, or user-unlock broadcast to the
         * appropriate {@link FooScreenListenerCallbacks} method.
         *
         * <p>For {@link Intent#ACTION_SCREEN_OFF} and {@link Intent#ACTION_SCREEN_ON}, the
         * display state is re-verified via {@link #isScreenOn()} before notifying callbacks
         * to guard against stale or out-of-order broadcasts.</p>
         *
         * @param context the context in which the receiver is running
         * @param intent  the received broadcast intent with action
         *                {@link Intent#ACTION_SCREEN_OFF}, {@link Intent#ACTION_SCREEN_ON},
         *                or {@link Intent#ACTION_USER_UNLOCKED}
         */
        @Override
        public void onReceive(Context context, Intent intent)
        {
            FooLog.v(TAG, "onReceive: intent == " + FooPlatformUtils.toString(intent));
            String action = intent.getAction();
            switch (action)
            {
                case Intent.ACTION_SCREEN_OFF:
                    if (!isScreenOn())
                    {
                        mCallbacks.onScreenOff();
                    }
                    break;
                case Intent.ACTION_SCREEN_ON:
                {
                    if (isScreenOn())
                    {
                        mCallbacks.onScreenOn();
                    }
                    break;
                }
                case Intent.ACTION_USER_UNLOCKED:
                    mCallbacks.onUserUnlocked();
                    break;
            }
        }
    }
}
