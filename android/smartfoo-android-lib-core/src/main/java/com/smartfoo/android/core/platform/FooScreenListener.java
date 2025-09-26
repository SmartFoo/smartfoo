package com.smartfoo.android.core.platform;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserManager;
import android.view.Display;

import androidx.annotation.NonNull;

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

        void onScreenDim();

        void onScreenUndim();

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
                    public void onScreenDim()
                    {
                        for (FooScreenListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onScreenDim();
                        }
                        mListenerManager.endTraversing();
                    }

                    @Override
                    public void onScreenUndim()
                    {
                        for (FooScreenListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onScreenUndim();
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

    public boolean isScreenOn()
    {
        return mScreenBroadcastReceiver.isScreenOn();
    }

    public boolean isScreenDim()
    {
        return mScreenBroadcastReceiver.isScreenDim();
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

        private final Context                        mContext;
        private final Object                         mSyncLock;
        private final DisplayManager                 mDisplayManager;
        private final Handler                        mHandler;
        private final DisplayManager.DisplayListener mDisplayListener;

        private boolean                    mIsStarted;
        private FooScreenListenerCallbacks mCallbacks;
        private boolean                    mIsScreenOn;
        private boolean                    mIsScreenDim;

        private FooScreenBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
            mSyncLock = new Object();
            mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            mHandler = new Handler(context.getMainLooper());
            mDisplayListener = new DisplayManager.DisplayListener()
            {
                @Override
                public void onDisplayAdded(int displayId)
                {
                    refreshScreenState();
                }

                @Override
                public void onDisplayChanged(int displayId)
                {
                    refreshScreenState();
                }

                @Override
                public void onDisplayRemoved(int displayId)
                {
                    refreshScreenState();
                }
            };
        }

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

        public boolean isScreenDim()
        {
            boolean isScreenDim = false;
            for (Display display : mDisplayManager.getDisplays())
            {
                int state = display.getState();
                if (state == Display.STATE_OFF)
                {
                    continue;
                }
                if (isDisplayStateDim(state))
                {
                    isScreenDim = true;
                    break;
                }
            }
            return isScreenDim;
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

                    mIsScreenOn = isScreenOn();
                    mIsScreenDim = mIsScreenOn && isScreenDim();

                    mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Intent.ACTION_SCREEN_OFF); // API 1
                    intentFilter.addAction(Intent.ACTION_SCREEN_ON); // API 1
                    intentFilter.addAction(Intent.ACTION_USER_UNLOCKED); // API 24
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
                    mDisplayManager.unregisterDisplayListener(mDisplayListener);

                    mCallbacks = null;
                    mIsScreenOn = false;
                    mIsScreenDim = false;
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
                    refreshScreenState();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    refreshScreenState();
                    break;
                case Intent.ACTION_USER_UNLOCKED:
                    FooScreenListenerCallbacks callbacks;
                    synchronized (mSyncLock)
                    {
                        callbacks = mCallbacks;
                    }
                    if (callbacks != null)
                    {
                        callbacks.onUserUnlocked();
                    }
                    break;
            }
        }

        private void refreshScreenState()
        {
            FooScreenListenerCallbacks callbacks;
            boolean notifyScreenOff = false;
            boolean notifyScreenOn = false;
            boolean notifyScreenDim = false;
            boolean notifyScreenUndim = false;

            synchronized (mSyncLock)
            {
                if (!mIsStarted)
                {
                    return;
                }

                callbacks = mCallbacks;

                boolean isScreenOn = isScreenOn();
                if (mIsScreenOn != isScreenOn)
                {
                    mIsScreenOn = isScreenOn;
                    if (isScreenOn)
                    {
                        notifyScreenOn = true;
                    }
                    else
                    {
                        notifyScreenOff = true;
                        mIsScreenDim = false;
                    }
                }

                if (mIsScreenOn)
                {
                    boolean isScreenDim = isScreenDim();
                    if (mIsScreenDim != isScreenDim)
                    {
                        mIsScreenDim = isScreenDim;
                        if (isScreenDim)
                        {
                            notifyScreenDim = true;
                        }
                        else
                        {
                            notifyScreenUndim = true;
                        }
                    }
                }
                else
                {
                    mIsScreenDim = false;
                }
            }

            if (callbacks == null)
            {
                return;
            }

            if (notifyScreenOff)
            {
                callbacks.onScreenOff();
            }
            if (notifyScreenOn)
            {
                callbacks.onScreenOn();
            }
            if (notifyScreenDim)
            {
                callbacks.onScreenDim();
            }
            if (notifyScreenUndim)
            {
                callbacks.onScreenUndim();
            }
        }

        private static boolean isDisplayStateDim(int state)
        {
            switch (state)
            {
                case Display.STATE_DOZE:
                case Display.STATE_DOZE_SUSPEND:
                case Display.STATE_ON_SUSPEND:
                    return true;
                default:
                    return false;
            }
        }
    }
}
