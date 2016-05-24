package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FooBluetoothHeadsetConnectionListener
{
    public interface OnBluetoothHeadsetConnectedCallbacks
    {
        void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice);

        void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice);
    }

    private final FooBluetoothHeadsetConnectionBroadcastReceiver           mBluetoothConnectionBroadcastReceiver;
    private final FooListenerManager<OnBluetoothHeadsetConnectedCallbacks> mListenerManager;

    public FooBluetoothHeadsetConnectionListener(
            @NonNull
            Context applicationContext,
            @NonNull
            BluetoothAdapter bluetoothAdapter)
    {
        mBluetoothConnectionBroadcastReceiver = new FooBluetoothHeadsetConnectionBroadcastReceiver(applicationContext, bluetoothAdapter);
        mListenerManager = new FooListenerManager<>();
    }

    public boolean isStarted()
    {
        return mBluetoothConnectionBroadcastReceiver.isStarted();
    }

    @NonNull
    public Map<String, BluetoothDevice> getConnectedHeadsets()
    {
        return mBluetoothConnectionBroadcastReceiver.getConnectedHeadsets();
    }

    public void attach(
            @NonNull
            OnBluetoothHeadsetConnectedCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);

        if (isStarted())
        {
            return;
        }

        mBluetoothConnectionBroadcastReceiver.start(new OnBluetoothHeadsetConnectedCallbacks()
        {
            @Override
            public void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
            {
                Set<OnBluetoothHeadsetConnectedCallbacks> callbacks = mListenerManager.beginTraversing();
                for (OnBluetoothHeadsetConnectedCallbacks callback : callbacks)
                {
                    callback.onBluetoothHeadsetConnected(bluetoothDevice);
                }
                mListenerManager.endTraversing();
            }

            @Override
            public void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
            {
                Set<OnBluetoothHeadsetConnectedCallbacks> callbacks = mListenerManager.beginTraversing();
                for (OnBluetoothHeadsetConnectedCallbacks callback : callbacks)
                {
                    callback.onBluetoothHeadsetDisconnected(bluetoothDevice);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    public void detach(
            @NonNull
            OnBluetoothHeadsetConnectedCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);

        if (mListenerManager.isEmpty())
        {
            mBluetoothConnectionBroadcastReceiver.stop();
        }
    }

    private static class FooBluetoothHeadsetConnectionBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooBluetoothHeadsetConnectionBroadcastReceiver.class);

        public class FooBluetoothServiceListener
                implements ServiceListener
        {
            private final BluetoothAdapter                     mBluetoothAdapter;
            private final int                                  mBluetoothProfileId;
            private final OnBluetoothHeadsetConnectedCallbacks mCallbacks;

            public FooBluetoothServiceListener(
                    @NonNull
                    BluetoothAdapter bluetoothAdapter,
                    int bluetoothProfileId,
                    @NonNull
                    OnBluetoothHeadsetConnectedCallbacks callbacks)
            {
                mBluetoothAdapter = bluetoothAdapter;
                mBluetoothProfileId = bluetoothProfileId;
                mCallbacks = callbacks;
            }

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy)
            {
                if (profile != mBluetoothProfileId)
                {
                    return;
                }

                for (BluetoothDevice bluetoothDevice : proxy.getConnectedDevices())
                {
                    mCallbacks.onBluetoothHeadsetConnected(bluetoothDevice);
                }

                mBluetoothAdapter.closeProfileProxy(mBluetoothProfileId, proxy);
            }

            @Override
            public void onServiceDisconnected(int profile)
            {
            }
        }

        private final Context                      mApplicationContext;
        private final BluetoothAdapter             mBluetoothAdapter;
        private final Map<String, BluetoothDevice> mConnectedBluetoothHeadsets;

        private final Object mSyncLock = new Object();

        private boolean mIsStarted;

        private OnBluetoothHeadsetConnectedCallbacks mCallbacks;

        public FooBluetoothHeadsetConnectionBroadcastReceiver(
                @NonNull
                Context applicationContext,
                @NonNull
                BluetoothAdapter bluetoothAdapter)
        {
            mApplicationContext = applicationContext;
            mBluetoothAdapter = bluetoothAdapter;
            mConnectedBluetoothHeadsets = new HashMap<>();
        }

        public Map<String, BluetoothDevice> getConnectedHeadsets()
        {
            synchronized (mSyncLock)
            {
                return new LinkedHashMap<>(mConnectedBluetoothHeadsets);
            }
        }

        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

        public boolean start(
                @NonNull
                OnBluetoothHeadsetConnectedCallbacks callbacks)
        {
            FooLog.v(TAG, "+start(...)");
            synchronized (mSyncLock)
            {
                if (!mIsStarted)
                {
                    mIsStarted = true;

                    mCallbacks = callbacks;

                    int bluetoothProfileId = BluetoothProfile.HEADSET;
                    FooBluetoothServiceListener bluetoothServiceListener = new FooBluetoothServiceListener(mBluetoothAdapter, bluetoothProfileId, new OnBluetoothHeadsetConnectedCallbacks()
                    {
                        @Override
                        public void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
                        {
                            FooBluetoothHeadsetConnectionBroadcastReceiver.this.onBluetoothHeadsetConnected(bluetoothDevice);
                        }

                        @Override
                        public void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
                        {
                            // ignore during startup
                        }
                    });
                    mBluetoothAdapter.getProfileProxy(mApplicationContext, bluetoothServiceListener, bluetoothProfileId);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                    intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
                    mApplicationContext.registerReceiver(this, intentFilter);
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

                    mApplicationContext.unregisterReceiver(this);
                }
            }
            FooLog.v(TAG, "-stop()");
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            FooLog.v(TAG, "onReceive: action=" + FooString.quote(action));

            switch (action)
            {
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int stateCurrent = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                    int statePrevious = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                    FooLog.v(TAG, "onReceive: bluetoothDevice=" + bluetoothDevice);
                    FooLog.v(TAG, "onReceive: stateCurrent=" +
                                  FooBluetoothUtils.bluetoothProfileStateToString(stateCurrent));
                    FooLog.v(TAG, "onReceive: statePrevious=" +
                                  FooBluetoothUtils.bluetoothProfileStateToString(statePrevious));
                    onBluetoothHeadsetConnectionStateChanged(bluetoothDevice, stateCurrent, statePrevious);
                    break;
                }
                case BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED:
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int stateCurrent = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                    int statePrevious = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                    FooLog.v(TAG, "onReceive: bluetoothDevice=" + bluetoothDevice);
                    FooLog.v(TAG, "onReceive: stateCurrent=" +
                                  FooBluetoothUtils.bluetoothHeadsetAudioStateToString(stateCurrent));
                    FooLog.v(TAG, "onReceive: statePrevious=" +
                                  FooBluetoothUtils.bluetoothHeadsetAudioStateToString(statePrevious));
                    break;
                }
            }
        }

        private void onBluetoothHeadsetConnectionStateChanged(BluetoothDevice bluetoothDevice, int stateCurrent, int statePrevious)
        {
            FooLog.v(TAG, "onBluetoothHeadsetConnectionStateChanged: bluetoothDevice=" + bluetoothDevice);
            FooLog.v(TAG, "onBluetoothHeadsetConnectionStateChanged: stateCurrent=" +
                          FooBluetoothUtils.bluetoothProfileStateToString(stateCurrent));
            FooLog.v(TAG, "onBluetoothHeadsetConnectionStateChanged: statePrevious=" +
                          FooBluetoothUtils.bluetoothProfileStateToString(statePrevious));
            if (stateCurrent == BluetoothProfile.STATE_CONNECTED)
            {
                onBluetoothHeadsetConnected(bluetoothDevice);
            }
            else if (stateCurrent == BluetoothProfile.STATE_DISCONNECTED)
            {
                onBluetoothHeadsetDisconnected(bluetoothDevice);
            }
        }

        private void onBluetoothHeadsetConnected(
                @NonNull
                BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothHeadsets.put(bluetoothDeviceAddress, bluetoothDevice);
                if (previousValue == null)
                {
                    FooLog.i(TAG, "onBluetoothHeadsetConnected: bluetoothDevice=" + bluetoothDevice);
                    mCallbacks.onBluetoothHeadsetConnected(bluetoothDevice);
                }
            }
        }

        private void onBluetoothHeadsetDisconnected(
                @NonNull
                BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothHeadsets.remove(bluetoothDeviceAddress);
                if (previousValue != null)
                {
                    FooLog.i(TAG, "onBluetoothHeadsetDisconnected: bluetoothDevice=" + bluetoothDevice);
                    mCallbacks.onBluetoothHeadsetDisconnected(bluetoothDevice);
                }
            }
        }
    }
}
