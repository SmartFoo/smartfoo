package com.smartfoo.android.core.platform;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.R;
import com.smartfoo.android.core.logging.FooLog;

import java.util.LinkedList;
import java.util.List;

/**
 * Reference:
 * https://developer.android.com/training/monitoring-device-state/battery-monitoring.html
 */
public class FooChargePortListener
{
    private static final String TAG = FooLog.TAG(FooScreenListener.class);

    public enum ChargePort
    {
        Unknown(R.string.charge_port_unknown_charger),
        AC(R.string.charge_port_ac_charger),
        USB(R.string.charge_port_usb_port),
        Wireless(R.string.charge_port_wireless_charger);

        final int mResId;

        ChargePort(@StringRes int resId)
        {
            mResId = resId;
        }

        @StringRes
        public int getStringRes()
        {
            return mResId;
        }

        public static String toString(@NonNull Context context, ChargePort chargePort)
        {
            return chargePort != null ? FooRes.getString(context, chargePort.getStringRes()) : null;
        }
    }

    public interface FooChargePortListenerCallbacks
    {
        void onChargePortConnected(ChargePort chargePort);

        void onChargePortDisconnected(ChargePort chargePort);
    }

    @NonNull
    public static Intent getBatteryChargingIntent(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //noinspection ConstantConditions
        return context.registerReceiver(null, intentFilter);
    }

    public static boolean isCharging(@NonNull Context context)
    {
        return isCharging(getBatteryChargingIntent(context));
    }

    private static boolean isCharging(@NonNull Intent intent)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(intent, "intent");
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }

    @NonNull
    public static List<ChargePort> getChargingPorts(@NonNull Context context)
    {
        return getChargingPorts(getBatteryChargingIntent(context));
    }

    @NonNull
    private static List<ChargePort> getChargingPorts(@NonNull Intent intent)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(intent, "intent");
        int chargingPorts = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        return getChargingPorts(chargingPorts);
    }

    public static List<String> getChargingPortsNames(@NonNull Context context)
    {
        return getChargePortNames(context, getChargingPorts(context));
    }

    public static List<String> getChargePortNames(@NonNull Context context, List<ChargePort> chargePorts)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        List<String> chargingPortsNames = new LinkedList<>();
        for (ChargePort chargePort : chargePorts)
        {
            if (chargePort != null)
            {
                chargingPortsNames.add(context.getString(chargePort.getStringRes()));
            }
        }
        return chargingPortsNames;
    }

    @NonNull
    private static List<ChargePort> getChargingPorts(int chargingPorts)
    {
        List<ChargePort> result = new LinkedList<>();
        if (chargingPorts != 0)
        {
            if ((chargingPorts & BatteryManager.BATTERY_PLUGGED_USB) == BatteryManager.BATTERY_PLUGGED_USB)
            {
                result.add(ChargePort.USB);
                chargingPorts &= ~BatteryManager.BATTERY_PLUGGED_USB;
            }
            if ((chargingPorts & BatteryManager.BATTERY_PLUGGED_WIRELESS) == BatteryManager.BATTERY_PLUGGED_WIRELESS)
            {
                result.add(ChargePort.Wireless);
                chargingPorts &= ~BatteryManager.BATTERY_PLUGGED_WIRELESS;
            }
            if ((chargingPorts & BatteryManager.BATTERY_PLUGGED_AC) == BatteryManager.BATTERY_PLUGGED_AC)
            {
                result.add(ChargePort.AC);
                chargingPorts &= ~BatteryManager.BATTERY_PLUGGED_AC;
            }
            if (chargingPorts != 0)
            {
                result.add(ChargePort.Unknown);
            }
        }
        return result;
    }

    private final Context                                            mContext;
    private final FooListenerManager<FooChargePortListenerCallbacks> mListenerManager;
    private final FooScreenBroadcastReceiver                         mScreenBroadcastReceiver;

    public FooChargePortListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
        mListenerManager = new FooListenerManager<>();
        mScreenBroadcastReceiver = new FooScreenBroadcastReceiver(context);
    }

    public boolean isCharging()
    {
        return isCharging(mContext);
    }

    public List<ChargePort> getChargingPorts()
    {
        return getChargingPorts(mContext);
    }

    public void attach(FooChargePortListenerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
        if (mListenerManager.size() == 1 && !mScreenBroadcastReceiver.isStarted())
        {
            mScreenBroadcastReceiver.start(new FooChargePortListenerCallbacks()
            {
                @Override
                public void onChargePortConnected(ChargePort chargePort)
                {
                    for (FooChargePortListenerCallbacks callbacks : mListenerManager.beginTraversing())
                    {
                        callbacks.onChargePortConnected(chargePort);
                    }
                    mListenerManager.endTraversing();
                }

                @Override
                public void onChargePortDisconnected(ChargePort chargePort)
                {
                    for (FooChargePortListenerCallbacks callbacks : mListenerManager.beginTraversing())
                    {
                        callbacks.onChargePortDisconnected(chargePort);
                    }
                    mListenerManager.endTraversing();
                }
            });
        }
    }

    public void detach(FooChargePortListenerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
        if (mListenerManager.size() == 0 && mScreenBroadcastReceiver.isStarted())
        {
            mScreenBroadcastReceiver.stop();
        }
    }

    private static class FooScreenBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(FooScreenBroadcastReceiver.class);

        private final Context mContext;
        private final Object  mSyncLock;

        private boolean                        mIsStarted;
        private FooChargePortListenerCallbacks mCallbacks;
        private List<ChargePort>               mChargingPortsPrevious;

        public FooScreenBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
            mSyncLock = new Object();
        }

        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

        public void start(@NonNull FooChargePortListenerCallbacks callbacks)
        {
            FooLog.v(TAG, "+start(...)");
            synchronized (mSyncLock)
            {
                if (!mIsStarted)
                {
                    mIsStarted = true;

                    mCallbacks = callbacks;

                    mChargingPortsPrevious = getChargingPorts(mContext);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Intent.ACTION_POWER_CONNECTED); // API 4
                    intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED); // API 4
                    mContext.registerReceiver(this, intentFilter);
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

                    mContext.unregisterReceiver(this);
                }
            }
            FooLog.v(TAG, "-stop()");
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            FooLog.v(TAG, "onReceive: intent == " + FooPlatformUtils.toString(intent));

            //FooLog.e(TAG,
            //        "onReceive: mChargingPortsPrevious == " + getChargePortNames(mContext, mChargingPortsPrevious));
            //FooLog.e(TAG,
            //        "onReceive: mChargingPortsPrevious == " + mChargingPortsPrevious);
            List<ChargePort> chargingPortsCurrent = getChargingPorts(context);
            //FooLog.e(TAG,
            //        "onReceive:   chargingPortsCurrent == " + chargingPortsCurrent);
            //FooLog.e(TAG,
            //        "onReceive:   chargingPortsCurrent == " + getChargePortNames(mContext, chargingPortsCurrent));

            for (ChargePort chargingPortPrevious : mChargingPortsPrevious)
            {
                if (!chargingPortsCurrent.contains(chargingPortPrevious))
                {
                    mCallbacks.onChargePortDisconnected(chargingPortPrevious);
                }
            }
            for (ChargePort chargingPortCurrent : chargingPortsCurrent)
            {
                if (!mChargingPortsPrevious.contains(chargingPortCurrent))
                {
                    mCallbacks.onChargePortConnected(chargingPortCurrent);
                }
            }

            mChargingPortsPrevious = chargingPortsCurrent;
        }
    }
}
