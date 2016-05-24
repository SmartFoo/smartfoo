package com.smartfoo.android.core.network;

import android.content.Context;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.network.FooCellularStateListener.FooCellularHookStateCallbacks;
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionCallbacks;
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionInfo;

/**
 * Consolidates FooDataConnectionListener and FooPhoneStateListener in to a single instance that is
 * used to ensure that a connection is not allowed if a phone call is active or there is no data connection.
 */
public class FooDataConnectionManager
        implements //
        FooDataConnectionCallbacks, FooCellularHookStateCallbacks
{
    private static final String TAG = FooLog.TAG(FooDataConnectionManager.class);

    public enum ConnectionState
    {
        OK,
        //
        PhoneOffHook,
        //
        NetworkDisconnected, //
    }

    private final Context mContext;

    //private final FooPowerLock              mPowerLock;
    //private final FooWifiLock               mWifiLock;
    private final FooCellularStateListener  mCellularStateListener;
    private final FooDataConnectionListener mDataConnectionStateListener;

    private FooDataConnectionCallbacks mCallbacks;

    private boolean mIsCellularOnHook;

    public FooDataConnectionManager(Context context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context cannot be null");
        }

        mContext = context;

        //mPowerLock = new FooPowerLock(mContext);
        //mWifiLock = new FooWifiLock(mContext);

        mCellularStateListener = new FooCellularStateListener(mContext);
        mIsCellularOnHook = mCellularStateListener.isOnHook();
        FooLog.v(TAG, "FooDeviceConnectionListener: mIsCellularOnHook=" + mIsCellularOnHook);

        mDataConnectionStateListener = new FooDataConnectionListener(mContext);
    }

    @Override
    public String toString()
    {
        return "{mIsCellularOnHook=" + mIsCellularOnHook //
               + ", mDataConnectionStateListener=" + mDataConnectionStateListener//
               + "}";
    }

    public FooDataConnectionInfo start(FooDataConnectionCallbacks callbacks)
    {
        FooLog.v(TAG, "+start(...)");

        mCallbacks = callbacks;

        //mPowerLock.lock();
        //mWifiLock.lock();

        if (!mDataConnectionStateListener.isStarted())
        {
            mDataConnectionStateListener.start(this);
        }
        if (!mCellularStateListener.isStarted())
        {
            mCellularStateListener.start(this, mDataConnectionStateListener);
        }

        FooLog.v(TAG, "-start(...)");

        return getDataConnectionInfo();
    }

    /**
     * Stop wifi/power locks and optionally connection listeners
     *
     * @param hard if true, then the connection listeners will be stopped
     */
    public void stop(boolean hard)
    {
        FooLog.v(TAG, "+stop(hard=" + hard + ")");

        if (hard)
        {
            mCallbacks = null;

            mCellularStateListener.stop();
            mDataConnectionStateListener.stop();
        }

        //mWifiLock.unlock();
        //mPowerLock.unlock();

        FooLog.v(TAG, "-stop(hard=" + hard + ")");
    }

    /**
     * @return the live state of the Cellular or Network data connection
     */
    public ConnectionState getConnectionState()
    {
        if (!mIsCellularOnHook)
        {
            return ConnectionState.PhoneOffHook;
        }
        else
        {
            if (getDataConnectionInfo().isConnected())
            {
                return ConnectionState.OK;
            }
            else
            {
                return ConnectionState.NetworkDisconnected;
            }
        }
    }

    /**
     * @return never null
     */
    public FooDataConnectionInfo getDataConnectionInfo()
    {
        return mDataConnectionStateListener.getDataConnectionInfo();
    }

    @Override
    public void onCellularOnHook()
    {
        FooLog.d(TAG, "onCellularOnHook()");
        if (!mIsCellularOnHook)
        {
            mIsCellularOnHook = true;

            // TODO: (Tony) DE760, CDMA networks don't get a data disconnected/connected event when a cell call is established so we must force it
            // Force connected state
            FooDataConnectionInfo connectionInfo = getDataConnectionInfo();
            connectionInfo.setConnected(true);

            onDataConnected(connectionInfo);
        }
    }

    @Override
    public void onCellularOffHook()
    {
        FooLog.d(TAG, "onCellularOffHook()");
        if (mIsCellularOnHook)
        {
            mIsCellularOnHook = false;

            // TODO:(pv) CDMA networks don't get a data disconnected/connected event when a cell call is established so we must force it
            // Force disconnected state
            FooDataConnectionInfo connectionInfo = getDataConnectionInfo();
            connectionInfo.setConnected(false);

            onDataDisconnected(connectionInfo);
        }
    }

    @Override
    public void onDataConnected(FooDataConnectionInfo dataConnectionInfo)
    {
        FooLog.d(TAG, "onDataConnected(" + dataConnectionInfo + ")");

        FooDataConnectionCallbacks callbacks = mCallbacks;
        if (callbacks != null)
        {
            if (mIsCellularOnHook)
            {
                callbacks.onDataConnected(dataConnectionInfo);
            }
            else
            {
                // If cell is off hook then mock disconnect
                callbacks.onDataDisconnected(dataConnectionInfo);
            }
        }
    }

    @Override
    public void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo)
    {
        FooLog.d(TAG, "onDataDisconnected(" + dataConnectionInfo + ")");

        FooDataConnectionCallbacks callbacks = mCallbacks;
        if (callbacks != null)
        {
            callbacks.onDataDisconnected(dataConnectionInfo);
        }
    }
}