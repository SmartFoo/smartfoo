package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothA2dp;
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

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inspiration:
 * http://blog.urvatechlabs.com/detect-programatically-if-headphone-or-bluetooth-headsets-attached-with-android-phone/
 * https://developer.android.com/samples/BluetoothChat/src/com.example.android.bluetoothchat/DeviceListActivity.html
 */
public class FooBluetoothAudioConnectionListener
{
    public interface OnBluetoothAudioConnectionCallbacks
    {
        void onBluetoothAudioConnected(BluetoothDevice bluetoothDevice);

        void onBluetoothAudioDisconnected(BluetoothDevice bluetoothDevice);
    }

    private final FooListenerAutoStartManager<OnBluetoothAudioConnectionCallbacks> mListenerManager;
    private final FooBluetoothAudioConnectionBroadcastReceiver                     mBluetoothConnectionBroadcastReceiver;

    public FooBluetoothAudioConnectionListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
            @Override
            public void onFirstAttach()
            {
                mBluetoothConnectionBroadcastReceiver.start(new OnBluetoothAudioConnectionCallbacks()
                {
                    @Override
                    public void onBluetoothAudioConnected(BluetoothDevice bluetoothDevice)
                    {
                        for (OnBluetoothAudioConnectionCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onBluetoothAudioConnected(bluetoothDevice);
                        }
                        mListenerManager.endTraversing();
                    }

                    @Override
                    public void onBluetoothAudioDisconnected(BluetoothDevice bluetoothDevice)
                    {
                        for (OnBluetoothAudioConnectionCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onBluetoothAudioDisconnected(bluetoothDevice);
                        }
                        mListenerManager.endTraversing();
                    }
                });
            }

