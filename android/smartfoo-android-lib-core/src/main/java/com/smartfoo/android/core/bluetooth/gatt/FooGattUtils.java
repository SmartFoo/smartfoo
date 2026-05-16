package com.smartfoo.android.core.bluetooth.gatt;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

import java.util.Locale;
import java.util.UUID;

/**
 * Static utility methods for GATT-level Bluetooth LE operations.
 *
 * <p>Includes device-address conversion between string and {@code long} forms, safe wrappers
 * around {@link android.bluetooth.BluetoothGatt#disconnect()} and
 * {@link android.bluetooth.BluetoothGatt#close()} that handle known exceptions, connection
 * state name formatting, and byte-array serialisation helpers that mirror the hidden
 * {@link android.bluetooth.BluetoothGattCharacteristic} encoding methods.
 * This class is not instantiable.</p>
 */
public class FooGattUtils
{
    private static final String TAG = FooLog.TAG(FooGattUtils.class);

    private FooGattUtils()
    {
    }

    /**
     * Extracts the remote device address string from a {@link android.bluetooth.BluetoothGatt}.
     * Returns {@code "00:00:00:00:00:00"} if {@code gatt} is null.
     *
     * @param gatt the GATT connection, or null
     * @return the device MAC address string, never null
     * @throws IllegalStateException if the device or its address string is unexpectedly null
     */
    @NonNull
    public static String gattDeviceAddressString(BluetoothGatt gatt)
    {
        if (gatt == null)
        {
            return "00:00:00:00:00:00";
        }

        BluetoothDevice bluetoothDevice = gatt.getDevice();
        if (bluetoothDevice == null)
        {
            throw new IllegalStateException("unexpected bluetoothDevice == null");
        }

        String bluetoothDeviceAddressString = bluetoothDevice.getAddress();
        if (FooString.isNullOrEmpty(bluetoothDeviceAddressString))
        {
            throw new IllegalStateException("unexpected bluetoothDevice.getAddress() == null/\"\"");
        }

        return bluetoothDeviceAddressString;
    }

    /**
     * Returns the remote device address of a {@link android.bluetooth.BluetoothGatt} as a {@code long}.
     *
     * @param gatt the GATT connection
     * @return the device MAC address as a {@code long}
     */
    public static long gattDeviceAddressLong(BluetoothGatt gatt)
    {
        String bluetoothDeviceAddressString = gattDeviceAddressString(gatt);
        return deviceAddressStringToLong(bluetoothDeviceAddressString);
    }

    /**
     * Converts a colon-separated MAC address string (e.g. {@code "AA:BB:CC:DD:EE:FF"}) to a
     * {@code long}.
     *
     * @param deviceAddressString the MAC address string; must not be null or empty
     * @return the address as a {@code long}
     * @throws IllegalArgumentException if the string is null or empty
     */
    public static long deviceAddressStringToLong(String deviceAddressString)
    {
        if (FooString.isNullOrEmpty(deviceAddressString))
        {
            throw new IllegalArgumentException("unexpected deviceAddressString == null/\"\"");
        }
        deviceAddressString = deviceAddressString.replace(":", "");
        return Long.parseLong(deviceAddressString, 16);
    }

