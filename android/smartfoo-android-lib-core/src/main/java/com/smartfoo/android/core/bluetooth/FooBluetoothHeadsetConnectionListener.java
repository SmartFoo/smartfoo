package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
        void onBluetoothDeviceConnected(BluetoothDevice bluetoothDevice);

        void onBluetoothDeviceDisconnected(BluetoothDevice bluetoothDevice);
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
            public void onBluetoothDeviceConnected(BluetoothDevice bluetoothDevice)
            {
                Set<OnBluetoothHeadsetConnectedCallbacks> callbacks = mListenerManager.beginTraversing();
                for (OnBluetoothHeadsetConnectedCallbacks callback : callbacks)
                {
                    callback.onBluetoothDeviceConnected(bluetoothDevice);
                }
                mListenerManager.endTraversing();
            }

            @Override
            public void onBluetoothDeviceDisconnected(BluetoothDevice bluetoothDevice)
            {
                Set<OnBluetoothHeadsetConnectedCallbacks> callbacks = mListenerManager.beginTraversing();
                for (OnBluetoothHeadsetConnectedCallbacks callback : callbacks)
                {
                    callback.onBluetoothDeviceDisconnected(bluetoothDevice);
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
                    mCallbacks.onBluetoothDeviceConnected(bluetoothDevice);
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

            /*
            // TODO:(pv) http://stackoverflow.com/a/12578825/252308
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices != null)
            {
                for (BluetoothDevice bondedDevice : bondedDevices)
                {
                    FooLog.e(TAG, "bondedDevice.getName()=" + bondedDevice.getName());
                    BluetoothClass bluetoothClass = bondedDevice.getBluetoothClass();
                    FooLog.e(TAG, "bondedDevice.getBluetoothClass()=" + bluetoothClass);
                    int majorDeviceClass = bluetoothClass.getMajorDeviceClass();
                    switch (majorDeviceClass)
                    {
                        case Major.AUDIO_VIDEO:
                            FooLog.e(TAG, "AUDIO_VIDEO");
                    }
                }
            }
            */
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
                        public void onBluetoothDeviceConnected(BluetoothDevice bluetoothDevice)
                        {
                            add(bluetoothDevice);
                        }

                        @Override
                        public void onBluetoothDeviceDisconnected(BluetoothDevice bluetoothDevice)
                        {
                        }
                    });
                    mBluetoothAdapter.getProfileProxy(mApplicationContext, bluetoothServiceListener, bluetoothProfileId);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                    intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
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

            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            FooLog.v(TAG, "onReceive: bluetoothDevice=" + bluetoothDevice);

            switch (action)
            {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                {
                    add(bluetoothDevice);
                    break;
                }
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                {
                    remove(bluetoothDevice);
                    break;
                }
            }
        }

        private void add(
                @NonNull
                BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothHeadsets.put(bluetoothDeviceAddress, bluetoothDevice);
                if (previousValue == null)
                {
                    FooLog.e(TAG, "add: bluetoothDevice=" + bluetoothDevice);
                    mCallbacks.onBluetoothDeviceConnected(bluetoothDevice);
                }
            }
        }

        private void remove(
                @NonNull
                BluetoothDevice bluetoothDevice)
        {
            String bluetoothDeviceAddress = bluetoothDevice.getAddress();
            synchronized (mSyncLock)
            {
                BluetoothDevice previousValue = mConnectedBluetoothHeadsets.remove(bluetoothDeviceAddress);
                if (previousValue != null)
                {
                    FooLog.e(TAG, "remove: bluetoothDevice=" + bluetoothDevice);
                    mCallbacks.onBluetoothDeviceDisconnected(bluetoothDevice);
                }
            }
        }
    }

    /*
    private final Context                                               mApplicationContext;
    private final BluetoothAdapter                                      mBluetoothAdapter;
    private final SparseArray<Set<BluetoothDevice>>                     mConnectedDevices;
    private final SparseArray<Set<OnBluetoothDeviceConnectedCallbacks>> mCallbacks;
    private final OnBluetoothDeviceConnectedCallbacks mCallback = new OnBluetoothDeviceConnectedCallbacks()
    {
        @Override
        public void onBluetoothDeviceConnected(int bluetoothProfileId, BluetoothDevice bluetoothDevice)
        {

        }
    };
    */

    /*
    public FooBluetoothDeviceConnectionListener(Context applicationContext, BluetoothAdapter bluetoothAdapter)
    {
        mApplicationContext = applicationContext;
        mBluetoothAdapter = bluetoothAdapter;
        //mConnectedDevices = new SparseArray<>();
        //mCallbacks = new SparseArray<>();
    }
    */

    /*
    public void stop(int bluetoothProfileId,
                     @NonNull
                     OnBluetoothDeviceConnectedCallbacks listener)
    {

    }
    */

    /*
    /* *
     * @param bluetoothProfileId One of {@link android.bluetooth.BluetoothProfile}.* profile ids
     * @param listener
     * @see {@link BluetoothAdapter#getProfileProxy(Context, ServiceListener, int)}
     * /
    public void start(int bluetoothProfileId,
                      @NonNull
                      OnBluetoothDeviceConnectedCallbacks listener)
    {
        Set<OnBluetoothDeviceConnectedCallbacks> callbacks = mCallbacks.get(bluetoothProfileId);
        if (callbacks == null)
        {
            callbacks = new LinkedHashSet<>();
            mCallbacks.put(bluetoothProfileId, callbacks);
        }

        callbacks.add(listener);

        FooBluetoothServiceListener bluetoothServiceListener = new FooBluetoothServiceListener(bluetoothProfileId, callbacks);

        mBluetoothAdapter.getProfileProxy(mApplicationContext, bluetoothServiceListener, bluetoothProfileId);
    }

    private void onBluetoothDeviceConnected(int bluetoothProfileId, BluetoothDevice bluetoothDevice)
    {
        Set<BluetoothDevice> connectedDevices = mConnectedDevices.get(bluetoothProfileId);
        if (connectedDevices == null)
        {
            connectedDevices = new LinkedHashSet<>();
            mConnectedDevices.put(bluetoothProfileId, connectedDevices);
        }

        connectedDevices.add(bluetoothDevice);

        Set<OnBluetoothDeviceConnectedCallbacks> callbacks = mCallbacks.get(bluetoothProfileId);
        if (callbacks == null)
        {
            return;
        }

        for (OnBluetoothDeviceConnectedCallbacks callback : callbacks)
        {
            callback.onBluetoothDeviceConnected(bluetoothProfileId, bluetoothDevice);
        }
    }
    */
}
