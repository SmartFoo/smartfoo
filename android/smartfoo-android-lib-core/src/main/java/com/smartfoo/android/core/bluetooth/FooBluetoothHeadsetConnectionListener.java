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
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inspiration:
 * http://blog.urvatechlabs.com/detect-programatically-if-headphone-or-bluetooth-headsets-attached-with-android-phone/
 */
public class FooBluetoothHeadsetConnectionListener
{
    public interface OnBluetoothHeadsetConnectionCallbacks
    {
        void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice);

        void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice);
    }

    private final FooListenerManager<OnBluetoothHeadsetConnectionCallbacks> mListenerManager;
    private final FooBluetoothHeadsetConnectionBroadcastReceiver            mBluetoothConnectionBroadcastReceiver;

    public FooBluetoothHeadsetConnectionListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mListenerManager = new FooListenerManager<>();
        mBluetoothConnectionBroadcastReceiver = new FooBluetoothHeadsetConnectionBroadcastReceiver(context);
    }

    public boolean isBluetoothHeadsetConnected()
    {
        return mBluetoothConnectionBroadcastReceiver.isBluetoothHeadsetConnected();
    }

    @NonNull
    public Map<String, BluetoothDevice> getConnectedBluetoothHeadsets()
    {
        return mBluetoothConnectionBroadcastReceiver.getConnectedBluetoothHeadsets();
    }

    public void attach(@NonNull OnBluetoothHeadsetConnectionCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.attach(callbacks);
        if (mListenerManager.size() == 1 && !mBluetoothConnectionBroadcastReceiver.isStarted())
        {
            mBluetoothConnectionBroadcastReceiver.start(new OnBluetoothHeadsetConnectionCallbacks()
            {
                @Override
                public void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
                {
                    for (OnBluetoothHeadsetConnectionCallbacks callbacks : mListenerManager.beginTraversing())
                    {
                        callbacks.onBluetoothHeadsetConnected(bluetoothDevice);
                    }
                    mListenerManager.endTraversing();
                }

                @Override
                public void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
                {
                    for (OnBluetoothHeadsetConnectionCallbacks callbacks : mListenerManager.beginTraversing())
                    {
                        callbacks.onBluetoothHeadsetDisconnected(bluetoothDevice);
                    }
                    mListenerManager.endTraversing();
                }
            });
        }
    }

    public void detach(@NonNull OnBluetoothHeadsetConnectionCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.detach(callbacks);
        if (mListenerManager.size() == 0 && mBluetoothConnectionBroadcastReceiver.isStarted())
        {
            mBluetoothConnectionBroadcastReceiver.stop();
        }
    }

    private static class FooBluetoothHeadsetConnectionBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooBluetoothHeadsetConnectionBroadcastReceiver.class);

        class FooBluetoothServiceListener
                implements ServiceListener
        {
            private final BluetoothAdapter                      mBluetoothAdapter;
            private final int                                   mBluetoothProfileId;
            private final OnBluetoothHeadsetConnectionCallbacks mCallbacks;

            FooBluetoothServiceListener(@NonNull BluetoothAdapter bluetoothAdapter,
                                        int bluetoothProfileId,
                                        @NonNull OnBluetoothHeadsetConnectionCallbacks callbacks)
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

                /*
                if (proxy instanceof BluetoothHeadset)
                {
                    BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) proxy;
                    //...
                }
                */

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

        private final Context                      mContext;
        private final Object                       mSyncLock;
        private final BluetoothAdapter             mBluetoothAdapter;
        private final Map<String, BluetoothDevice> mConnectedBluetoothHeadsets;

        private boolean                               mIsStarted;
        private OnBluetoothHeadsetConnectionCallbacks mCallbacks;

        public FooBluetoothHeadsetConnectionBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
            mSyncLock = new Object();
            mBluetoothAdapter = FooBluetoothUtils.getBluetoothAdapter(context);
            mConnectedBluetoothHeadsets = new HashMap<>();
        }

        public boolean isBluetoothHeadsetConnected()
        {
            synchronized (mSyncLock)
            {
                return mConnectedBluetoothHeadsets.size() > 0;
            }
        }

        @NonNull
        public Map<String, BluetoothDevice> getConnectedBluetoothHeadsets()
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

        public boolean start(@NonNull OnBluetoothHeadsetConnectionCallbacks callbacks)
        {
            FooLog.v(TAG, "+start(...)");
            if (mBluetoothAdapter != null)
            {
                synchronized (mSyncLock)
                {
                    if (!mIsStarted)
                    {
                        mIsStarted = true;

                        mCallbacks = callbacks;

                        int bluetoothProfileId = BluetoothProfile.HEADSET;
                        FooBluetoothServiceListener bluetoothServiceListener = new FooBluetoothServiceListener(mBluetoothAdapter, bluetoothProfileId, new OnBluetoothHeadsetConnectionCallbacks()
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
                        mBluetoothAdapter.getProfileProxy(mContext, bluetoothServiceListener, bluetoothProfileId);

                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
                        mContext.registerReceiver(this, intentFilter);
                    }
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
            FooLog.v(TAG, "onReceive: intent == " + FooPlatformUtils.toString(intent));
            String action = intent.getAction();
            switch (action)
            {
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int stateCurrent = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                    int statePrevious = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                    FooLog.v(TAG, "onReceive: bluetoothDevice == " + bluetoothDevice);
                    FooLog.v(TAG, "onReceive: stateCurrent == " +
                                  FooBluetoothUtils.bluetoothProfileStateToString(stateCurrent));
                    FooLog.v(TAG, "onReceive: statePrevious == " +
                                  FooBluetoothUtils.bluetoothProfileStateToString(statePrevious));
                    onBluetoothHeadsetConnectionStateChanged(bluetoothDevice, stateCurrent, statePrevious);
                    break;
                }
                case BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED:
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int stateCurrent = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                    int statePrevious = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                    FooLog.v(TAG, "onReceive: bluetoothDevice == " + bluetoothDevice);
                    FooLog.v(TAG, "onReceive: stateCurrent == " +
                                  FooBluetoothUtils.bluetoothHeadsetAudioStateToString(stateCurrent));
                    FooLog.v(TAG, "onReceive: statePrevious == " +
                                  FooBluetoothUtils.bluetoothHeadsetAudioStateToString(statePrevious));
                    break;
                }
            }
        }

        private void onBluetoothHeadsetConnectionStateChanged(BluetoothDevice bluetoothDevice, int stateCurrent, int statePrevious)
        {
            FooLog.v(TAG, "onBluetoothHeadsetConnectionStateChanged: bluetoothDevice == " + bluetoothDevice);
            FooLog.v(TAG, "onBluetoothHeadsetConnectionStateChanged: stateCurrent == " +
                          FooBluetoothUtils.bluetoothProfileStateToString(stateCurrent));
            FooLog.v(TAG, "onBluetoothHeadsetConnectionStateChanged: statePrevious == " +
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

        private void onBluetoothHeadsetConnected(@NonNull BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothHeadsets.put(bluetoothDeviceAddress, bluetoothDevice);
                if (previousValue == null)
                {
                    FooLog.i(TAG, "onBluetoothHeadsetConnected: bluetoothDevice == " + bluetoothDevice);
                    mCallbacks.onBluetoothHeadsetConnected(bluetoothDevice);
                }
            }
        }

        private void onBluetoothHeadsetDisconnected(@NonNull BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothHeadsets.remove(bluetoothDeviceAddress);
                if (previousValue != null)
                {
                    FooLog.i(TAG, "onBluetoothHeadsetDisconnected: bluetoothDevice == " + bluetoothDevice);
                    mCallbacks.onBluetoothHeadsetDisconnected(bluetoothDevice);
                }
            }
        }
    }
}
