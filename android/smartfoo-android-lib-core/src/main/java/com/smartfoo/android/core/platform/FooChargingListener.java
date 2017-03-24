package com.smartfoo.android.core.platform;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;

public class FooChargingListener
{
    private static final String TAG = FooLog.TAG(FooScreenListener.class);

    public interface FooChargingListenerCallbacks
    {
        void onChargingConnected();

        void onChargingDisconnected();
    }

    public static boolean isCharging(@NonNull Intent intent)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(intent, "intent");
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private final Context                                          mContext;
    private final FooListenerManager<FooChargingListenerCallbacks> mListenerManager;
    private final FooScreenBroadcastReceiver                       mScreenBroadcastReceiver;

    public FooChargingListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
        mListenerManager = new FooListenerManager<>();
        mScreenBroadcastReceiver = new FooScreenBroadcastReceiver(context);
    }

    public boolean isCharging()
    {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);
        //noinspection ConstantConditions
        return isCharging(batteryStatus);
    }

    public void attach(FooChargingListenerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
        if (mListenerManager.size() == 1 && !mScreenBroadcastReceiver.isStarted())
        {
            mScreenBroadcastReceiver.start(new FooChargingListenerCallbacks()
            {
                @Override
                public void onChargingConnected()
                {
                    for (FooChargingListenerCallbacks callbacks : mListenerManager.beginTraversing())
                    {
                        callbacks.onChargingConnected();
                    }
                    mListenerManager.endTraversing();
                }

                @Override
                public void onChargingDisconnected()
                {
                    for (FooChargingListenerCallbacks callbacks : mListenerManager.beginTraversing())
                    {
                        callbacks.onChargingDisconnected();
                    }
                    mListenerManager.endTraversing();
                }
            });
        }
    }

    public void detach(FooChargingListenerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
        if (mListenerManager.size() == 0 && mScreenBroadcastReceiver.isStarted())
        {
            mScreenBroadcastReceiver.stop();
        }
    }

    private static class FooScreenBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooScreenBroadcastReceiver.class);

        private final Context mContext;
        private final Object  mSyncLock;

        private boolean                      mIsStarted;
        private FooChargingListenerCallbacks mCallbacks;

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

        public void start(@NonNull FooChargingListenerCallbacks callbacks)
        {
            FooLog.v(TAG, "+start(...)");
            synchronized (mSyncLock)
            {
                if (!mIsStarted)
                {
                    mIsStarted = true;

                    mCallbacks = callbacks;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Intent.ACTION_POWER_CONNECTED); // API 4
                    intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED); // API 4
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
            if (isCharging(intent))
            {
                mCallbacks.onChargingConnected();
            }
            else
            {
                mCallbacks.onChargingDisconnected();
            }
        }
    }
}
