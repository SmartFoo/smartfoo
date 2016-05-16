package com.smartfoo.android.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.util.SparseArray;

import com.smartfoo.android.core.FooString;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * "Compat" API18 port of hidden API21 {@link android.bluetooth.le.BluetoothLeUtils}
 * Helper class for Bluetooth LE utils.
 */
public class FooBluetoothUtils
{
    //
    // Non-BluetoothLeUtils: BEGIN
    //

    public static long gattDeviceAddressToLong(BluetoothGatt gatt)
    {
        return bluetoothDeviceAddressToLong(gatt.getDevice());
    }

    public static long bluetoothDeviceAddressToLong(BluetoothDevice device)
    {
        return macAddressStringToLong(device.getAddress());
    }

    public static String gattDeviceAddressToPrettyString(BluetoothGatt gatt)
    {
        return bluetoothDeviceAddressToPrettyString(gatt.getDevice());
    }

    public static String bluetoothDeviceAddressToPrettyString(BluetoothDevice device)
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

    public static String macAddressStringToStrippedLowerCaseString(String macAddress)
    {
        if (FooString.isNullOrEmpty(macAddress))
        {
            throw new IllegalArgumentException("macAddress must not be null/\"\"");
        }
        return macAddress.replace(":", "").toLowerCase();
    }

    public static long macAddressStringToLong(String macAddress)
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

    public static String macAddressStringToPrettyString(String macAddress)
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

    //
    // Non-BluetoothLeUtils: END
    //

    private FooBluetoothUtils()
    {
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

    public static String toString(BluetoothGattService service, BluetoothGattCharacteristic characteristic, Object value)
    {
        return toString(service.getUuid(), characteristic.getUuid(), value);
    }

    public static String toString(UUID uuidService, UUID uuidCharacteristic, Object value)
    {
        return FooString.quote(FooGattUuids.get(uuidService).getName())
               + "\\" + FooString.quote(FooGattUuids.get(uuidCharacteristic).getName())
               + "\\" + FooString.quote(value);
    }

    /**
     * Returns a string composed from a {@link SparseArray}.
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
