package com.smartfoo.android.core.platform;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;

// TODO:(pv) Also listen for shutdown?
/**
 * Observes device boot, reboot, and shutdown broadcast events.
 *
 * <p>Registers a {@link BroadcastReceiver} for {@link Intent#ACTION_BOOT_COMPLETED},
 * {@link Intent#ACTION_REBOOT}, and {@link Intent#ACTION_SHUTDOWN} and fans them out
 * to all attached {@link FooBootListenerCallbacks} via the auto-start listener pattern
 * (the receiver is registered only while at least one callback is attached).</p>
 *
 * <p>Requires {@code android.permission.RECEIVE_BOOT_COMPLETED} in the manifest.</p>
 */
public class FooBootListener
{
    private static final String TAG = FooLog.TAG(FooBootListener.class);

    /** Callback interface for device boot lifecycle events. */
    public interface FooBootListenerCallbacks
    {
        //void onLockedBootCompleted();

        /** Called when the device has finished booting ({@link Intent#ACTION_BOOT_COMPLETED}). */
        void onBootCompleted();

        /** Called when the device is rebooting ({@link Intent#ACTION_REBOOT}). */
        void onReboot();

        /** Called when the device is shutting down ({@link Intent#ACTION_SHUTDOWN}). */
        void onShutdown();
    }

    private final FooListenerAutoStartManager<FooBootListenerCallbacks> mListenerManager;
    private final FooBootBroadcastReceiver                              mBootBroadcastReceiver;

    /**
     * Constructs a new listener bound to the given context.
     *
     * @param context the application context used to register/unregister the broadcast receiver
     * @throws IllegalArgumentException if {@code context} is null
     */
    public FooBootListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
            /** Starts the boot broadcast receiver when the first external callback is attached. */
            @Override
            public void onFirstAttach()
            {
                mBootBroadcastReceiver.start(new FooBootListenerCallbacks()
                {
                    /*
                    @Override
                    public void onLockedBootCompleted()
                    {
                        for (FooBootListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onLockedBootCompleted();
                        }
                        mListenerManager.endTraversing();
                    }
                    */

                    /** Delegates to all attached {@link FooBootListenerCallbacks#onBootCompleted()} listeners. */
                    @Override
                    public void onBootCompleted()
                    {
                        for (FooBootListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onBootCompleted();
                        }
                        mListenerManager.endTraversing();
                    }

                    /** Delegates to all attached {@link FooBootListenerCallbacks#onReboot()} listeners. */
                    @Override
                    public void onReboot()
                    {
                        for (FooBootListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onReboot();
                        }
                        mListenerManager.endTraversing();
                    }

                    /** Delegates to all attached {@link FooBootListenerCallbacks#onShutdown()} listeners. */
                    @Override
                    public void onShutdown()
                    {
                        for (FooBootListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onShutdown();
                        }
                        mListenerManager.endTraversing();
                    }
                });
            }

            /**
             * Stops the boot broadcast receiver when the last external callback is detached.
             *
             * @return {@code false} to allow the listener manager to remove the internal callback entry
             */
            @Override
            public boolean onLastDetach()
            {
                mBootBroadcastReceiver.stop();
                return false;
            }
        });
        mBootBroadcastReceiver = new FooBootBroadcastReceiver(context);
    }

    /**
     * Registers {@code callbacks} to receive boot lifecycle events.
     *
     * <p>The underlying broadcast receiver is registered automatically when the first callbacks
     * instance is attached.</p>
     *
     * @param callbacks the listener to register; no-op if already registered
     */
    public void attach(FooBootListenerCallbacks callbacks)
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
    public void detach(FooBootListenerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    private static class FooBootBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooBootBroadcastReceiver.class);

        private final Context mContext;

        private boolean                  mIsStarted;
        private FooBootListenerCallbacks mCallbacks;

        public FooBootBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
        }

        /**
         * Returns {@code true} if this receiver is currently registered with the system.
         *
         * @return {@code true} if started
         */
        public boolean isStarted()
        {
            return mIsStarted;
        }

        /**
         * Registers this receiver to listen for boot, reboot, and shutdown broadcasts.
         * Does nothing if already started.
         *
         * @param callbacks the callbacks to notify on boot events; must not be null
         */
        public void start(@NonNull FooBootListenerCallbacks callbacks)
        {
            FooLog.v(TAG, "+start(...)");
            if (!mIsStarted)
            {
                mIsStarted = true;

                mCallbacks = callbacks;

                IntentFilter intentFilter = new IntentFilter();
                //intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
                intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
                intentFilter.addAction(Intent.ACTION_REBOOT);
                intentFilter.addAction(Intent.ACTION_SHUTDOWN);
                mContext.registerReceiver(this, intentFilter);
            }
            FooLog.v(TAG, "-start(...)");
        }

        /**
         * Unregisters this receiver from the system. Does nothing if not currently started.
         */
        public void stop()
        {
            FooLog.v(TAG, "+stop(...)");
            if (mIsStarted)
            {
                mIsStarted = false;
                mContext.unregisterReceiver(this);
            }
            FooLog.v(TAG, "-stop(...)");
        }

        /**
         * Dispatches the received boot, reboot, or shutdown broadcast to the appropriate
         * {@link FooBootListenerCallbacks} method.
         *
         * @param context the context in which the receiver is running
         * @param intent  the received broadcast intent with action
         *                {@link Intent#ACTION_BOOT_COMPLETED}, {@link Intent#ACTION_REBOOT},
         *                or {@link Intent#ACTION_SHUTDOWN}
         */
        @Override
        public void onReceive(Context context, Intent intent)
        {
            FooLog.v(TAG, "onReceive: intent == " + FooPlatformUtils.toString(intent));
            String action = intent.getAction();
            switch (action)
            {
                /*
                case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                    FooLog.e(TAG, "onReceive: ACTION_LOCKED_BOOT_COMPLETED");
                    mCallbacks.onLockedBootCompleted();
                    break;
                    */
                case Intent.ACTION_BOOT_COMPLETED:
                    FooLog.i(TAG, "onReceive: ACTION_BOOT_COMPLETED");
                    mCallbacks.onBootCompleted();
                    break;
                case Intent.ACTION_REBOOT:
                    mCallbacks.onReboot();
                    break;
                case Intent.ACTION_SHUTDOWN:
                    FooLog.i(TAG, "onReceive: ACTION_SHUTDOWN");
                    mCallbacks.onShutdown();
                    break;
            }
        }
    }

    /**
     * Manifest-declared {@link BroadcastReceiver} for {@link Intent#ACTION_LOCKED_BOOT_COMPLETED}.
     *
     * <p>Declared in the manifest so it can be invoked by the system before the user unlocks the
     * device (direct-boot mode). Currently logs the event; subclass or extend to add behaviour.</p>
     */
    public static class FooLockedBootCompletedBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooLockedBootCompletedBroadcastReceiver.class);

        /**
         * Handles the {@link Intent#ACTION_LOCKED_BOOT_COMPLETED} broadcast.
         *
         * @param context the context in which the receiver is running
         * @param intent  the {@link Intent#ACTION_LOCKED_BOOT_COMPLETED} intent
         */
        @Override
        public void onReceive(Context context, Intent intent)
        {
            FooLog.v(TAG, "onReceive: intent == " + FooPlatformUtils.toString(intent));
            String action = intent.getAction();
            switch (action)
            {
                case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                    FooLog.i(TAG, "onReceive: ACTION_LOCKED_BOOT_COMPLETED");
                    break;
            }
        }
    }
}
