package com.smartfoo.android.core.bluetooth.gatt;

import android.Manifest;
import android.content.Context;
import android.os.Looper;

import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.collections.FooLongSparseArray;
import com.smartfoo.android.core.logging.FooLog;

import java.util.Iterator;

public class FooGattManager
{
    private static final String TAG = FooLog.TAG(FooGattManager.class);

    private final Context                            mContext;
    private final Looper                             mLooper;
    private final FooLongSparseArray<FooGattHandler> mGattHandlers;

    public FooGattManager(Context context)
    {
        this(context, null);
    }

    public FooGattManager(Context context, Looper looper)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        if (looper == null)
        {
            looper = Looper.getMainLooper();
        }

        mContext = context;
        mLooper = looper;

        mGattHandlers = new FooLongSparseArray<>();
    }

    public Context getContext()
    {
        return mContext;
    }

    //package
    Looper getLooper()
    {
        return mLooper;
    }

    /**
     * Allocates a GattHandler. To free the GattHandler, call {@link FooGattHandler#close()}
     *
     * @param deviceAddress deviceAddress
     * @return never null
     */
    public FooGattHandler getGattHandler(long deviceAddress)
    {
        FooGattUtils.throwExceptionIfInvalidBluetoothAddress(deviceAddress);

        synchronized (mGattHandlers)
        {
            FooGattHandler gattHandler = mGattHandlers.get(deviceAddress);
            if (gattHandler == null)
            {
                gattHandler = new FooGattHandler(this, deviceAddress);
                mGattHandlers.put(deviceAddress, gattHandler);
            }

            return gattHandler;
        }
    }

    //package
    void removeGattHandler(FooGattHandler gattHandler)
    {
        if (gattHandler == null)
        {
            throw new IllegalArgumentException("gattHandler must not be null");
        }

        long deviceAddress = gattHandler.getDeviceAddressLong();
        mGattHandlers.remove(deviceAddress);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void close()
    {
        FooLog.v(TAG, "+close()");

        synchronized (mGattHandlers)
        {
            Iterator<FooGattHandler> it = mGattHandlers.iterateValues();
            while (it.hasNext())
            {
                FooGattHandler gattHandler = it.next();
                it.remove();

                gattHandler.close(false);
            }
        }

        FooLog.v(TAG, "-close()");
    }
}
