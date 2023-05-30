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
public class FooBootListener
{
    private static final String TAG = FooLog.TAG(FooBootListener.class);

    public interface FooBootListenerCallbacks
    {
        //void onLockedBootCompleted();

        void onBootCompleted();

        void onReboot();

        void onShutdown();
    }

    private final FooListenerAutoStartManager<FooBootListenerCallbacks> mListenerManager;
    private final FooBootBroadcastReceiver                              mBootBroadcastReceiver;

    public FooBootListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
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

                    @Override
                    public void onBootCompleted()
                    {
                        for (FooBootListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onBootCompleted();
                        }
                        mListenerManager.endTraversing();
                    }

                    @Override
                    public void onReboot()
                    {
                        for (FooBootListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onReboot();
                        }
                        mListenerManager.endTraversing();
                    }

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

            @Override
            public boolean onLastDetach()
            {
                mBootBroadcastReceiver.stop();
                return false;
            }
        });
        mBootBroadcastReceiver = new FooBootBroadcastReceiver(context);
    }

    public void attach(FooBootListenerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
    }

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

        public boolean isStarted()
        {
            return mIsStarted;
        }

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

    public static class FooLockedBootCompletedBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooLockedBootCompletedBroadcastReceiver.class);

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
