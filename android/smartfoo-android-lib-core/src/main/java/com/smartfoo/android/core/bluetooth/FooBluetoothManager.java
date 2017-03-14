package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.logging.FooLog;

public class FooBluetoothManager
{
    private static final String TAG = FooLog.TAG(FooBluetoothManager.class);

    private final boolean                               mIsBluetoothSupported;
    private final boolean                               mIsBluetoothLowEnergySupported;
    private final BluetoothManager                      mBluetoothManager;
    private final BluetoothAdapter                      mBluetoothAdapter;
    private final FooBluetoothHeadsetConnectionListener mBluetoothHeadsetConnectionListener;
    private final FooBluetoothAdapterStateListener      mBluetoothAdapterStateListener;

    public FooBluetoothManager(
            @NonNull
                    Context applicationContext)
    {
        mIsBluetoothSupported = FooBluetoothUtils.isBluetoothSupported(applicationContext);
        mIsBluetoothLowEnergySupported = FooBluetoothUtils.isBluetoothLowEnergySupported(applicationContext);

        mBluetoothManager = FooBluetoothUtils.getBluetoothManager(applicationContext);
        mBluetoothAdapter = FooBluetoothUtils.getBluetoothAdapter(applicationContext);

        mBluetoothHeadsetConnectionListener = new FooBluetoothHeadsetConnectionListener(applicationContext);

        mBluetoothAdapterStateListener = new FooBluetoothAdapterStateListener(applicationContext);
    }

    public boolean isBluetoothSupported()
    {
        return mIsBluetoothSupported;
    }

    public boolean isBluetoothLowEnergySupported()
    {
        return mIsBluetoothLowEnergySupported;
    }

    public BluetoothManager getBluetoothManager()
    {
        return mBluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter()
    {
        return mBluetoothAdapter;
    }

    /**
     * @return true if successfully toggled
     * @see <ul>
     * <li><a href="https://code.google.com/p/android/issues/detail?id=67272">https://code.google.com/p/android/issues/detail?id=67272</a></li>
     * <li><a href="https://github.com/RadiusNetworks/android-ibeacon-service/issues/16">https://github.com/RadiusNetworks/android-ibeacon-service/issues/16</a></li>
     * </ul>
     */
    public boolean bluetoothAdapterStateToggle()
    {
        return bluetoothAdapterEnable(!isBluetoothAdapterEnabled());
    }

    /**
     * @param on on
     * @return true if successfully set; false if the set failed
     * @see <ul>
     * <li><a href="https://code.google.com/p/android/issues/detail?id=67272">https://code.google.com/p/android/issues/detail?id=67272</a></li>
     * <li><a href="https://github.com/RadiusNetworks/android-ibeacon-service/issues/16">https://github.com/RadiusNetworks/android-ibeacon-service/issues/16</a></li>
     * </ul>
     */
    public boolean bluetoothAdapterEnable(boolean on)
    {
        if (mBluetoothAdapter == null)
        {
            return false;
        }

        // TODO:(pv) Known to sometimes throw DeadObjectException
        //  https://code.google.com/p/android/issues/detail?id=67272
        //  https://github.com/RadiusNetworks/android-ibeacon-service/issues/16

        if (on)
        {
            try
            {
                mBluetoothAdapter.enable();
                return true;
            }
            catch (Exception e)
            {
                FooLog.v(TAG, "bluetoothAdapterEnable: mBluetoothAdapter.enable()", e);
                return false;
            }
        }
        else
        {
            try
            {
                mBluetoothAdapter.disable();
                return true;
            }
            catch (Exception e)
            {
                FooLog.v(TAG, "bluetoothAdapterEnable: mBluetoothAdapter.disable()", e);
                return false;
            }
        }
    }

    public boolean isBluetoothAdapterEnabled()
    {
        try
        {
            // TODO:(pv) Known to sometimes throw DeadObjectException
            //  https://code.google.com/p/android/issues/detail?id=67272
            //  https://github.com/RadiusNetworks/android-ibeacon-service/issues/16
            return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
        }
        catch (Exception e)
        {
            FooLog.v(TAG, "isBluetoothAdapterEnabled: mBluetoothAdapter.isEnabled()", e);
            return false;
        }
    }

    public FooBluetoothHeadsetConnectionListener getBluetoothHeadsetConnectionListener()
    {
        return mBluetoothHeadsetConnectionListener;
    }

    public FooBluetoothAdapterStateListener getBluetoothAdapterStateListener()
    {
        return mBluetoothAdapterStateListener;
    }
}
