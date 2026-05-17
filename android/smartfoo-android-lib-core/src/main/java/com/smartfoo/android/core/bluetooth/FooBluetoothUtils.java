package com.smartfoo.android.core.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothClass.Device.Major;
import android.bluetooth.BluetoothClass.Service;
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
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.gatt.FooGattUuid;
import com.smartfoo.android.core.bluetooth.gatt.FooGattUuids;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Static utility methods for common Bluetooth operations.
 *
 * <p>Covers adapter and connection state string conversion, MAC address parsing and formatting,
 * GATT characteristic/service descriptions, device audio classification, and BLE scan error
 * decoding. This class is not instantiable.</p>
 */
public class FooBluetoothUtils
{
    private FooBluetoothUtils()
    {
    }

    /**
     * Returns true if the device hardware supports Bluetooth Classic.
     *
     * @param context context
     * @return true if the {@link PackageManager#FEATURE_BLUETOOTH} system feature is present
     */
    public static boolean isBluetoothSupported(@NonNull Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    /**
     * Returns true if the device hardware supports Bluetooth Low Energy (BLE).
     *
     * @param context context
     * @return true if the {@link PackageManager#FEATURE_BLUETOOTH_LE} system feature is present
     */
    public static boolean isBluetoothLowEnergySupported(@NonNull Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Returns the system {@link BluetoothManager} service, or null if Bluetooth is not supported
     * on this device.
     *
     * @param context context used to retrieve the system service; must not be null
     * @return the {@link BluetoothManager}, or null if Bluetooth is not supported
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

    /**
     * Returns true if the given Bluetooth device is classified as an audio output device
     * (headset, handsfree, loudspeaker, headphones, car audio, or hi-fi audio) with the
     * {@link android.bluetooth.BluetoothClass.Service#RENDER} service bit set.
     *
     * @param bluetoothDevice the device to inspect; returns false if null
     * @return true if the device is an audio output
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static boolean isAudioOutput(BluetoothDevice bluetoothDevice)
    {
        if (bluetoothDevice == null)
        {
            return false;
        }

        BluetoothClass deviceBluetoothClass = bluetoothDevice.getBluetoothClass();
        boolean hasServiceRender = deviceBluetoothClass.hasService(Service.RENDER);
        int deviceClass = deviceBluetoothClass.getDeviceClass();

        if (hasServiceRender)
        {
            switch (deviceClass)
            {
                case Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                case Device.AUDIO_VIDEO_HANDSFREE:
                case Device.AUDIO_VIDEO_LOUDSPEAKER:
                case Device.AUDIO_VIDEO_HEADPHONES:
                case Device.AUDIO_VIDEO_CAR_AUDIO:
                case Device.AUDIO_VIDEO_HIFI_AUDIO:
                    return true;
            }
        }

        return false;
    }

    /**
     * Extracts the remote device's MAC address from a {@link BluetoothGatt} and returns it
     * as a {@code long}.
     *
     * @param gatt the GATT connection whose device address is to be converted
     * @return the device MAC address as a {@code long}
     */
    public static long gattDeviceAddressToLong(@NonNull BluetoothGatt gatt)
    {
        return bluetoothDeviceAddressToLong(gatt.getDevice());
    }

    /**
     * Returns the MAC address of a {@link BluetoothDevice} as a {@code long}.
     *
     * @param device the device whose address is to be converted
     * @return the device MAC address as a {@code long}
     */
    public static long bluetoothDeviceAddressToLong(@NonNull BluetoothDevice device)
    {
        return macAddressStringToLong(device.getAddress());
    }

    /**
     * Returns the remote device address from a {@link BluetoothGatt} as a colon-separated
     * uppercase hex string (e.g. {@code "AA:BB:CC:DD:EE:FF"}).
     *
     * @param gatt the GATT connection; must not be null
     * @return the pretty-printed MAC address, never null
     */
    public static String gattDeviceAddressToPrettyString(@NonNull BluetoothGatt gatt)
    {
        return bluetoothDeviceAddressToPrettyString(gatt.getDevice());
    }

    /**
     * Returns the address of a {@link BluetoothDevice} as a colon-separated uppercase hex string
     * (e.g. {@code "AA:BB:CC:DD:EE:FF"}).
     *
     * @param device the Bluetooth device; must not be null
     * @return the pretty-printed MAC address, never null
     */
    public static String bluetoothDeviceAddressToPrettyString(@NonNull BluetoothDevice device)
    {
        return macAddressStringToPrettyString(device.getAddress());
    }

    /**
     * Returns the last four hexadecimal characters of a device address as an uppercase string
     * suitable for short display labels (e.g. {@code "FF"} for {@code "AA:BB:CC:DD:EE:FF"}).
     * Returns {@code "null"} if the address is null or empty.
     *
     * @param deviceAddress a colon-separated MAC address string; may be null
     * @return the short address string, never null
     */
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

    /**
     * Returns the last four hexadecimal characters of a {@code long} device address as an
     * uppercase string.
     *
     * @param deviceAddress the MAC address as a {@code long}
     * @return the short address string, never null
     */
    public static String getShortDeviceAddressString(long deviceAddress)
    {
        return getShortDeviceAddressString(macAddressLongToString(deviceAddress));
    }

    /**
     * Strips colons from a MAC address and converts it to a lowercase hex string
     * (e.g. {@code "AA:BB:CC:DD:EE:FF"} → {@code "aabbccddeeff"}).
     *
     * @param macAddress the colon-separated MAC address; must not be null
     * @return the stripped lowercase string, never null
     */
    public static String macAddressStringToStrippedLowerCaseString(@NonNull String macAddress)
    {
        return macAddress.replace(":", "").toLowerCase();
    }

    /**
     * Converts a colon-separated MAC address string to a {@code long}.
     *
     * @param macAddress the colon-separated or bare hex MAC address string; must not be null
     * @return the address as a {@code long}
     * @throws NumberFormatException if the string cannot be parsed as a hex number
     */
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

    /**
     * Normalises a MAC address string to a colon-separated uppercase hex string
     * (e.g. {@code "aabbccddeeff"} → {@code "AA:BB:CC:DD:EE:FF"}).
     *
     * @param macAddress the MAC address in any common format; must not be null
     * @return the pretty-printed address string, never null
     */
    public static String macAddressStringToPrettyString(@NonNull String macAddress)
    {
        return macAddressLongToPrettyString(macAddressStringToLong(macAddress));
    }

    /**
     * Formats a {@code long} MAC address as a colon-separated uppercase hex string
     * (e.g. {@code "AA:BB:CC:DD:EE:FF"}).
     *
     * @param macAddress the MAC address as a {@code long}
     * @return the formatted address string, never null
     */
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

    /**
     * Formats a {@code long} MAC address as a zero-padded 12-character lowercase hex string
     * without colons (e.g. {@code "aabbccddeeff"}).
     *
     * @param macAddressLong the MAC address as a {@code long}
     * @return a 12-character lowercase hex string, never null
     */
    public static String macAddressLongToString(long macAddressLong)
    {
        return String.format(Locale.US, "%012x", macAddressLong);
    }

    /**
     * Returns a human-readable string for a Bluetooth adapter state constant.
     *
     * @param bluetoothAdapterState one of the {@code BluetoothAdapter.STATE_*} constants
     * @return a string such as {@code "STATE_ON(12)"}
     */
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

    /**
     * Returns a human-readable string for a Bluetooth profile connection state constant.
     *
     * @param bluetoothConnectionState one of the {@link android.bluetooth.BluetoothProfile} {@code STATE_*} constants
     * @return a string such as {@code "STATE_CONNECTED(2)"}
     */
    public static String bluetoothConnectionStateToString(int bluetoothConnectionState)
    {
        String name;
        switch (bluetoothConnectionState)
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
        return name + '(' + bluetoothConnectionState + ')';
    }

    /**
     * Returns a human-readable string for a Bluetooth headset audio state constant.
     *
     * @param bluetoothAudioState one of the {@link android.bluetooth.BluetoothHeadset} {@code STATE_AUDIO_*} constants
     * @return a string such as {@code "STATE_AUDIO_CONNECTED(12)"}
     */
    public static String bluetoothAudioStateToString(int bluetoothAudioState)
    {
        String name;
        switch (bluetoothAudioState)
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
        return name + '(' + bluetoothAudioState + ')';
    }

    /**
     * Returns a human-readable string for a BLE {@link android.bluetooth.le.ScanCallback}
     * error code.
     *
     * @param value one of the {@code ScanCallback.SCAN_FAILED_*} constants
     * @return a string such as {@code "SCAN_FAILED_ALREADY_STARTED(1)"}
     */
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

    /**
     * Returns a formatted string describing a GATT service/characteristic pair and an associated
     * value, using the friendly name from {@link FooGattUuids} where available.
     *
     * @param service        the GATT service
     * @param characteristic the GATT characteristic within that service
     * @param value          the value to include in the string (may be null)
     * @return a quoted, back-slash-delimited description string
     */
    public static String toString(@NonNull BluetoothGattService service, @NonNull BluetoothGattCharacteristic characteristic, Object value)
    {
        return toString(service.getUuid(), characteristic.getUuid(), value);
    }

    /**
     * Returns a formatted string describing a GATT service/characteristic pair and an associated
     * value, using the friendly name from {@link FooGattUuids} where available.
     *
     * @param uuidService        the UUID of the GATT service
     * @param uuidCharacteristic the UUID of the GATT characteristic within that service
     * @param value              the value to include in the string (may be null)
     * @return a quoted, back-slash-delimited description string
     */
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

    /**
     * Returns a human-readable description of a GATT service, using the friendly name from
     * {@link FooGattUuids} where available.
     *
     * @param service the GATT service to describe; returns {@code "null"} if null
     * @return a description string, never null
     */
    public static String getDescription(BluetoothGattService service)
    {
        if (service == null)
        {
            return "null";
        }
        return getDescription(service.getUuid());
    }

    /**
     * Returns a human-readable description of a GATT characteristic, using the friendly name
     * from {@link FooGattUuids} where available.
     *
     * @param characteristic the GATT characteristic to describe; returns {@code "null"} if null
     * @return a description string, never null
     */
    public static String getDescription(BluetoothGattCharacteristic characteristic)
    {
        if (characteristic == null)
        {
            return "null";
        }
        return getDescription(characteristic.getUuid());
    }

    /**
     * Returns a human-readable description for a Bluetooth UUID, using the registered
     * name from {@link FooGattUuids} when available, or the UUID string otherwise.
     *
     * @param uuid the UUID to describe
     * @return a description string, never null
     */
    public static String getDescription(UUID uuid)
    {
        FooGattUuid gattUuid = FooGattUuids.get(uuid);
        return (gattUuid != null) ? gattUuid.toString() : uuid.toString();
    }
}
