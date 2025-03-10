package com.smartfoo.android.core.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.R;
import com.smartfoo.android.core.logging.FooLog;

public class FooDataConnectionListener
{
    private static final String TAG = FooLog.TAG(FooDataConnectionListener.class);

    public static boolean VERBOSE_LOG = false;

    public interface FooDataConnectionListenerCallbacks
    {
        void onDataConnected(FooDataConnectionInfo dataConnectionInfo);

        void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo);
    }

    public static class FooDataConnectionInfo
    {
        /**
         * Intentionally not "UNKNOWN": Could mean that we are in airplane mode and have no current network connection
         * at all.
         */
        public static final int    TYPE_NONE      = -1;
        public static final String TYPE_NONE_NAME = "NONE";

        /**
         * Per:
         * http://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID()
         * https://github.com/android/platform_frameworks_base/blob/master/wifi/java/android/net/wifi/WifiSsid.java#L47
         */
        public static final String SSID_NOT_CONNECTED = "<unknown ssid>";

        /**
         * See {@link NetworkInfo#getType()}
         */
        private int    mType;
        private String mTypeName;

        /**
         * Depends on {@link #mType}:
         * <ul>
         * <li>ConnectivityManager.TYPE_BLUETOOTH: see ?</li>
         * <li>ConnectivityManager.TYPE_ETHERNET: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE: see {@link TelephonyManager#getNetworkType()}</li>
         * <li>ConnectivityManager.TYPE_MOBILE_DUN: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE_HIPRI: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE_MMS: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE_SUPL: see ?</li>
         * <li>ConnectivityManager.TYPE_WIFI: see ?</li>
         * <li>ConnectivityManager.TYPE_WIMAX: see ?</li>
         * </ul>
         */
        private int    mSubtype;
        private String mSubtypeName;

        private String mSSID;

        private boolean mIsConnected;

        FooDataConnectionInfo(NetworkInfo networkInfo, String ssid)
        {
            update(networkInfo, ssid);
        }

        /**
         * @return See {@link NetworkInfo#getType()}
         */
        public int getType()
        {
            return mType;
        }

        /**
         * <ul>
         * <li>ConnectivityManager.TYPE_BLUETOOTH: see ?</li>
         * <li>ConnectivityManager.TYPE_ETHERNET: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE: see {@link TelephonyManager#getNetworkType()}</li>
         * <li>ConnectivityManager.TYPE_MOBILE_DUN: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE_HIPRI: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE_MMS: see ?</li>
         * <li>ConnectivityManager.TYPE_MOBILE_SUPL: see ?</li>
         * <li>ConnectivityManager.TYPE_WIFI: see ?</li>
         * <li>ConnectivityManager.TYPE_WIMAX: see ?</li>
         * </ul>
         *
         * @return Depends on {@link #getType()}
         */
        public int getSubtype()
        {
            return mSubtype;
        }

        public String getSSID()
        {
            return mSSID;
        }

        /**
         * @param networkInfo
         * @param ssid        ignored if networkInfo.getType() != ConnectivityManager.TYPE_WIFI
         */
        private void update(NetworkInfo networkInfo, String ssid)
        {
            if (networkInfo != null)
            {
                setConnected(true);
                mType = networkInfo.getType();
                mTypeName = networkInfo.getTypeName();
                if (mType == ConnectivityManager.TYPE_WIFI)
                {
                    // mType == TYPE_WIFI has only SSID and no mSubtype/mSubtypeName
                    mSubtype = TYPE_NONE;
                    mSubtypeName = TYPE_NONE_NAME;
                    mSSID = ssid;
                }
                else
                {
                    // mType != TYPE_WIFI has only mSubtype/mSubtypeName and no SSID
                    mSubtype = networkInfo.getSubtype();
                    mSubtypeName = networkInfo.getSubtypeName();
                    mSSID = "";
                }
            }
            else
            {
                setConnected(false);
                mType = TYPE_NONE;
                mTypeName = TYPE_NONE_NAME;
                mSubtype = TYPE_NONE;
                mSubtypeName = TYPE_NONE_NAME;
                mSSID = "";
            }
            FooLog.v(TAG, "update: " + this);
        }

        void setConnected(boolean value)
        {
            mIsConnected = value;
        }

        public boolean isConnected()
        {
            return mIsConnected;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder() //
                    .append('{') //
                    .append("mIsConnected=").append(mIsConnected) //
                    .append(", mType=").append(mTypeName).append('(').append(mType).append(')');
            if (mType == ConnectivityManager.TYPE_WIMAX)
            {
                sb.append(", mSSID=").append(FooString.quote(mSSID));
            }
            else
            {
                sb.append(", mSubtype=").append(mSubtypeName).append('(').append(mSubtype).append(')');
            }
            sb.append('}');
            return sb.toString();
        }

        public String toSpeech(@NonNull Context context)
        {
            FooRun.throwIllegalArgumentExceptionIfNull(context, "context");

            StringBuilder sb = new StringBuilder();

            int resId = getNetworkTypeResourceId(true);
            String text = context.getString(resId);
            sb.append(text);

            if (mType == ConnectivityManager.TYPE_WIFI && !FooString.isNullOrEmpty(mSSID))
            {
                sb.append(' ').append(mSSID);
            }

            String speech = sb.toString().trim();
            speech = speech.replaceAll("Wi-Fi", "WIFI");//.replace(".", " ");
            speech = FooString.separateCamelCaseWords(speech);
            speech = speech.replaceAll("W I F I", "WIFI");
            speech = speech.replaceAll("(?i)(.)(?i)inc", "$1 ink"); // "P Binc" to "P B ink"
            return speech;
        }

        /**
         * @param debug false to return a generic cellular network type, true to return detailed cellular network type
         * @return The resource id for the string that represents the connection type, or
         * R.string.network_source_Unknown or R.string.network_source_None
         */
        public int getNetworkTypeResourceId(boolean debug)
        {
            switch (mType)
            {
                case TYPE_NONE:
                    return R.string.network_source_None;
                case ConnectivityManager.TYPE_BLUETOOTH:
                    return R.string.network_source_Bluetooth;
                case ConnectivityManager.TYPE_ETHERNET:
                    return R.string.network_source_Ethernet;
                case ConnectivityManager.TYPE_WIFI:
                    return R.string.network_source_WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                    if (debug)
                    {
                        switch (mSubtype)
                        {
                            case TelephonyManager.NETWORK_TYPE_1xRTT:
                                return R.string.network_type_cellular_1xRTT;
                            case TelephonyManager.NETWORK_TYPE_CDMA:
                                return R.string.network_type_cellular_CDMA;
                            case TelephonyManager.NETWORK_TYPE_EDGE:
                                return R.string.network_type_cellular_EDGE;
                            case TelephonyManager.NETWORK_TYPE_EHRPD:
                                return R.string.network_type_cellular_eHRPD;
                            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                                return R.string.network_type_cellular_EVDO_0;
                            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                                return R.string.network_type_cellular_EVDO_A;
                            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                                return R.string.network_type_cellular_EVDO_B;
                            case TelephonyManager.NETWORK_TYPE_GPRS:
                                return R.string.network_type_cellular_GPRS;
                            case TelephonyManager.NETWORK_TYPE_HSDPA:
                                return R.string.network_type_cellular_HSDPA;
                            case TelephonyManager.NETWORK_TYPE_HSPA:
                                return R.string.network_type_cellular_HSPA;
                            case TelephonyManager.NETWORK_TYPE_HSPAP:
                                return R.string.network_type_cellular_HSPAP;
                            case TelephonyManager.NETWORK_TYPE_HSUPA:
                                return R.string.network_type_cellular_HSUPA;
                            case TelephonyManager.NETWORK_TYPE_IDEN:
                                return R.string.network_type_cellular_iDen;
                            case TelephonyManager.NETWORK_TYPE_LTE:
                                return R.string.network_type_cellular_LTE;
                            case TelephonyManager.NETWORK_TYPE_UMTS:
                                return R.string.network_type_cellular_UMTS;
                        }
                    }
                    else
                    {
                        return R.string.network_source_cellular;
                    }
                case ConnectivityManager.TYPE_WIMAX:
                    return R.string.network_source_WIMAX;
                default:
                    return R.string.network_source_Unknown;
            }
        }
    }

    private final Object mSyncLock = new Object();

    private final Context                     mContext;
    private final ConnectivityManager         mConnectivityManager;
    private final WifiManager                 mWifiManager;
    private final FooDataConnectionInfo       mDataConnectionInfo;
    private final NetworkConnectivityReceiver mNetworkReceiver;

    private boolean mIsStarted;

    private FooDataConnectionListenerCallbacks mCallbacks;

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public FooDataConnectionListener(@NonNull Context context)
    {
        mContext = FooRun.toNonNull(context, "context");

        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        String ssid = null;
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null)
        {
            ssid = wifiInfo.getSSID();
        }
        mDataConnectionInfo = new FooDataConnectionInfo(activeNetworkInfo, ssid);
        FooLog.v(TAG, "FooDataConnectionListener: mDataConnectionInfo=" + mDataConnectionInfo);

        mNetworkReceiver = new NetworkConnectivityReceiver();
    }

    @Override
    public String toString()
    {
        return "{ mDataConnectionInfo=" + mDataConnectionInfo + " }";
    }

    public boolean isStarted()
    {
        synchronized (mSyncLock)
        {
            return mIsStarted;
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @NonNull
    public FooDataConnectionInfo getDataConnectionInfo()
    {
        synchronized (mSyncLock)
        {
            if (!mIsStarted)
            {
                NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
                String ssid = null;
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (wifiInfo != null)
                {
                    ssid = wifiInfo.getSSID();
                }
                mDataConnectionInfo.update(activeNetworkInfo, ssid);
            }
            return mDataConnectionInfo;
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public void start(@NonNull FooDataConnectionListenerCallbacks callbacks)
    {
        FooLog.v(TAG, "+start(...)");
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        synchronized (mSyncLock)
        {
            if (mIsStarted)
            {
                return;
            }

            mIsStarted = true;

            mCallbacks = callbacks;

            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetworkReceiver, filter);

            NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            String ssid = null;
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null)
            {
                ssid = wifiInfo.getSSID();
            }
            mDataConnectionInfo.update(activeNetworkInfo, ssid);
        }
        FooLog.v(TAG, "-start(...)");
    }

    public void stop()
    {
        FooLog.v(TAG, "+stop()");
        synchronized (mSyncLock)
        {
            if (!mIsStarted)
            {
                return;
            }

            mIsStarted = false;

            mContext.unregisterReceiver(mNetworkReceiver);
        }
        FooLog.v(TAG, "-stop()");
    }

    private class NetworkConnectivityReceiver
            extends BroadcastReceiver
    {
        private final String TAG = FooLog.TAG(NetworkConnectivityReceiver.class);

        /**
         * Necessarily complex due to issues discussed in: http://stackoverflow.com/q/5276032/252308
         */
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (VERBOSE_LOG)
            {
                FooLog.v(TAG, "onReceive: action=" + FooString.quote(action));
            }

            NetworkInfo networkInfo = null;
            int networkType = FooDataConnectionInfo.TYPE_NONE;
            String networkTypeName = FooDataConnectionInfo.TYPE_NONE_NAME;
            String ssid = null;
            boolean isConnected = false;
            boolean isDisconnected = false;
            boolean isConnectedOrConnecting = false;
            boolean isRoaming = false;

            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action))
            {
                //
                // WIFI
                //
                networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null)
                {
                    networkType = networkInfo.getType();
                    networkTypeName = networkInfo.getTypeName();

                    // NOTE: networkType should always be ConnectivityManager.TYPE_WIFI

                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    if (wifiInfo != null)
                    {
                        ssid = wifiInfo.getSSID();
                    }

                    // NOTE: NetworkInfo.isConnected() is [supposedly] not reliable for WIFI

                    NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();
                    if (VERBOSE_LOG)
                    {
                        FooLog.v(TAG, "onReceive: detailedState=" + detailedState);
                    }
                    isConnected = detailedState == NetworkInfo.DetailedState.CONNECTED;
                    isDisconnected = !isConnected;

                    isConnectedOrConnecting = networkInfo.isConnectedOrConnecting();
                    isRoaming = networkInfo.isRoaming();
                }
                else
                {
                    isDisconnected = true;
                }
            }
            else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
            {
                boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
                if (VERBOSE_LOG)
                {
                    FooLog.v(TAG, "onReceive: EXTRA_IS_FAILOVER=" + isFailover);
                }

                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (VERBOSE_LOG)
                {
                    FooLog.v(TAG, "onReceive: EXTRA_NO_CONNECTIVITY=" + noConnectivity);
                }

                networkInfo = mConnectivityManager.getActiveNetworkInfo();
                if (networkInfo != null)
                {
                    networkType = networkInfo.getType();
                    networkTypeName = networkInfo.getTypeName();

                    if (networkType == ConnectivityManager.TYPE_WIFI)
                    {
                        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                        if (wifiInfo != null)
                        {
                            ssid = wifiInfo.getSSID();
                        }

                        // NOTE: NetworkInfo.isConnected() is [supposedly] not reliable for WIFI

                        NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();
                        if (VERBOSE_LOG)
                        {
                            FooLog.v(TAG, "onReceive: detailedState=" + detailedState);
                        }
                        isConnected = detailedState == NetworkInfo.DetailedState.CONNECTED;
                        isDisconnected = !isConnected;
                    }
                    else
                    {
                        isConnected = networkInfo.isConnected() && !noConnectivity;
                        isDisconnected = !isConnected;
                    }

                    isConnectedOrConnecting = networkInfo.isConnectedOrConnecting();
                    isRoaming = networkInfo.isRoaming();
                }
                else
                {
                    isDisconnected = true;
                }
            }

            if (VERBOSE_LOG)
            {
                FooLog.v(TAG, "onReceive: networkInfo=" + networkInfo);
                FooLog.v(TAG, "onReceive: networkType=" + networkTypeName + '(' + networkType + ')');
                FooLog.v(TAG, "onReceive: ssid=" + ssid);
                FooLog.v(TAG, "onReceive: isConnected=" + isConnected);
                FooLog.v(TAG, "onReceive: isDisconnected=" + isDisconnected);
                FooLog.v(TAG, "onReceive: isConnectedOrConnecting=" + isConnectedOrConnecting);
                FooLog.v(TAG, "onReceive: isRoaming=" + isRoaming);
            }

            //
            // NOTE: There should only be 3 possibilities: Disconnected, Connecting, or Connected
            //

            if (isConnected)
            {
                onDataConnected(networkInfo, ssid);
            }
            else if (isDisconnected && !isConnectedOrConnecting)
            {
                // NOTE: We don't have any use for notifying Disconnected while Connect*ING*
                onDataDisconnected(networkInfo, ssid);
            }
            else
            {
                if (VERBOSE_LOG)
                {
                    FooLog.v(TAG, "isConnectedOrConnecting: ignoring");
                }
            }
        }
    }

    /**
     * @param networkInfo networkInfo
     * @param ssid        ssid
     */
    private void onDataConnected(NetworkInfo networkInfo, String ssid)
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, "onDataConnected(" + networkInfo + ", " + FooString.quote(ssid) + ')');
        }

        int networkType = FooDataConnectionInfo.TYPE_NONE;
        String networkTypeName = FooDataConnectionInfo.TYPE_NONE_NAME;
        int networkSubtype = FooDataConnectionInfo.TYPE_NONE;
        String networkSubtypeName = FooDataConnectionInfo.TYPE_NONE_NAME;
        if (networkInfo != null)
        {
            networkType = networkInfo.getType();
            networkTypeName = networkInfo.getTypeName();
            networkSubtype = networkInfo.getSubtype();
            networkSubtypeName = networkInfo.getSubtypeName();
        }
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, "onDataConnected: networkType=" + networkTypeName + '(' + networkType + ')');
            FooLog.v(TAG, "onDataConnected: networkSubType=" + networkSubtypeName + '(' + networkSubtype + ')');
            FooLog.v(TAG, "onDataConnected: ssid=" + ssid);
            FooLog.v(TAG, "onDataConnected: mDataConnectionInfo=" + mDataConnectionInfo);
        }

        //
        // Especially when switching network types, we may not always get a Disconnect event before a Connect event…
        // ...because that network may still technically be connected (ex: LTE->WiFi; LTE stays connected).
        // Ignore connect events that match the connection we are already connected to.
        // Notify connect events that are different than the connection we are already connected to.
        //
        final boolean notify;

        if (networkType == mDataConnectionInfo.getType())
        {
            if (networkType == ConnectivityManager.TYPE_WIFI)
            {
                //
                // Special case for WIFI to detect if SSID has changed
                //

                if (ssid == null)
                {
                    if (mDataConnectionInfo.isConnected())
                    {
                        if (VERBOSE_LOG)
                        {
                            FooLog.v(TAG,
                                    "onDataConnected: Same network Type, SSID == null, already connected; ignoring");
                        }
                        notify = false;
                    }
                    else
                    {
                        FooLog.d(TAG,
                                "onDataConnected: Same network Type, SSID == null, not already connected; notifying callbacks");
                        notify = true;
                    }
                }
                else if (ssid.equals(mDataConnectionInfo.getSSID()))
                {
                    if (mDataConnectionInfo.isConnected())
                    {
                        if (VERBOSE_LOG)
                        {
                            FooLog.v(TAG,
                                    "onDataConnected: Same network Type/SSID, already connected; ignoring");
                        }
                        notify = false;
                    }
                    else
                    {
                        FooLog.d(TAG,
                                "onDataConnected: Same network Type/SSID, not already connected; notifying callbacks");
                        notify = true;
                    }
                }
                else if (ssid.equals(FooDataConnectionInfo.SSID_NOT_CONNECTED))
                {
                    if (VERBOSE_LOG)
                    {
                        FooLog.v(TAG,
                                "onDataConnected: Same network Type, unconnected SSID=" +
                                FooString.quote(FooDataConnectionInfo.SSID_NOT_CONNECTED) + "; ignoring");
                    }
                    notify = false;
                }
                else
                {
                    FooLog.d(TAG,
                            "onDataConnected: Same network Type, different SSID; notifying callbacks");
                    notify = true;
                }
            }
            else
            {
                //
                // Special case for non-WIFI to detect if networkSubtype has changed
                //

                if (networkSubtype == mDataConnectionInfo.getSubtype())
                {
                    if (mDataConnectionInfo.isConnected())
                    {
                        if (VERBOSE_LOG)
                        {
                            FooLog.v(TAG,
                                    "onDataConnected: Same network Type/Subtype, already connected; ignoring");
                        }
                        notify = false;
                    }
                    else
                    {
                        FooLog.d(TAG,
                                "onDataConnected: Same network Type/Subtype, not already connected; notifying callbacks");
                        notify = true;
                    }
                }
                else
                {
                    FooLog.d(TAG,
                            "onDataConnected: Same network Type, different Subtype; notifying callbacks");
                    notify = true;
                }
            }
        }
        else
        {
            FooLog.d(TAG,
                    "onDataConnected: Different network Type; notifying callbacks");
            notify = true;
        }

        if (notify)
        {
            mDataConnectionInfo.update(networkInfo, ssid);

            mCallbacks.onDataConnected(mDataConnectionInfo);
        }
    }

    /**
     * In general:
     * <ul>
     * <li>If connected over wifi and wifi disconnects, notify.</li>
     * <li>If connected over cellular and cellular disconnects, notify.</li>
     * <li>If connected over wifi and cellular disconnects, ignore.</li>
     * <li>If connected over cellular and wifi disconnects, ignore (not actually possible, but event ordering could
     * make it look this way).</li>
     * </ul>
     *
     * @param networkInfo networkInfo
     * @param ssid        ssid
     */
    private void onDataDisconnected(NetworkInfo networkInfo, String ssid)
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, "onDataDisconnected(" + networkInfo + ", " + FooString.quote(ssid) + ')');
        }

        //
        // Ignore disconnect events from networks that are not the one we are connected to,
        // or if we are already disconnected.
        //

        final boolean notify;

        if (networkInfo == null)
        {
            if (mDataConnectionInfo.isConnected())
            {
                FooLog.d(TAG,
                        "onDataDisconnected: networkInfo == null (AIRPLANE MODE?), not already disconnected; notifying callbacks");
                notify = true;
            }
            else
            {
                if (VERBOSE_LOG)
                {
                    FooLog.v(TAG,
                            "onDataDisconnected: networkInfo == null (AIRPLANE MODE?), already disconnected; ignoring");
                }
                notify = false;
            }
        }
        else
        {
            int networkType = networkInfo.getType();
            String networkTypeName = networkInfo.getTypeName();
            int networkSubtype = networkInfo.getSubtype();
            String networkSubtypeName = networkInfo.getSubtypeName();
            if (VERBOSE_LOG)
            {
                FooLog.v(TAG, "onDataDisconnected: networkType=" + networkTypeName + '(' + networkType + ')');
                FooLog.v(TAG, "onDataDisconnected: networkSubType=" + networkSubtypeName + '(' + networkSubtype + ')');
                FooLog.v(TAG, "onDataDisconnected: ssid=" + FooString.quote(ssid));
                FooLog.v(TAG, "onDataDisconnected: mDataConnectionInfo=" + mDataConnectionInfo);
            }

            if (networkType == mDataConnectionInfo.getType())
            {
                if (networkType == ConnectivityManager.TYPE_WIFI)
                {
                    //
                    // Special case for WIFI to detect if SSID has changed
                    //

                    if (ssid == null)
                    {
                        if (mDataConnectionInfo.isConnected())
                        {
                            FooLog.d(TAG,
                                    "onDataDisconnected: networkType == WIFI, SSID == null, not already disconnected; notifying callbacks");
                            notify = true;
                        }
                        else
                        {
                            if (VERBOSE_LOG)
                            {
                                FooLog.v(TAG,
                                        "onDataDisconnected: networkType == WIFI, SSID == null, already disconnected; ignoring");
                            }
                            notify = false;
                        }
                    }
                    else if (ssid.equals(mDataConnectionInfo.getSSID()))
                    {
                        if (mDataConnectionInfo.isConnected())
                        {
                            FooLog.d(TAG,
                                    "onDataDisconnected: networkType == WIFI, same SSID, not already disconnected; notifying callbacks");
                            notify = true;
                        }
                        else
                        {
                            if (VERBOSE_LOG)
                            {
                                FooLog.v(TAG,
                                        "onDataDisconnected: networkType == WIFI, same SSID, already disconnected; ignoring");
                            }
                            notify = false;
                        }
                    }
                    else
                    {
                        if (VERBOSE_LOG)
                        {
                            FooLog.v(TAG,
                                    "onDataDisconnected: networkType == WIFI, different SSID; ignoring");
                        }
                        notify = false;
                    }
                }
                else
                {
                    //
                    // Special case for non-WIFI to detect if networkSubtype has changed
                    //

                    if (networkSubtype == mDataConnectionInfo.getSubtype())
                    {
                        if (mDataConnectionInfo.isConnected())
                        {
                            FooLog.d(TAG,
                                    "onDataDisconnected: Same network Type/Subtype, not already disconnected; notifying callbacks");
                            notify = true;
                        }
                        else
                        {
                            if (VERBOSE_LOG)
                            {
                                FooLog.v(TAG,
                                        "onDataDisconnected: Same network Type/Subtype, already disconnected; ignoring");
                            }
                            notify = false;
                        }
                    }
                    else
                    {
                        if (VERBOSE_LOG)
                        {
                            FooLog.v(TAG,
                                    "onDataDisconnected: Same network Type, different Subtype; ignoring");
                        }
                        notify = false;
                    }
                }
            }
            else
            {
                if (VERBOSE_LOG)
                {
                    FooLog.v(TAG,
                            "onDataDisconnected: Different network Type; ignoring");
                }
                notify = false;
            }
        }

        if (notify)
        {
            mDataConnectionInfo.setConnected(false);

            mCallbacks.onDataDisconnected(mDataConnectionInfo);
        }
    }
}