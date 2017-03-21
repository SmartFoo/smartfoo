package com.smartfoo.android.core.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.gatt.FooGattUuids;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class FooBluetoothUtils
{
    private FooBluetoothUtils()
    {
    }

    public static boolean isBluetoothSupported(@NonNull Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    public static boolean isBluetoothLowEnergySupported(@NonNull Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * @param context context
     * @return null if Bluetooth is not supported
     */
    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    @Nullable
    public static BluetoothManager getBluetoothManager(@NonNull Context context)
    {
        BluetoothManager bluetoothManager = null;

        if (isBluetoothSupported(context))
        {
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        }

        return bluetoothManager;
    }

    /**
     * Per: http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
     * "To get a BluetoothAdapter representing the local Bluetooth adapter, when running on JELLY_BEAN_MR1 and below,
     * call the static getDefaultAdapter() method; when running on JELLY_BEAN_MR2 and higher, retrieve it through
     * getSystemService(String) with BLUETOOTH_SERVICE. Fundamentally, this is your starting point for all Bluetooth
     * actions."
     *
     * @param context context
     * @return null if Bluetooth is not supported
     */
    @Nullable
    public static BluetoothAdapter getBluetoothAdapter(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");

        BluetoothAdapter bluetoothAdapter = null;

        if (isBluetoothSupported(context))
        {
            if (VERSION.SDK_INT <= VERSION_CODES.JELLY_BEAN_MR1)
            {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            else
            {
                BluetoothManager bluetoothManager = getBluetoothManager(context);
                if (bluetoothManager != null)
                {
                    bluetoothAdapter = bluetoothManager.getAdapter();
                }
            }
        }

        return bluetoothAdapter;
    }

    public static long gattDeviceAddressToLong(@NonNull BluetoothGatt gatt)
    {
        return bluetoothDeviceAddressToLong(gatt.getDevice());
    }

    public static long bluetoothDeviceAddressToLong(@NonNull BluetoothDevice device)
    {
        return macAddressStringToLong(device.getAddress());
    }

    public static String gattDeviceAddressToPrettyString(@NonNull BluetoothGatt gatt)
    {
        return bluetoothDeviceAddressToPrettyString(gatt.getDevice());
    }

    public static String bluetoothDeviceAddressToPrettyString(@NonNull BluetoothDevice device)
    {
        return macAddressStringToPrettyString(device.getAddress());
    }

    public static String getShortDeviceAddressString(String deviceAddress)
    {
        if (deviceAddress != null)
        {
            deviceAddress = macAddressStringToStrippedLowerCaseString(deviceAddress);
            int start = Math.max(deviceAddress.length() - 4, 0);
            deviceAddress = deviceAddress.substring(start, deviceAddress.length());
            deviceAddress = deviceAddress.toUpperCase();
        }
        if (FooString.isNullOrEmpty(deviceAddress))
        {
            deviceAddress = "null";
        }
        return deviceAddress;
    }

    public static String getShortDeviceAddressString(long deviceAddress)
    {
        return getShortDeviceAddressString(macAddressLongToString(deviceAddress));
    }

    public static String macAddressStringToStrippedLowerCaseString(@NonNull String macAddress)
    {
        return macAddress.replace(":", "").toLowerCase();
    }

    public static long macAddressStringToLong(@NonNull String macAddress)
    {
        /*
        if (macAddress == null || macAddress.length() != 17)
        {
            throw new IllegalArgumentException("macAddress (" + FooString.quote(macAddress) +
                                               ") must be of format \"%02X:%02X:%02X:%02X:%02X:%02X\"");
        }
        */
        return Long.parseLong(macAddressStringToStrippedLowerCaseString(macAddress), 16);
    }

    public static String macAddressStringToPrettyString(@NonNull String macAddress)
    {
        return macAddressLongToPrettyString(macAddressStringToLong(macAddress));
    }

    public static String macAddressLongToPrettyString(long macAddress)
    {
        //noinspection PointlessBitwiseExpression
        return String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X",
                (byte) ((macAddress >> 40) & 0xff),
                (byte) ((macAddress >> 32) & 0xff),
                (byte) ((macAddress >> 24) & 0xff),
                (byte) ((macAddress >> 16) & 0xff),
                (byte) ((macAddress >> 8) & 0xff),
                (byte) ((macAddress >> 0) & 0xff));
    }

    public static String macAddressLongToString(long macAddressLong)
    {
        return String.format(Locale.US, "%012x", macAddressLong);
    }

    public static String bluetoothAdapterStateToString(int bluetoothAdapterState)
    {
        String name;
        switch (bluetoothAdapterState)
        {
            case BluetoothAdapter.STATE_OFF:
                name = "STATE_OFF";
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                name = "STATE_TURNING_ON";
                break;
            case BluetoothAdapter.STATE_ON:
                name = "STATE_ON";
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                name = "STATE_TURNING_OFF";
                break;
            default:
                name = "UNKNOWN";
                break;
        }
        return name + '(' + bluetoothAdapterState + ')';
    }

    public static String bluetoothProfileStateToString(int bluetoothProfileState)
    {
        String name;
        switch (bluetoothProfileState)
        {
            case BluetoothProfile.STATE_DISCONNECTED:
                name = "STATE_DISCONNECTED";
                break;
            case BluetoothProfile.STATE_CONNECTING:
                name = "STATE_CONNECTING";
                break;
            case BluetoothProfile.STATE_CONNECTED:
                name = "STATE_CONNECTED";
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                name = "STATE_DISCONNECTING";
                break;
            default:
                name = "UNKNOWN";
                break;
        }
        return name + '(' + bluetoothProfileState + ')';
    }

    public static String bluetoothHeadsetAudioStateToString(int bluetoothHeadsetAudioState)
    {
        String name;
        switch (bluetoothHeadsetAudioState)
        {
            case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                name = "STATE_AUDIO_DISCONNECTED";
                break;
            case BluetoothHeadset.STATE_AUDIO_CONNECTING:
                name = "STATE_AUDIO_CONNECTING";
                break;
            case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                name = "STATE_AUDIO_CONNECTED";
                break;
            default:
                name = "UNKNOWN";
                break;
        }
        return name + '(' + bluetoothHeadsetAudioState + ')';
    }

    public static String scanCallbackErrorToString(int value)
    {
        String name;
        switch (value)
        {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                name = "SCAN_FAILED_ALREADY_STARTED";
                break;
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                name = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
                break;
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                name = "SCAN_FAILED_INTERNAL_ERROR";
                break;
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                name = "SCAN_FAILED_FEATURE_UNSUPPORTED";
                break;
            default:
                name = "UNKNOWN";
                break;
        }
        return name + '(' + value + ')';
    }

    public static String toString(@NonNull BluetoothGattService service, @NonNull BluetoothGattCharacteristic characteristic, Object value)
    {
        return toString(service.getUuid(), characteristic.getUuid(), value);
    }

    public static String toString(@NonNull UUID uuidService, @NonNull UUID uuidCharacteristic, Object value)
    {
        return FooString.quote(FooGattUuids.get(uuidService).getName())
               + "\\" + FooString.quote(FooGattUuids.get(uuidCharacteristic).getName())
               + "\\" + FooString.quote(value);
    }

    /**
     * Returns a string composed from a {@link SparseArray}.
     *
     * @param array array
     * @return never null
     */
    public static String toString(SparseArray<byte[]> array)
    {
        if (array == null)
        {
            return "null";
        }
        if (array.size() == 0)
        {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        for (int i = 0; i < array.size(); ++i)
        {
            buffer.append(array.keyAt(i)).append("=").append(Arrays.toString(array.valueAt(i)));
        }
        buffer.append('}');
        return buffer.toString();
    }

    /**
     * Returns a string composed from a {@link Map}.
     */
    static <T> String toString(Map<T, byte[]> map)
    {
        if (map == null)
        {
            return "null";
        }
        if (map.isEmpty())
        {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        Iterator<Entry<T, byte[]>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<T, byte[]> entry = it.next();
            Object key = entry.getKey();
            buffer.append(key).append("=").append(Arrays.toString(map.get(key)));
            if (it.hasNext())
            {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }
}
