package com.smartfoo.android.core.network;

import android.Manifest;
import android.content.Context;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
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

    public interface FooCellularDataConnectionCallbacks
    {
        /**
         * @param dataNetworkType See {@link TelephonyManager#getDataNetworkType()}<br>
         *                        NOTE that these are different from {@link NetworkInfo#getType()}
         */
        void onCellularDataConnected(int dataNetworkType);

        /**
         * @param dataNetworkType See {@link TelephonyManager#getDataNetworkType()}<br>
         *                        NOTE that these are different from {@link NetworkInfo#getType()}
         */
        void onCellularDataDisconnected(int dataNetworkType);
    }

    private final Object mSyncLock = new Object();

    private final TelephonyManager mTelephonyManager;

    private boolean mIsStarted;

    private HookState mHookState = HookState.Unknown;

    private FooCellularHookStateCallbacks      mCallbacksHookState;
    private FooCellularDataConnectionCallbacks mCallbacksDataConnection;

    public FooCellularStateListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    private int getCallState()
    {
        return mTelephonyManager.getCallState();
    }

    public int getDataConnectionState()
    {
        return mTelephonyManager.getDataState();
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public int getDataConnectionType()
    {
        return mTelephonyManager.getDataNetworkType();
    }

    public boolean isStarted()
    {
        synchronized (mSyncLock)
        {
            return mIsStarted;
        }
    }

    public void start(FooCellularHookStateCallbacks callbacksHookState,
                      FooCellularDataConnectionCallbacks callbacksDataConnection)
    {
        FooLog.v(TAG, "+start(...)");
        synchronized (mSyncLock)
        {
            int events = PhoneStateListener.LISTEN_NONE;

            if (callbacksHookState != null)
            {
                events |= PhoneStateListener.LISTEN_CALL_STATE;
            }

            if (callbacksDataConnection != null)
            {
                events |= PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
            }

            if (events != PhoneStateListener.LISTEN_NONE)
            {
                if (!mIsStarted)
                {
                    mIsStarted = true;

                    mCallbacksHookState = callbacksHookState;
                    mCallbacksDataConnection = callbacksDataConnection;

                    mHookState = HookState.Unknown;
                    int callState = getCallState();
                    updateHookState(callState);

                    mTelephonyManager.listen(this, events);
                }
            }
        }
        FooLog.v(TAG, "-start(...)");
    }

    public void stop()
    {
        FooLog.v(TAG, "+stop()");
        synchronized (mSyncLock)
        {
            if (mIsStarted)
            {
                mIsStarted = false;

                mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);

                mHookState = HookState.Unknown;
            }
        }
        FooLog.v(TAG, "-stop()");
    }

    public enum HookState
    {
        /**
         * This should only occur in the rare moment that {@link TelephonyManager#getCallState()} returns {@link
         * TelephonyManager#CALL_STATE_RINGING} when {@link #start(FooCellularHookStateCallbacks,
         * FooCellularDataConnectionCallbacks)} is called. The HookState will become known when {@link
         * TelephonyManager#CALL_STATE_RINGING} inevitably stops ringing.
         * <p>
         * TL;DR: The javadoc for {@link TelephonyManager#CALL_STATE_OFFHOOK} says "Device call state: Off-hook. At
         * least one call exists that is dialing, active, or on hold, AND NO CALLS ARE RINGING OR WAITING." (emphasis
         * mine). This means that the Android API makes it impossible to determine if the phone is OffHook/In-A-Call
         * when {@link TelephonyManager#getCallState()} returns {@link TelephonyManager#CALL_STATE_RINGING}.
         */
        Unknown,
        OnHook,
        OffHook
    }

    public boolean isOnHook()
    {
        return getHookState() == HookState.OnHook;
    }

    public boolean isOffHook()
    {
        return getHookState() == HookState.OffHook;
    }

    /**
     * If started, then {@link HookState#OnHook}, {@link HookState#OffHook}, or {@link HookState#Unknown} in the rare
     * case that an incoming phone call is ringing when {@link #start(FooCellularHookStateCallbacks,
     * FooCellularDataConnectionCallbacks)} is called.
     * <p>
     * If not started, then will return a false {@link HookState#OnHook} if the phone is offhook and an incoming phone
     * call is ringing.
     *
     * @return {@link HookState}
     */
    public HookState getHookState()
    {
        if (mIsStarted)
        {
            return mHookState;
        }

        switch (getCallState())
        {
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return HookState.OffHook;
            default:
                return HookState.OnHook;
        }
    }

    /**
     * @param callState See {@link TelephonyManager#getCallState()}
     * @return true if mHookState changed, otherwise false
     */
    private boolean updateHookState(int callState)
    {
        HookState hookState;

        switch (callState)
        {
            case TelephonyManager.CALL_STATE_IDLE:
                hookState = HookState.OnHook;
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                return false;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                hookState = HookState.OffHook;
                break;
            default:
                throw new IllegalArgumentException("Unknown callState == " + getCallStateName(callState));
        }

        if (mHookState == hookState)
        {
            return false;
        }

        mHookState = hookState;

        return true;
    }

    @Override
    public void onCallStateChanged(int callState, String incomingNumber)
    {
        FooLog.i(TAG,
                "onCallStateChanged(callState=" + getCallStateName(callState)
                + ", incomingNumber=" + FooString.quote(incomingNumber) +
                ')');

        if (!updateHookState(callState))
        {
            return;
        }

        switch (mHookState)
        {
            case OffHook:
                mCallbacksHookState.onCellularOffHook();
                break;
            case OnHook:
                mCallbacksHookState.onCellularOnHook();
                break;
        }
    }

    @Override
    public void onDataConnectionStateChanged(int dataConnectionState, int dataNetworkType)
    {
        FooLog.i(TAG,
                "onDataConnectionStateChanged(dataConnectionState=" + getDataConnectionStateName(dataConnectionState) +
                ", dataNetworkType=" + getDataNetworkTypeName(dataNetworkType) +
                ')');

        switch (dataConnectionState)
        {
            case TelephonyManager.DATA_DISCONNECTED:
            case TelephonyManager.DATA_SUSPENDED:
                mCallbacksDataConnection.onCellularDataDisconnected(dataNetworkType);
                break;
            case TelephonyManager.DATA_CONNECTED:
                mCallbacksDataConnection.onCellularDataConnected(dataNetworkType);
                break;
            case TelephonyManager.DATA_CONNECTING:
                // ignore
                break;
            default:
                FooLog.w(TAG, "onDataConnectionStateChanged: Unhandled dataConnectionState="
                              + getDataConnectionStateName(dataConnectionState));
                break;
        }
    }

    /**
     * @param callState See {@link TelephonyManager#getCallState()}
     * @return name of the callState
     */
    public static String getCallStateName(int callState)
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

    /**
     * @param dataConnectionState See {@link TelephonyManager#getDataState()}
     * @return name of the dataConnectionState
     */
    public static String getDataConnectionStateName(int dataConnectionState)
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
     * Unknown network class.
     */
    public static final int NETWORK_CLASS_UNKNOWN = 0;
    /**
     * Class of broadly defined "2G" networks.
     */
    public static final int NETWORK_CLASS_2_G     = 1;
    /**
     * Class of broadly defined "3G" networks.
     */
    public static final int NETWORK_CLASS_3_G     = 2;
    /**
     * Class of broadly defined "4G" networks.
     */
    public static final int NETWORK_CLASS_4_G     = 3;

    public static final int NETWORK_TYPE_LTE_CA = 19;

    /**
     * @param dataNetworkType See {@link TelephonyManager#getDataNetworkType()}
     * @return NETWORK_CLASS_*
     */
    public static int getDataNetworkTypeClass(int dataNetworkType)
    {
        switch (dataNetworkType)
        {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_CLASS_2_G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return NETWORK_CLASS_3_G;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
            case NETWORK_TYPE_LTE_CA:
                return NETWORK_CLASS_4_G;
            default:
                return NETWORK_CLASS_UNKNOWN;
        }
    }

    /**
     * @param dataNetworkType See {@link TelephonyManager#getDataNetworkType()}
     * @return never null
     */
    public static String getDataNetworkTypeName(int dataNetworkType)
    {
        String name;
        switch (dataNetworkType)
        {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                name = "NETWORK_TYPE_GPRS";
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                name = "NETWORK_TYPE_EDGE";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                name = "NETWORK_TYPE_UMTS";
                break;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                name = "NETWORK_TYPE_CDMA";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                name = "NETWORK_TYPE_EVDO_0";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                name = "NETWORK_TYPE_EVDO_A";
                break;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                name = "NETWORK_TYPE_1xRTT";
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                name = "NETWORK_TYPE_HSDPA";
                break;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                name = "NETWORK_TYPE_HSUPA";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                name = "NETWORK_TYPE_HSPA";
                break;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                name = "NETWORK_TYPE_IDEN";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                name = "NETWORK_TYPE_EVDO_B";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                name = "NETWORK_TYPE_LTE";
                break;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                name = "NETWORK_TYPE_EHRPD";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                name = "NETWORK_TYPE_HSPAP";
                break;
            case TelephonyManager.NETWORK_TYPE_GSM:
                name = "NETWORK_TYPE_GSM";
                break;
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                name = "NETWORK_TYPE_TD_SCDMA";
                break;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                name = "NETWORK_TYPE_IWLAN";
                break;
            case NETWORK_TYPE_LTE_CA:
                name = "NETWORK_TYPE_LTE_CA";
                break;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                name = "NETWORK_TYPE_UNKNOWN";
                break;
        }
        return name + '(' + dataNetworkType + ')';
    }
}