    /**
     * Formats a {@code long} MAC address as a colon-separated uppercase hex string
     * (e.g. {@code "AA:BB:CC:DD:EE:FF"}).
     *
     * @param deviceAddressLong the MAC address as a {@code long}
     * @return the formatted address string, never null
     */
    @NonNull
    public static String deviceAddressLongToString(long deviceAddressLong)
    {
        //noinspection PointlessBitwiseExpression
        return String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X",
                (byte) ((deviceAddressLong >> 40) & 0xff),
                (byte) ((deviceAddressLong >> 32) & 0xff),
                (byte) ((deviceAddressLong >> 24) & 0xff),
                (byte) ((deviceAddressLong >> 16) & 0xff),
                (byte) ((deviceAddressLong >> 8) & 0xff),
                (byte) ((deviceAddressLong >> 0) & 0xff));
    }

    /**
     * Returns a human-readable string for a Bluetooth profile connection state.
     *
     * @param state one of the {@link android.bluetooth.BluetoothProfile} {@code STATE_*} constants
     * @return a string such as {@code "STATE_CONNECTED(2)"}
     */
    public static String bluetoothProfileStateToString(int state)
    {
        String text;
        switch (state)
        {
            case BluetoothProfile.STATE_DISCONNECTED:
                text = "STATE_DISCONNECTED";
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                text = "STATE_DISCONNECTING";
                break;
            case BluetoothProfile.STATE_CONNECTING:
                text = "STATE_CONNECTING";
                break;
            case BluetoothProfile.STATE_CONNECTED:
                text = "STATE_CONNECTED";
                break;
            default:
                text = "STATE_UNKNOWN";
                break;
        }
        return text + '(' + state + ')';
    }

    /**
     * Throws an {@link IllegalArgumentException} if {@code deviceAddress} is {@code 0} or
     * {@code -1}, which indicate an unset or invalid Bluetooth address.
     *
     * @param deviceAddress the address to validate
     * @throws IllegalArgumentException if the address is 0 or -1
     */
    public static void throwExceptionIfInvalidBluetoothAddress(long deviceAddress)
    {
        if (!(deviceAddress != 0 && deviceAddress != -1))
        {
            throw new IllegalArgumentException("deviceAddress invalid");
        }
    }

    /**
     * Calls {@link android.bluetooth.BluetoothGatt#disconnect()} followed immediately by
     * {@link android.bluetooth.BluetoothGatt#close()}, suppressing any exception thrown by
     * {@code disconnect()} so that {@code close()} is always attempted.
     *
     * <p><b>Note:</b> Per <a href="https://code.google.com/p/android/issues/detail?id=183108">
     * Android bug #183108</a>, calling {@code close()} immediately after {@code disconnect()} can
     * trigger a status-257 error on Android 5.x. Use this overload only when the intent is to
     * fully destroy the GATT client.</p>
     *
     * @param callerName a label used in log messages to identify the caller
     * @param gatt       the GATT connection to disconnect and close; a null value is logged and
     *                   returns false
     * @return true if both {@code disconnect()} and {@code close()} completed without throwing;
     *         false otherwise
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static boolean safeDisconnectAndClose(@NonNull final String callerName, final BluetoothGatt gatt)
    {
        return safeDisconnect(callerName, gatt, true) && safeClose(callerName, gatt);
    }

    /**
     * Calls {@link android.bluetooth.BluetoothGatt#disconnect()} and returns whether the call
     * succeeded without throwing an exception.
     *
     * <p><b>Note:</b> Per <a href="https://code.google.com/p/android/issues/detail?id=183108">
     * Android bug #183108</a>, callers should not call
     * {@link android.bluetooth.BluetoothGatt#close()} immediately after this method on Android
     * 5.x; wait for the {@code onConnectionStateChange} callback first.</p>
     *
     * @param callerName a label used in log messages to identify the caller
     * @param gatt       the GATT connection to disconnect; a null value is logged and returns false
     * @return true if {@code disconnect()} completed without throwing; false otherwise
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static boolean safeDisconnect(@NonNull final String callerName, final BluetoothGatt gatt)
    {
        return safeDisconnect(callerName, gatt, false);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private static boolean safeDisconnect(@NonNull final String callerName, final BluetoothGatt gatt, boolean ignoreException)
    {
        String debugInfo = gattDeviceAddressString(gatt) + FooString.quote(callerName) + "->safeDisconnect";
        FooLog.v(TAG, debugInfo + "(gatt=" + gatt + ')');

        if (gatt == null)
        {
            FooLog.w(TAG, debugInfo + ": gatt == null; ignoring");
            return false;
        }

        try
        {
            FooLog.v(TAG, debugInfo + ": gatt.disconnect()");
            gatt.disconnect();
        }
        catch (Exception e)
        {
            FooLog.w(TAG, debugInfo + ": gatt.disconnect() EXCEPTION; ignoring", e);
            if (!ignoreException)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Calls {@link android.bluetooth.BluetoothGatt#close()} and returns whether the call
     * succeeded without throwing an exception. All exceptions are caught and logged.
     *
     * @param callerName a label used in log messages to identify the caller
     * @param gatt       the GATT connection to close; a null value is logged and returns false
     * @return true if {@code close()} completed without throwing; false otherwise
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static boolean safeClose(@NonNull final String callerName, final BluetoothGatt gatt)
    {
        String debugInfo = gattDeviceAddressString(gatt) + ' ' + callerName + "->safeClose";
        FooLog.v(TAG, debugInfo + "(gatt=" + gatt + ')');

        if (gatt == null)
        {
            FooLog.w(TAG, debugInfo + ": gatt == null; ignoring");
            return false;
        }

        try
        {
            FooLog.v(TAG, debugInfo + ": gatt.close()");
            gatt.close();
        }
        catch (Exception e)
        {
            FooLog.w(TAG, debugInfo + ": gatt.close() EXCEPTION; ignoring", e);
            return false;
        }

        return true;
    }

    /**
     * Creates a {@link android.bluetooth.BluetoothGattCharacteristic} attached to a new
     * primary {@link android.bluetooth.BluetoothGattService} with the given UUIDs.
     * Useful for constructing test/mock objects.
     *
     * @param serviceUuid        UUID of the enclosing service
     * @param characteristicUuid UUID of the characteristic
     * @return a newly created characteristic already added to its service
     */
    public static BluetoothGattCharacteristic createBluetoothGattCharacteristic(UUID serviceUuid, UUID characteristicUuid)
    {
        BluetoothGattService service = new BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(characteristicUuid, 0, 0);
        service.addCharacteristic(characteristic);
        return characteristic;
    }

    //
    //
    //

    /**
     * Code taken from {@link BluetoothGattCharacteristic#setValue(int, int, int)}
     *
     * @param value      New value for this characteristic
     * @param formatType Integer format type used to transform the value parameter
     * @param offset     Offset at which the value should be placed
     * @return never null
     */
    public static byte[] toBytes(int value, int formatType, int offset)
    {
        byte[] bytes = new byte[offset + getTypeLen(formatType)];

        switch (formatType)
        {
            case BluetoothGattCharacteristic.FORMAT_SINT8:
                value = intToSignedBits(value, 8);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT8:
                bytes[offset] = (byte) (value & 0xFF);
                break;

            case BluetoothGattCharacteristic.FORMAT_SINT16:
                value = intToSignedBits(value, 16);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT16:
                bytes[offset++] = (byte) (value & 0xFF);
                bytes[offset] = (byte) ((value >> 8) & 0xFF);
                break;

            case BluetoothGattCharacteristic.FORMAT_SINT32:
                value = intToSignedBits(value, 32);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT32:
                bytes[offset++] = (byte) (value & 0xFF);
                bytes[offset++] = (byte) ((value >> 8) & 0xFF);
                bytes[offset++] = (byte) ((value >> 16) & 0xFF);
                bytes[offset] = (byte) ((value >> 24) & 0xFF);
                break;

            default:
                throw new NumberFormatException("Unknown formatType " + formatType);
        }
        return bytes;
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#setValue(int, int, int, int)}
     *
     * @param mantissa   Mantissa for this characteristic
     * @param exponent   exponent value for this characteristic
     * @param formatType Float format type used to transform the value parameter
     * @param offset     Offset at which the value should be placed
     * @return never null
     */
    public static byte[] toBytes(int mantissa, int exponent, int formatType, int offset)
    {
        byte[] bytes = new byte[offset + getTypeLen(formatType)];

        switch (formatType)
        {
            case BluetoothGattCharacteristic.FORMAT_SFLOAT:
                mantissa = intToSignedBits(mantissa, 12);
                exponent = intToSignedBits(exponent, 4);
                bytes[offset++] = (byte) (mantissa & 0xFF);
                bytes[offset] = (byte) ((mantissa >> 8) & 0x0F);
                bytes[offset] += (byte) ((exponent & 0x0F) << 4);
                break;

            case BluetoothGattCharacteristic.FORMAT_FLOAT:
                mantissa = intToSignedBits(mantissa, 24);
                exponent = intToSignedBits(exponent, 8);
                bytes[offset++] = (byte) (mantissa & 0xFF);
                bytes[offset++] = (byte) ((mantissa >> 8) & 0xFF);
                bytes[offset++] = (byte) ((mantissa >> 16) & 0xFF);
                bytes[offset] += (byte) (exponent & 0xFF);
                break;

            default:
                throw new NumberFormatException("Unknown formatType " + formatType);
        }

        return bytes;
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#setValue(String)}
     *
     * @param value New value for this characteristic
     * @return true if the locally stored value has been set
     */
    public static byte[] toBytes(String value)
    {
        return (value != null) ? value.getBytes() : new byte[] {};
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#getTypeLen(int)}
     * Returns the size of a give value type.
     */
    @SuppressWarnings("JavadocReference")
    private static int getTypeLen(int formatType)
    {
        return formatType & 0xF;
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#intToSignedBits(int, int)}
     * Convert an integer into the signed bits of a given length.
     */
    @SuppressWarnings("JavadocReference")
    private static int intToSignedBits(int i, int size)
    {
        if (i < 0)
        {
            i = (1 << size - 1) + (i & ((1 << size - 1) - 1));
        }
        return i;
    }
}
