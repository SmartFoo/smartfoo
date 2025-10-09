package com.smartfoo.android.core.network;

import android.Manifest;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.network.FooCellularStateListener.FooCellularHookStateCallbacks;
import com.smartfoo.android.core.network.FooCellularStateListener.HookState;
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionInfo;
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionListenerCallbacks;

/**
 * Consolidates FooDataConnectionListener and FooCellularStateListener in to a single instance that is
 * used to ensure that a connection is not allowed if a phone call is active or there is no data connection.
 */
public class FooDataConnectionManager
{
    private static final String TAG = FooLog.TAG(FooDataConnectionManager.class);

    public enum ConnectionState
    {
        OK,
        PhoneOffHook,
        NetworkDisconnected,
    }

    public interface FooDataConnectionManagerCallbacks
    {
        void onCellularOffHook();

        void onCellularOnHook();

        void onDataConnected(FooDataConnectionInfo dataConnectionInfo);

        void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo);
    }

    private final FooListenerAutoStartManager<FooDataConnectionManagerCallbacks> mListenerManager;
    //private final FooPowerLock                                          mPowerLock;
    //private final FooWifiLock                                           mWifiLock;
    private final FooCellularStateListener                                       mCellularStateListener;
    private final FooCellularHookStateCallbacks                                  mCellularHookStateCallbacks;
    private final FooDataConnectionListener                                      mDataConnectionListener;
    private final FooDataConnectionListenerCallbacks                             mDataConnectionListenerCallbacks;

    private boolean   mIsStarted;
    private HookState mLastCellularHookState;

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public FooDataConnectionManager(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");

        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            @Override
            public void onFirstAttach()
            {
                if (!mIsStarted)
                {
                    mIsStarted = true;

                    //mPowerLock.lock();
                    //mWifiLock.lock();

                    if (!mCellularStateListener.isStarted())
                    {
                        mLastCellularHookState = mCellularStateListener.getHookState();
                        mCellularStateListener.start(mCellularHookStateCallbacks, null);
                    }
                    if (!mDataConnectionListener.isStarted())
                    {
                        mDataConnectionListener.start(mDataConnectionListenerCallbacks);
                    }
                }
            }

            @Override
            public boolean onLastDetach()
            {
                if (mIsStarted)
                {
                    mIsStarted = false;

                    //mWifiLock.unlock();
                    //mPowerLock.unlock();

                    mCellularStateListener.stop();
                    mDataConnectionListener.stop();
                }
                return false;
            }
        });

        //mPowerLock = new FooPowerLock(mContext);
        //mWifiLock = new FooWifiLock(mContext);

        mCellularStateListener = new FooCellularStateListener(context);
        mCellularHookStateCallbacks = new FooCellularHookStateCallbacks()
        {
            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            @Override
            public void onCellularOffHook()
            {
                FooDataConnectionManager.this.onCellularOffHook();
            }

            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            @Override
            public void onCellularOnHook()
            {
                FooDataConnectionManager.this.onCellularOnHook();
            }
        };

        mDataConnectionListener = new FooDataConnectionListener(context);
        mDataConnectionListenerCallbacks = new FooDataConnectionListenerCallbacks()
        {
            @Override
            public void onDataConnected(FooDataConnectionInfo dataConnectionInfo)
            {
                FooDataConnectionManager.this.onDataConnected(dataConnectionInfo);
            }

            @Override
            public void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo)
            {
                FooDataConnectionManager.this.onDataDisconnected(dataConnectionInfo);
            }
        };
    }

    @Override
    @NonNull
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public String toString()
    {
        return "{ getConnectionState()=" + getConnectionState()
               + ", getDataConnectionInfo()=" + getDataConnectionInfo()
               + " }";
    }

    public boolean isStarted()
    {
        return mIsStarted;
    }

    public void attach(FooDataConnectionManagerCallbacks callbacks)
    {
        FooLog.v(TAG, "+attach(...)");
        mListenerManager.attach(callbacks);
        FooLog.v(TAG, "-attach(...)");
    }

    public void detach(FooDataConnectionManagerCallbacks callbacks)
    {
        FooLog.v(TAG, "+detach(...)");
        mListenerManager.detach(callbacks);
        FooLog.v(TAG, "-detach(...)");
    }

    /**
     * @return the live state of the data connection; either {@link ConnectionState#OK},
     * {@link ConnectionState#PhoneOffHook}, or {@link ConnectionState#NetworkDisconnected}
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @NonNull
    public ConnectionState getConnectionState()
    {
        if (mCellularStateListener.isOffHook())
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

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @NonNull
    public FooDataConnectionInfo getDataConnectionInfo()
    {
        return mDataConnectionListener.getDataConnectionInfo();
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private void onCellularOnHook()
    {
        FooLog.d(TAG, "onCellularOnHook()");
        if (mLastCellularHookState != HookState.OnHook)
        {
            mLastCellularHookState = HookState.OnHook;

            // CDMA networks don't get a data disconnected/connected event when a cell call is established.
            // Force connected state
            FooDataConnectionInfo connectionInfo = getDataConnectionInfo();
            connectionInfo.setConnected(true);

            onDataConnected(connectionInfo);
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private void onCellularOffHook()
    {
        FooLog.d(TAG, "onCellularOffHook()");
        if (mLastCellularHookState != HookState.OffHook)
        {
            mLastCellularHookState = HookState.OffHook;

            // CDMA networks don't get a data disconnected/connected event when a cell call is established.
            // Force disconnected state
            FooDataConnectionInfo connectionInfo = getDataConnectionInfo();
            connectionInfo.setConnected(false);

            onDataDisconnected(connectionInfo);
        }
    }

    private void onDataConnected(FooDataConnectionInfo dataConnectionInfo)
    {
        FooLog.d(TAG, "onDataConnected(" + dataConnectionInfo + ")");
        if (mCellularStateListener.isOnHook())
        {
            for (FooDataConnectionManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onDataConnected(dataConnectionInfo);
            }
            mListenerManager.endTraversing();
        }
        else
        {
            // If cell is off hook then mock disconnect
            onDataDisconnected(dataConnectionInfo);
        }
    }

    private void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo)
    {
        FooLog.d(TAG, "onDataDisconnected(" + dataConnectionInfo + ")");
        for (FooDataConnectionManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onDataDisconnected(dataConnectionInfo);
        }
        mListenerManager.endTraversing();
    }
}