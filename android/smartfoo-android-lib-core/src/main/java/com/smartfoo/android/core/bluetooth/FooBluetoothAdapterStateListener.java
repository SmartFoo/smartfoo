package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

public class FooBluetoothAdapterStateListener
{
    public interface FooBluetoothAdapterStateCallbacks
    {
        void onBluetoothAdapterEnabled();

        void onBluetoothAdapterDisabled();
    }

    private final FooBluetoothAdapterStateBroadcastReceiver mBluetoothAdapterStateReceiver;

    public FooBluetoothAdapterStateListener(Context context)
    {
        mBluetoothAdapterStateReceiver = new FooBluetoothAdapterStateBroadcastReceiver(context);
    }

    public boolean isBluetoothAdapterEnabled()
    {
        return mBluetoothAdapterStateReceiver.isBluetoothAdapterEnabled();
    }

    public int getBluetoothAdapterState()
    {
        return mBluetoothAdapterStateReceiver.getBluetoothAdapterState();
    }

    public boolean isStarted()
    {
        return mBluetoothAdapterStateReceiver.isStarted();
    }

    /**
     * @param callbacks callbacks
     * @return {@link #isBluetoothAdapterEnabled()}
     */
    public boolean start(FooBluetoothAdapterStateCallbacks callbacks)
    {
        mBluetoothAdapterStateReceiver.start(callbacks);

        return mBluetoothAdapterStateReceiver.isBluetoothAdapterEnabled();
    }

    public void stop()
    {
        mBluetoothAdapterStateReceiver.stop();
    }

    private static class FooBluetoothAdapterStateBroadcastReceiver
            extends BroadcastReceiver
    {
        private final String TAG = FooLog.TAG(FooBluetoothAdapterStateBroadcastReceiver.class);

        private final Context          mContext;
        private final BluetoothAdapter mBluetoothAdapter;

        private final Object mSyncLock = new Object();

        private boolean mIsStarted;
        private int     mPreviousBluetoothAdapterState;
        private boolean mPreviousBluetoothAdapterEnabled;

        private FooBluetoothAdapterStateCallbacks mCallbacks;

        public FooBluetoothAdapterStateBroadcastReceiver(Context context)
        {
            if (context == null)
            {
                throw new IllegalArgumentException("context must not be null");
            }

            mContext = context;
            mBluetoothAdapter = FooBluetoothUtils.getBluetoothAdapter(context);
        }

        public static boolean isBluetoothAdapterEnabled(int bluetoothAdapterState)
        {
            return bluetoothAdapterState == BluetoothAdapter.STATE_ON;
        }

        public boolean isBluetoothAdapterEnabled()
        {
            return getBluetoothAdapterState() == BluetoothAdapter.STATE_ON;
        }

        /**
         * @return {@link BluetoothAdapter#STATE_OFF}, {@link BluetoothAdapter#STATE_TURNING_ON}, {@link
         * BluetoothAdapter#STATE_ON}, {@link BluetoothAdapter#STATE_TURNING_OFF}, or -1 if Bluetooth is not supported
         */
        public int getBluetoothAdapterState()
        {
            try
            {
                // TODO:(pv) Known to sometimes throw DeadObjectException
                //  https://code.google.com/p/android/issues/detail?id=67272
                //  https://github.com/RadiusNetworks/android-ibeacon-service/issues/16
                return mBluetoothAdapter != null ? mBluetoothAdapter.getState() : -1;
            }
            catch (Exception e)
            {
                FooLog.v(TAG, "isBluetoothAdapterEnabled: mBluetoothAdapter.isEnabled()", e);
                return -1;
            }
        }

        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

        /**
         * @param callbacks
         * @return true if started, otherwise false
         */
        public boolean start(FooBluetoothAdapterStateCallbacks callbacks)
        {
            if (mBluetoothAdapter == null)
            {
                return false;
            }

            FooLog.v(TAG, "+start(...)");
            synchronized (mSyncLock)
            {
                mCallbacks = callbacks;

                if (!mIsStarted)
                {
                    mIsStarted = true;

                    onBluetoothAdapterStateChanged();

                    IntentFilter filter = new IntentFilter();
                    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                    mContext.registerReceiver(this, filter);
                }
            }
            FooLog.v(TAG, "-start(...)");

            return true;
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
            String action = intent.getAction();
            FooLog.v(TAG, "onReceive: action=" + FooString.quote(action));

            int bluetoothAdapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            FooLog.v(TAG, "onReceive: bluetoothAdapterState=" +
                          FooBluetoothUtils.bluetoothAdapterStateToString(bluetoothAdapterState));

            onBluetoothAdapterStateChanged(bluetoothAdapterState);
        }

        private void onBluetoothAdapterStateChanged()
        {
            onBluetoothAdapterStateChanged(getBluetoothAdapterState());
        }

        private void onBluetoothAdapterStateChanged(int bluetoothAdapterState)
        {
            FooLog.v(TAG, "onBluetoothAdapterStateChanged(bluetoothAdapterState=" +
                          FooBluetoothUtils.bluetoothAdapterStateToString(bluetoothAdapterState) + ')');

            synchronized (mSyncLock)
            {
                if (!mIsStarted)
                {
                    FooLog.v(TAG, "onBluetoothAdapterStateChanged: mIsStarted == false; ignoring");
                    return;
                }

                if (bluetoothAdapterState == mPreviousBluetoothAdapterState)
                {
                    FooLog.v(TAG, "onBluetoothAdapterStateChanged: bluetoothAdapterState == mPreviousBluetoothAdapterState; ignoring");
                    return;
                }

                mPreviousBluetoothAdapterState = bluetoothAdapterState;
                FooLog.v(TAG, "onBluetoothAdapterStateChanged: mPreviousBluetoothAdapterState=" +
                              mPreviousBluetoothAdapterState);

                boolean isBluetoothAdapterEnabled = isBluetoothAdapterEnabled(bluetoothAdapterState);

                if (isBluetoothAdapterEnabled == mPreviousBluetoothAdapterEnabled)
                {
                    FooLog.v(TAG, "onBluetoothAdapterStateChanged: isBluetoothAdapterEnabled == mPreviousBluetoothAdapterEnabled; ignoring");
                    return;
                }

                mPreviousBluetoothAdapterEnabled = isBluetoothAdapterEnabled;
                FooLog.v(TAG, "onBluetoothAdapterStateChanged: mPreviousBluetoothAdapterEnabled=" +
                              mPreviousBluetoothAdapterEnabled);

                if (mCallbacks == null)
                {
                    FooLog.v(TAG, "onBluetoothAdapterStateChanged: mCallbacks == null; ignoring");
                    return;
                }

                if (isBluetoothAdapterEnabled)
                {
                    FooLog.i(TAG, "onBluetoothAdapterStateChanged: #BLUETOOTH ENABLED");
                    mCallbacks.onBluetoothAdapterEnabled();
                }
                else
                {
                    FooLog.i(TAG, "onBluetoothAdapterStateChanged: #BLUETOOTH DISABLED");
                    mCallbacks.onBluetoothAdapterDisabled();
                }
            }
        }
    }
}
