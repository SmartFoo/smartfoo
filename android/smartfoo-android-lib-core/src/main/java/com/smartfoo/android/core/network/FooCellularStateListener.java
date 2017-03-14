package com.smartfoo.android.core.network;

import android.content.Context;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.smartfoo.android.core.logging.FooLog;

public class FooCellularStateListener
        extends PhoneStateListener
{
    private static final String TAG = FooLog.TAG(FooCellularStateListener.class);

    public interface FooCellularHookStateCallbacks
    {
        void onCellularOffHook();

        void onCellularOnHook();
    }

    public interface FooCellularDataStateCallbacks
    {
        /**
         * @param networkType See {@link TelephonyManager#getNetworkType()}<br>NOTE that these are different from
         *                    {@link
         *                    NetworkInfo#getType()}
         */
        void onCellularDataConnected(int networkType);

        /**
         * @param networkType See {@link TelephonyManager#getNetworkType()}<br>NOTE that these are different from
         *                    {@link
         *                    NetworkInfo#getType()}
         */
        void onCellularDataDisconnected(int networkType);
    }

    private final Object mSyncLock = new Object();

    private final TelephonyManager mTelephonyManager;

    private boolean mIsStarted;

    private FooCellularHookStateCallbacks mCallbacksHookState;
    private FooCellularDataStateCallbacks mCallbacksDataState;

    public FooCellularStateListener(Context context)
    {
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public boolean isStarted()
    {
        synchronized (mSyncLock)
        {
            return mIsStarted;
        }
    }

    public void start(FooCellularHookStateCallbacks callbacksHookState, FooCellularDataStateCallbacks callbacksDataState)
    {
        FooLog.i(TAG, "+start(...)");
        synchronized (mSyncLock)
        {
            mCallbacksHookState = callbacksHookState;
            mCallbacksDataState = callbacksDataState;

            if (!mIsStarted)
            {
                mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE //
                                               | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

                mIsStarted = true;
            }
        }
        FooLog.i(TAG, "-start(...)");
    }

    public void stop()
    {
        FooLog.i(TAG, "+stop()");
        synchronized (mSyncLock)
        {
            mCallbacksHookState = null;
            mCallbacksDataState = null;

            if (mIsStarted)
            {
                mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
                mIsStarted = false;
            }
        }
        FooLog.i(TAG, "-stop()");
    }

    public boolean isOnHook()
    {
        return mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber)
    {
        super.onCallStateChanged(state, incomingNumber);

        FooCellularHookStateCallbacks callbacks = mCallbacksHookState;
        if (callbacks != null)
        {
            switch (state)
            {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    callbacks.onCellularOffHook();
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    // ignore
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    callbacks.onCellularOnHook();
                    break;
                default:
                    FooLog.w(TAG, "onCallStateChanged: Unhandled call state=" + callStateToString(state));
                    break;
            }
        }
    }

    @Override
    public void onDataConnectionStateChanged(int dataConnectionState, int networkType)
    {
        super.onDataConnectionStateChanged(dataConnectionState, networkType);

        FooLog.w(TAG, "onDataConnectionStateChanged: dataConnectionState="
                      + dataConnectionStateToString(dataConnectionState));
        FooLog.w(TAG, "onDataConnectionStateChanged: networkType=" + networkTypeToString(networkType));

        FooCellularDataStateCallbacks callbacks = mCallbacksDataState;
        if (callbacks != null)
        {
            switch (dataConnectionState)
            {
                case TelephonyManager.DATA_DISCONNECTED:
                case TelephonyManager.DATA_SUSPENDED:
                    callbacks.onCellularDataDisconnected(networkType);
                    break;
                case TelephonyManager.DATA_CONNECTED:
                    callbacks.onCellularDataConnected(networkType);
                    break;
                case TelephonyManager.DATA_CONNECTING:
                    // ignore
                    break;
                default:
                    FooLog.w(TAG, "onDataConnectionStateChanged: Unhandled dataConnectionState="
                                  + dataConnectionStateToString(dataConnectionState));
                    break;
            }
        }
    }

    public static String callStateToString(int callState)
    {
        switch (callState)
        {
            case TelephonyManager.CALL_STATE_IDLE:
                return "CALL_STATE_IDLE(" + callState + ")";
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return "CALL_STATE_OFFHOOK(" + callState + ")";
            case TelephonyManager.CALL_STATE_RINGING:
                return "CALL_STATE_RINGING(" + callState + ")";
            default:
                return "CALL_STATE_UNKNOWN(" + callState + ")";
        }
    }

    public static String dataConnectionStateToString(int dataConnectionState)
    {
        switch (dataConnectionState)
        {
            case TelephonyManager.DATA_DISCONNECTED:
                return "DATA_DISCONNECTED(" + dataConnectionState + ")";
            case TelephonyManager.DATA_CONNECTING:
                return "DATA_CONNECTING(" + dataConnectionState + ")";
            case TelephonyManager.DATA_CONNECTED:
                return "DATA_CONNECTED(" + dataConnectionState + ")";
            case TelephonyManager.DATA_SUSPENDED:
                return "DATA_SUSPENDED(" + dataConnectionState + ")";
            default:
                return "DATA_UNKNOWN(" + dataConnectionState + ")";
        }
    }

    /**
     * @param networkType See {@link TelephonyManager#getNetworkType()}
     * @return never null
     */
    public static String networkTypeToString(int networkType)
    {
        switch (networkType)
        {
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "NETWORK_TYPE_1xRTT(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "NETWORK_TYPE_CDMA(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "NETWORK_TYPE_EDGE(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "NETWORK_TYPE_EHRPD(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "NETWORK_TYPE_EVDO_0(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "NETWORK_TYPE_EVDO_A(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "NETWORK_TYPE_EVDO_B(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "NETWORK_TYPE_GPRS(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "NETWORK_TYPE_HSDPA(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "NETWORK_TYPE_HSPA(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "NETWORK_TYPE_HSPAP(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "NETWORK_TYPE_HSUPA(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "NETWORK_TYPE_IDEN(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "NETWORK_TYPE_LTE(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "NETWORK_TYPE_UMTS(" + networkType + ")";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                return "NETWORK_TYPE_UNKNOWN(" + networkType + ")";
        }
    }
}