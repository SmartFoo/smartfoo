package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

/**
 * Monitors the Bluetooth adapter power state and notifies a single registered
 * {@link FooBluetoothAdapterStateCallbacks} listener when the adapter transitions between
 * enabled and disabled.
 *
 * <p>Call {@link #start(FooBluetoothAdapterStateCallbacks)} to begin receiving callbacks and
 * {@link #stop()} to unregister. Duplicate state transitions (e.g. ON→ON) are suppressed.</p>
 */
public class FooBluetoothAdapterStateListener
{
    /**
     * Callback interface for Bluetooth adapter enable/disable events.
     */
    public interface FooBluetoothAdapterStateCallbacks
    {
        /** Called when the Bluetooth adapter transitions to the {@code STATE_ON} state. */
        void onBluetoothAdapterEnabled();

        /** Called when the Bluetooth adapter leaves the {@code STATE_ON} state. */
        void onBluetoothAdapterDisabled();
    }

    private final FooBluetoothAdapterStateBroadcastReceiver mBluetoothAdapterStateReceiver;

    public FooBluetoothAdapterStateListener(Context context)
    {
        mBluetoothAdapterStateReceiver = new FooBluetoothAdapterStateBroadcastReceiver(context);
    }

    /**
     * Returns true if the Bluetooth adapter is currently in the {@code STATE_ON} state.
     *
     * @return true if Bluetooth is enabled
     */
    public boolean isBluetoothAdapterEnabled()
    {
        return mBluetoothAdapterStateReceiver.isBluetoothAdapterEnabled();
    }

    /**
     * Returns the current raw Bluetooth adapter state.
     *
     * @return one of {@link android.bluetooth.BluetoothAdapter#STATE_OFF},
     *         {@link android.bluetooth.BluetoothAdapter#STATE_TURNING_ON},
     *         {@link android.bluetooth.BluetoothAdapter#STATE_ON},
     *         {@link android.bluetooth.BluetoothAdapter#STATE_TURNING_OFF},
     *         or {@code -1} if Bluetooth is not supported on this device
     */
    public int getBluetoothAdapterState()
    {
        return mBluetoothAdapterStateReceiver.getBluetoothAdapterState();
    }

    /**
     * Returns true if this listener has been started and is actively monitoring the adapter state.
     *
     * @return true if started
     */
    public boolean isStarted()
    {
        return mBluetoothAdapterStateReceiver.isStarted();
    }

    /**
     * Registers the given callbacks and begins monitoring Bluetooth adapter state changes.
     * The receiver immediately emits the current adapter state so the caller never misses
     * the initial value.
     *
     * @param callbacks the callbacks to notify; may be null to suppress notifications
     * @return {@link #isBluetoothAdapterEnabled()} at the moment start is called
     */
    public boolean start(FooBluetoothAdapterStateCallbacks callbacks)
    {
        mBluetoothAdapterStateReceiver.start(callbacks);

        return mBluetoothAdapterStateReceiver.isBluetoothAdapterEnabled();
    }

    /**
     * Unregisters the broadcast receiver and stops monitoring Bluetooth adapter state changes.
     * Safe to call even if {@link #start(FooBluetoothAdapterStateCallbacks)} was never called.
     */
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
         * Registers callbacks, starts listening for adapter state broadcasts, and immediately
         * emits the current adapter state so the caller never misses the initial value.
         *
         * @param callbacks the callbacks to notify on adapter state changes; may be null to
         *                  suppress notifications while still monitoring state
         * @return true if the receiver was successfully registered, false if Bluetooth is not
         *         supported on this device
         */
        @SuppressWarnings("UnusedReturnValue")
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
