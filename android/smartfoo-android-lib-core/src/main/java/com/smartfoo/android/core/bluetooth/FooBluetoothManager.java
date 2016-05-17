package com.smartfoo.android.core.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.logging.FooLog;

public class FooBluetoothManager
{
    private static final String TAG = FooLog.TAG(FooBluetoothManager.class);

    public static boolean isBluetoothSupported(Context context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    public static boolean isBluetoothLowEnergySupported(Context context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public static BluetoothManager getBluetoothManager(Context context)
    {
        if (!isBluetoothSupported(context))
        {
            return null;
        }

        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    /**
     * Per: http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
     * "To get a BluetoothAdapter representing the local Bluetooth adapter, when running on JELLY_BEAN_MR1 and below,
     * call the static getDefaultAdapter() method; when running on JELLY_BEAN_MR2 and higher, retrieve it through
     * getSystemService(String) with BLUETOOTH_SERVICE. Fundamentally, this is your starting point for all Bluetooth
     * actions."
     *
     * @return
     */
    public static BluetoothAdapter getBluetoothAdapter(Context context)
    {
        if (!isBluetoothSupported(context))
        {
            return null;
        }

        BluetoothAdapter bluetoothAdapter;
        if (VERSION.SDK_INT <= VERSION_CODES.JELLY_BEAN_MR1)
        {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        else
        {
            //noinspection ConstantConditions
            bluetoothAdapter = getBluetoothManager(context).getAdapter();
        }

        return bluetoothAdapter;
    }

    private final Context                               mApplicationContext;
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
        mApplicationContext = applicationContext;
        mIsBluetoothSupported = isBluetoothSupported(applicationContext);
        mIsBluetoothLowEnergySupported = isBluetoothLowEnergySupported(applicationContext);

        mBluetoothManager = getBluetoothManager(applicationContext);
        mBluetoothAdapter = getBluetoothAdapter(applicationContext);

        if (mBluetoothAdapter != null)
        {
            mBluetoothHeadsetConnectionListener = new FooBluetoothHeadsetConnectionListener(applicationContext, mBluetoothAdapter);
        }
        else
        {
            mBluetoothHeadsetConnectionListener = null;
        }

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
     * @param on
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
