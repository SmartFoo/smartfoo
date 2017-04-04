package com.smartfoo.android.core.network;

import android.content.Context;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
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

    private final FooListenerManager<FooDataConnectionCallbacks> mListenerManager;
    //private final FooPowerLock                                   mPowerLock;
    //private final FooWifiLock                                    mWifiLock;
    private final FooCellularStateListener                       mCellularStateListener;
    private final FooDataConnectionListener                      mDataConnectionStateListener;

    private boolean mIsStarted;
    private boolean mIsCellularOnHook;

    public FooDataConnectionManager(@NonNull Context context)
    {
        mContext = FooRun.toNonNull(context, "context");

        mListenerManager = new FooListenerManager<>();

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
        return "{ mIsCellularOnHook=" + mIsCellularOnHook
               + ", mDataConnectionStateListener=" + mDataConnectionStateListener
               + " }";
    }

    public boolean isStarted()
    {
        return mIsStarted;
    }

    @NonNull
    public FooDataConnectionInfo attach(FooDataConnectionCallbacks callbacks)
    {
        FooLog.v(TAG, "+attach(...)");

        mListenerManager.attach(callbacks);
        if (mListenerManager.size() == 1 && !mIsStarted)
        {
            mIsStarted = true;

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
        }

        FooLog.v(TAG, "-attach(...)");

        return getDataConnectionInfo();
    }

    public void detach(FooDataConnectionCallbacks callbacks)
    {
        FooLog.v(TAG, "+detach(...)");

        mListenerManager.detach(callbacks);
        if (mListenerManager.size() == 0 && mIsStarted)
        {
            mIsStarted = false;

            //mWifiLock.unlock();
            //mPowerLock.unlock();

            mCellularStateListener.stop();
            mDataConnectionStateListener.stop();
        }

        FooLog.v(TAG, "-detach(...)");
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

    @NonNull
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

            // TODO: (pv) CDMA networks don't get a data disconnected/connected event when a cell call is established so we must force it
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
        for (FooDataConnectionCallbacks callbacks : mListenerManager.beginTraversing())
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
        mListenerManager.endTraversing();
    }

    @Override
    public void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo)
    {
        FooLog.d(TAG, "onDataDisconnected(" + dataConnectionInfo + ")");
        for (FooDataConnectionCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onDataDisconnected(dataConnectionInfo);
        }
        mListenerManager.endTraversing();
    }
}