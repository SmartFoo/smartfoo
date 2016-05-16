package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.support.annotation.NonNull;

public class FooBluetoothServiceListener
        implements ServiceListener
{
    public interface OnGetConnectedBluetoothDevicesCallbacks
    {
        void onConnectedBluetoothDevice(BluetoothDevice bluetoothDevice);
    }

    private final BluetoothAdapter                        mBluetoothAdapter;
    private final int                                     mBluetoothProfileId;
    private final OnGetConnectedBluetoothDevicesCallbacks mCallbacks;

    public FooBluetoothServiceListener(BluetoothAdapter bluetoothAdapter,
                                       int bluetoothProfileId,
                                       @NonNull
                                       OnGetConnectedBluetoothDevicesCallbacks callbacks)
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
            mCallbacks.onConnectedBluetoothDevice(bluetoothDevice);
        }

        mBluetoothAdapter.closeProfileProxy(mBluetoothProfileId, proxy);
    }

    @Override
    public void onServiceDisconnected(int profile)
    {
        //mCallbacks.onConnectedBluetoothDevice(null);
    }
}
