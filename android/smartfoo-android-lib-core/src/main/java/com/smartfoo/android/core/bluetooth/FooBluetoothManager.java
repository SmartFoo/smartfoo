package com.smartfoo.android.core.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.logging.FooLog;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Central facade for Bluetooth functionality in an application.
 *
 * <p>Provides convenient access to the platform {@link BluetoothManager} and
 * {@link BluetoothAdapter}, exposes enable/disable helpers with known-bug workarounds, and
 * owns pre-constructed listeners for adapter state changes and Bluetooth audio connections.</p>
 *
 * <p>Construct once with an application {@link android.content.Context} and keep the instance
 * for the application lifetime.</p>
 */
public class FooBluetoothManager
{
    private static final String TAG = FooLog.TAG(FooBluetoothManager.class);

    private final boolean                             mIsBluetoothSupported;
    private final boolean                             mIsBluetoothLowEnergySupported;
    private final BluetoothManager                    mBluetoothManager;
    private final BluetoothAdapter                    mBluetoothAdapter;
    private final FooBluetoothAdapterStateListener    mBluetoothAdapterStateListener;
    private final FooBluetoothAudioConnectionListener mBluetoothAudioConnectionListener;

    public FooBluetoothManager(@NonNull Context applicationContext)
    {
        mIsBluetoothSupported = FooBluetoothUtils.isBluetoothSupported(applicationContext);
        mIsBluetoothLowEnergySupported = FooBluetoothUtils.isBluetoothLowEnergySupported(applicationContext);

        mBluetoothManager = FooBluetoothUtils.getBluetoothManager(applicationContext);
        mBluetoothAdapter = FooBluetoothUtils.getBluetoothAdapter(applicationContext);

        mBluetoothAdapterStateListener = new FooBluetoothAdapterStateListener(applicationContext);

        mBluetoothAudioConnectionListener = new FooBluetoothAudioConnectionListener(applicationContext);
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
     * Returns true if the Bluetooth adapter is currently enabled.
     * Handles the known {@code DeadObjectException} that the framework may throw.
     *
     * @return true if Bluetooth is enabled, false if disabled or if an exception occurs
     */
    public boolean isBluetoothAdapterEnabled()
    {
        try
        {
            // NOTE:(pv) Known to sometimes throw DeadObjectException
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

    /**
     * @return true if successfully toggled
     * @see <ul>
     * <li><a href="https://code.google.com/p/android/issues/detail?id=67272">https://code.google.com/p/android/issues/detail?id=67272</a></li>
     * <li><a href="https://github.com/RadiusNetworks/android-ibeacon-service/issues/16">https://github.com/RadiusNetworks/android-ibeacon-service/issues/16</a></li>
     * </ul>
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

    /**
     * Returns the {@link FooBluetoothAdapterStateListener} associated with this manager.
     * The listener can be used to register for adapter enable/disable callbacks.
     *
     * @return never null
     */
    @NonNull
    public FooBluetoothAdapterStateListener getBluetoothAdapterStateListener()
    {
        return mBluetoothAdapterStateListener;
    }

    private static final Comparator<BluetoothDevice> BLUETOOTH_DEVICE_COMPARATOR = new Comparator<BluetoothDevice>()
    {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public int compare(BluetoothDevice o1, BluetoothDevice o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    };

    /**
     * Returns the set of currently bonded Bluetooth devices sorted by device name.
     *
     * @return a name-sorted set of bonded devices, or null if Bluetooth is not supported
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public SortedSet<BluetoothDevice> getBondedDevices()
    {
        if (mBluetoothAdapter == null)
        {
            return null;
        }

        SortedSet<BluetoothDevice> bondedDevices = new TreeSet<>(BLUETOOTH_DEVICE_COMPARATOR);
        bondedDevices.addAll(mBluetoothAdapter.getBondedDevices());
        return bondedDevices;
    }

    /**
     * Returns the {@link FooBluetoothAudioConnectionListener} associated with this manager.
     *
     * @return never null
     */
    @NonNull
    public FooBluetoothAudioConnectionListener getBluetoothAudioConnectionListener()
    {
        return mBluetoothAudioConnectionListener;
    }
}
