package com.smartfoo.android.core.platform;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener.OnBluetoothHeadsetConnectionCallbacks;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener.OnWiredHeadsetConnectionCallbacks;

import java.util.Map;

public class FooHeadsetConnectionListener
{
    public interface OnHeadsetConnectionCallbacks
            extends OnWiredHeadsetConnectionCallbacks,
            OnBluetoothHeadsetConnectionCallbacks
    {
    }

    private final FooWiredHeadsetConnectionListener     mWiredHeadsetConnectionListener;
    private final FooBluetoothHeadsetConnectionListener mBluetoothHeadsetConnectionListener;

    public FooHeadsetConnectionListener(@NonNull Context applicationContext)
    {
        mWiredHeadsetConnectionListener = new FooWiredHeadsetConnectionListener(applicationContext);
        mBluetoothHeadsetConnectionListener = new FooBluetoothHeadsetConnectionListener(applicationContext);
    }

    public boolean isHeadsetConnected()
    {
        return isWiredHeadsetConnected() || isBluetoothHeadsetConnected();
    }

    public boolean isWiredHeadsetConnected()
    {
        return mWiredHeadsetConnectionListener.isWiredHeadsetConnected();
    }

    public boolean isBluetoothHeadsetConnected()
    {
        return mBluetoothHeadsetConnectionListener.isBluetoothHeadsetConnected();
    }

    @NonNull
    public Map<String, BluetoothDevice> getConnectedBluetoothHeadsets()
    {
        return mBluetoothHeadsetConnectionListener.getConnectedBluetoothHeadsets();
    }

    public boolean isStarted()
    {
        return mWiredHeadsetConnectionListener.isStarted();
    }

    public void attach(OnHeadsetConnectionCallbacks callbacks)
    {
        mWiredHeadsetConnectionListener.attach(callbacks);
        mBluetoothHeadsetConnectionListener.attach(callbacks);
    }

    public void detach(OnHeadsetConnectionCallbacks callbacks)
    {
        mWiredHeadsetConnectionListener.detach(callbacks);
        mBluetoothHeadsetConnectionListener.detach(callbacks);
    }
}
