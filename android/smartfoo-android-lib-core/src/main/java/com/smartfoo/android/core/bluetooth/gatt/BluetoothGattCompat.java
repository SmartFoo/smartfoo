package com.smartfoo.android.core.bluetooth.gatt;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.logging.FooLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Original source:
 * https://github.com/Polidea/RxAndroidBle/blob/master/rxandroidble/src/main/java/com/polidea/rxandroidble2/internal/util/BleConnectionCompat.java
 * Apache License 2.0 (with no copyright header at the time this code was copied).
 * https://www.apache.org/licenses/LICENSE-2.0.html
 */
public class BluetoothGattCompat
{
    private static final String TAG = FooLog.TAG(BluetoothGattCompat.class);

    private final Context context;

    @SuppressWarnings("WeakerAccess")
    public BluetoothGattCompat(Context context)
    {
        this.context = context;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothGatt connectGatt(BluetoothDevice remoteDevice,
                                     boolean autoConnect,
                                     BluetoothGattCallback bluetoothGattCallback)
    {
        if (remoteDevice == null)
        {
            return null;
        }

        /*
         * Issue that caused a race condition mentioned below was fixed in 7.0.0_r1
         * https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/android/bluetooth/BluetoothGatt.java#649
         * compared to
         * https://android.googlesource.com/platform/frameworks/base/+/android-6.0.1_r72/core/java/android/bluetooth/BluetoothGatt.java#739
         * issue: https://android.googlesource.com/platform/frameworks/base/+/d35167adcaa40cb54df8e392379dfdfe98bcdba2%5E%21/#F0
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || !autoConnect)
        {
            return connectGattCompat(bluetoothGattCallback, remoteDevice, autoConnect);
        }

        /*
         * Some implementations of Bluetooth Stack have a race condition where autoConnect flag
         * is not properly set *BEFORE* calling connectGatt. That's the reason for using reflection
         * to set the flag manually.
         */
        try
        {
            FooLog.v(TAG, "Trying to connectGatt using reflection.");
            Object iBluetoothGatt = getIBluetoothGatt(getIBluetoothManager());
            if (iBluetoothGatt == null)
            {
                FooLog.w(TAG, "Couldn't get iBluetoothGatt object");
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
            }
            BluetoothGatt bluetoothGatt = createBluetoothGatt(iBluetoothGatt, remoteDevice);
            boolean connectedSuccessfully = connectUsingReflection(bluetoothGatt, bluetoothGattCallback, true);
            if (!connectedSuccessfully)
            {
                FooLog.w(TAG, "Connection using reflection failed, closing gatt");
                bluetoothGatt.close();
            }
            return bluetoothGatt;
        }
        catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | InstantiationException
                | NoSuchFieldException exception)
        {
            FooLog.w(TAG, "Error during reflection", exception);
            return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private BluetoothGatt connectGattCompat(BluetoothGattCallback bluetoothGattCallback,
                                            BluetoothDevice device,
                                            boolean autoConnect)
    {
        FooLog.v(TAG, "Connecting without reflection");
        if (Build.VERSION.SDK_INT >= 23)
        {
            return device.connectGatt(context, autoConnect, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
        else
        {
            return device.connectGatt(context, autoConnect, bluetoothGattCallback);
        }
    }

    private boolean connectUsingReflection(BluetoothGatt bluetoothGatt,
                                           BluetoothGattCallback bluetoothGattCallback,
                                           @SuppressWarnings("SameParameterValue") boolean autoConnect)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException
    {
        FooLog.v(TAG, "Connecting using reflection");
        setAutoConnectValue(bluetoothGatt, autoConnect);
        @SuppressWarnings("JavaReflectionMemberAccess")
        Method connectMethod = bluetoothGatt.getClass()
                .getDeclaredMethod("connect", Boolean.class, BluetoothGattCallback.class);
        connectMethod.setAccessible(true);
        return (Boolean) (connectMethod.invoke(bluetoothGatt, true, bluetoothGattCallback));
    }

    @NonNull
    @TargetApi(Build.VERSION_CODES.M)
    private BluetoothGatt createBluetoothGatt(Object iBluetoothGatt,
                                              BluetoothDevice remoteDevice)
            throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Constructor bluetoothGattConstructor = BluetoothGatt.class
                .getDeclaredConstructors()[0];
        bluetoothGattConstructor.setAccessible(true);
        FooLog.v(TAG, "Found constructor with args count = " + bluetoothGattConstructor.getParameterTypes().length);
        if (bluetoothGattConstructor.getParameterTypes().length == 4)
        {
            return (BluetoothGatt) (bluetoothGattConstructor.newInstance(context, iBluetoothGatt, remoteDevice, BluetoothDevice.TRANSPORT_LE));
        }
        else
        {
            return (BluetoothGatt) (bluetoothGattConstructor.newInstance(context, iBluetoothGatt, remoteDevice));
        }
    }

    private Object getIBluetoothGatt(Object iBluetoothManager)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        if (iBluetoothManager == null)
        {
            return null;
        }
        Method getBluetoothGattMethod = getMethodFromClass(iBluetoothManager.getClass(), "getBluetoothGatt");
        return getBluetoothGattMethod.invoke(iBluetoothManager);
    }

    private Object getIBluetoothManager()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
        {
            return null;
        }
        Method getBluetoothManagerMethod = getMethodFromClass(bluetoothAdapter.getClass(), "getBluetoothManager");
        return getBluetoothManagerMethod.invoke(bluetoothAdapter);
    }

    private Method getMethodFromClass(Class<?> cls,
                                      String methodName)
            throws NoSuchMethodException
    {
        Method method = cls.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
    }

    private void setAutoConnectValue(BluetoothGatt bluetoothGatt,
                                     boolean autoConnect)
            throws NoSuchFieldException, IllegalAccessException
    {
        @SuppressWarnings("JavaReflectionMemberAccess")
        Field autoConnectField = bluetoothGatt.getClass().getDeclaredField("mAutoConnect");
        autoConnectField.setAccessible(true);
        autoConnectField.setBoolean(bluetoothGatt, autoConnect);
    }
}
