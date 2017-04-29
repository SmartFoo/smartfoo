package com.smartfoo.android.core.platform;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build.VERSION;
import android.os.PowerManager;
import android.os.UserManager;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;

public class FooScreenListener
{
    private static final String TAG = FooLog.TAG(FooScreenListener.class);

    public interface FooScreenListenerCallbacks
    {
        void onScreenOff();

        void onScreenOn();

        void onUserUnlocked();
    }

    private final FooListenerAutoStartManager<FooScreenListenerCallbacks> mListenerManager;
    private final FooScreenBroadcastReceiver                              mScreenBroadcastReceiver;
    private final PowerManager                                            mPowerManager;
    private final KeyguardManager                                         mKeyguardManager;
    private final UserManager                                             mUserManager;

    public FooScreenListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
            @Override
            public void onFirstAttach()
            {
                mScreenBroadcastReceiver.start(new FooScreenListenerCallbacks()
                {
                    @Override
                    public void onScreenOff()
                    {
                        for (FooScreenListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onScreenOff();
                        }
                        mListenerManager.endTraversing();
                    }

                    @Override
                    public void onScreenOn()
                    {
                        for (FooScreenListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onScreenOn();
                        }
                        mListenerManager.endTraversing();
                    }

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

    @SuppressLint("ObsoleteSdkInt")
    public boolean isScreenOn()
    {
        //noinspection deprecation
        return VERSION.SDK_INT >= 20 ? mPowerManager.isInteractive() : mPowerManager.isScreenOn();
    }

    public boolean isUserUnlocked()
    {
        boolean isUserUnlocked = mUserManager.isUserUnlocked();
        FooLog.e(TAG, "isUserUnlocked: isUserUnlocked == " + isUserUnlocked);
        return isUserUnlocked;
    }

    public void attach(FooScreenListenerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
    }

    public void detach(FooScreenListenerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    private static class FooScreenBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooScreenBroadcastReceiver.class);

        private final Context mContext;
        private final Object  mSyncLock;

        private boolean                    mIsStarted;
        private FooScreenListenerCallbacks mCallbacks;

        public FooScreenBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
            mSyncLock = new Object();
        }

        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

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

        @Override
        public void onReceive(Context context, Intent intent)
        {
            FooLog.v(TAG, "onReceive: intent == " + FooPlatformUtils.toString(intent));
            String action = intent.getAction();
            switch (action)
            {
                case Intent.ACTION_SCREEN_OFF:
                    mCallbacks.onScreenOff();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    mCallbacks.onScreenOn();
                    break;
                case Intent.ACTION_USER_UNLOCKED:
                    mCallbacks.onUserUnlocked();
                    break;
            }
        }
    }
}