            @Override
            public boolean onLastDetach()
            {
                mBluetoothConnectionBroadcastReceiver.stop();
                return false;
            }
        });
        mBluetoothConnectionBroadcastReceiver = new FooBluetoothAudioConnectionBroadcastReceiver(context);
    }

    public boolean isBluetoothAudioConnected()
    {
        return mBluetoothConnectionBroadcastReceiver.isBluetoothAudioConnected();
    }

    public boolean isBluetoothAudioConnected(String deviceMacAddress)
    {
        return mBluetoothConnectionBroadcastReceiver.isBluetoothAudioConnected(deviceMacAddress);
    }

    @NonNull
    public Map<String, BluetoothDevice> getConnectedBluetoothAudioDevices()
    {
        return mBluetoothConnectionBroadcastReceiver.getConnectedBluetoothAudioDevices();
    }

    public void attach(@NonNull OnBluetoothAudioConnectionCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.attach(callbacks);
    }

    public void detach(@NonNull OnBluetoothAudioConnectionCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.detach(callbacks);
    }

    private static class FooBluetoothAudioConnectionBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooBluetoothAudioConnectionBroadcastReceiver.class);

        class FooBluetoothAudioServiceListener
                implements ServiceListener
        {
            private final BluetoothAdapter                    mBluetoothAdapter;
            private final OnBluetoothAudioConnectionCallbacks mCallbacks;

            FooBluetoothAudioServiceListener(@NonNull BluetoothAdapter bluetoothAdapter,
                                             @NonNull OnBluetoothAudioConnectionCallbacks callbacks)
            {
                mBluetoothAdapter = bluetoothAdapter;
                mCallbacks = callbacks;
            }

            final int[] BLUETOOTH_AUDIO_PROFILE_IDS = {
                    BluetoothProfile.HEADSET,
                    BluetoothProfile.A2DP,
                    };

            public void start(@NonNull Context context)
            {
                for (int bluetoothAudioProfileId : BLUETOOTH_AUDIO_PROFILE_IDS)
                {
                    mBluetoothAdapter.getProfileProxy(context, this, bluetoothAudioProfileId);
                }
            }

            @Override
            public void onServiceConnected(int profileId, BluetoothProfile proxy)
            {
                FooLog.e(TAG, "onServiceConnected(profileId=" + profileId + ", proxy=" + proxy + ')');

                for (BluetoothDevice bluetoothDevice : proxy.getConnectedDevices())
                {
                    mCallbacks.onBluetoothAudioConnected(bluetoothDevice);
                }

                mBluetoothAdapter.closeProfileProxy(profileId, proxy);
            }

            @Override
            public void onServiceDisconnected(int profileId)
            {
                FooLog.e(TAG, "onServiceDisconnected(profileId=" + profileId + ')');
            }
        }

        private final Context                      mContext;
        private final Object                       mSyncLock;
        private final BluetoothAdapter             mBluetoothAdapter;
        private final Map<String, BluetoothDevice> mConnectedBluetoothAudioDevices;

        private boolean                             mIsStarted;
        private OnBluetoothAudioConnectionCallbacks mCallbacks;

        public FooBluetoothAudioConnectionBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
            mSyncLock = new Object();
            mBluetoothAdapter = FooBluetoothUtils.getBluetoothAdapter(context);
            mConnectedBluetoothAudioDevices = new HashMap<>();
        }

        public boolean isBluetoothAudioConnected()
        {
            synchronized (mSyncLock)
            {
                return mConnectedBluetoothAudioDevices.size() > 0;
            }
        }

        public boolean isBluetoothAudioConnected(String deviceMacAddress)
        {
            synchronized (mSyncLock)
            {
                return mConnectedBluetoothAudioDevices.containsKey(deviceMacAddress);
            }
        }

        @NonNull
        public Map<String, BluetoothDevice> getConnectedBluetoothAudioDevices()
        {
            synchronized (mSyncLock)
            {
                return new LinkedHashMap<>(mConnectedBluetoothAudioDevices);
            }
        }

        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

        public boolean start(@NonNull OnBluetoothAudioConnectionCallbacks callbacks)
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

                        FooBluetoothAudioServiceListener bluetoothAudioServiceListener = new FooBluetoothAudioServiceListener(mBluetoothAdapter, new OnBluetoothAudioConnectionCallbacks()
                        {
                            @Override
                            public void onBluetoothAudioConnected(BluetoothDevice bluetoothDevice)
                            {
                                FooBluetoothAudioConnectionBroadcastReceiver.this.onBluetoothAudioConnected(bluetoothDevice);
                            }

                            @Override
                            public void onBluetoothAudioDisconnected(BluetoothDevice bluetoothDevice)
                            {
                                // ignore during startup
                            }
                        });
                        bluetoothAudioServiceListener.start(mContext);

                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
                        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
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
                    //FooLog.v(TAG, "onReceive: bluetoothDevice == " + bluetoothDevice);
                    if (FooBluetoothUtils.isAudioOutput(bluetoothDevice))
                    {
                        int connectionStateCurrent = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                        int connectionStatePrevious = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                        //FooLog.v(TAG, "onReceive: connectionStateCurrent == " +
                        //              FooBluetoothUtils.bluetoothConnectionStateToString(connectionStateCurrent));
                        //FooLog.v(TAG, "onReceive: connectionStatePrevious == " +
                        //              FooBluetoothUtils.bluetoothConnectionStateToString(connectionStatePrevious));
                        onBluetoothAudioConnectionStateChanged(bluetoothDevice, connectionStateCurrent, connectionStatePrevious);
                    }
                    break;
                }
                case BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED:
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    FooLog.v(TAG, "onReceive: bluetoothDevice == " + bluetoothDevice);
                    if (FooBluetoothUtils.isAudioOutput(bluetoothDevice))
                    {
                        int audioStateCurrent = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                        int audioStatePrevious = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                        FooLog.w(TAG, "onReceive: UNHANDLED audioStateCurrent == " +
                                      FooBluetoothUtils.bluetoothAudioStateToString(audioStateCurrent));
                        FooLog.w(TAG, "onReceive: UNHANDLED audioStatePrevious == " +
                                      FooBluetoothUtils.bluetoothAudioStateToString(audioStatePrevious));
                    }
                    break;
                }
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //FooLog.v(TAG, "onReceive: bluetoothDevice == " + bluetoothDevice);
                    if (FooBluetoothUtils.isAudioOutput(bluetoothDevice))
                    {
                        int connectionStateCurrent = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                        int connectionStatePrevious = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                        //FooLog.v(TAG, "onReceive: connectionStateCurrent == " +
                        //              FooBluetoothUtils.bluetoothConnectionStateToString(connectionStateCurrent));
                        //FooLog.v(TAG, "onReceive: connectionStatePrevious == " +
                        //              FooBluetoothUtils.bluetoothConnectionStateToString(connectionStatePrevious));
                        onBluetoothAudioConnectionStateChanged(bluetoothDevice, connectionStateCurrent, connectionStatePrevious);
                    }
                    break;
                }
            }
        }

        private void onBluetoothAudioConnectionStateChanged(BluetoothDevice bluetoothDevice, int connectionStateCurrent, int connectionStatePrevious)
        {
            FooLog.v(TAG, "onBluetoothAudioConnectionStateChanged: bluetoothDevice == " + bluetoothDevice);
            FooLog.v(TAG, "onBluetoothAudioConnectionStateChanged: connectionStateCurrent == " +
                          FooBluetoothUtils.bluetoothConnectionStateToString(connectionStateCurrent));
            FooLog.v(TAG, "onBluetoothAudioConnectionStateChanged: connectionStatePrevious == " +
                          FooBluetoothUtils.bluetoothConnectionStateToString(connectionStatePrevious));
            if (connectionStateCurrent == BluetoothProfile.STATE_CONNECTED)
            {
                onBluetoothAudioConnected(bluetoothDevice);
            }
            else if (connectionStateCurrent == BluetoothProfile.STATE_DISCONNECTED)
            {
                onBluetoothAudioDisconnected(bluetoothDevice);
            }
        }

        private void onBluetoothAudioConnected(@NonNull BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothAudioDevices.put(bluetoothDeviceAddress, bluetoothDevice);
                if (previousValue == null)
                {
                    FooLog.i(TAG, "onBluetoothAudioConnected: bluetoothDevice == " + bluetoothDevice);
                    mCallbacks.onBluetoothAudioConnected(bluetoothDevice);
                }
            }
        }

        private void onBluetoothAudioDisconnected(@NonNull BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothAudioDevices.remove(bluetoothDeviceAddress);
                if (previousValue != null)
                {
                    FooLog.i(TAG, "onBluetoothAudioDisconnected: bluetoothDevice == " + bluetoothDevice);
                    mCallbacks.onBluetoothAudioDisconnected(bluetoothDevice);
                }
            }
        }
    }
}
