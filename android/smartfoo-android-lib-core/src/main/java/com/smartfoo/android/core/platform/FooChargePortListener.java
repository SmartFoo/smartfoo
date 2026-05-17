package com.smartfoo.android.core.platform;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
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

        /**
         * Returns the string resource ID associated with this charge-port type.
         *
         * @return a {@link androidx.annotation.StringRes} resource ID
         */
        @StringRes
        public int getStringRes()
        {
            return mResId;
        }

        /**
         * Returns a localised display name for the given {@code chargePort}, or null if
         * {@code chargePort} is null.
         *
         * @param context    context used to resolve the string resource
         * @param chargePort the port to name; may be null
         * @return the localised string, or null
         */
        public static String toString(@NonNull Context context, ChargePort chargePort)
        {
            return chargePort != null ? FooRes.getString(context, chargePort.getStringRes()) : null;
        }
    }

    /** Callback interface for charge-port connection events. */
    public interface FooChargePortListenerCallbacks
    {
        /**
         * Called when a new charge port is connected.
         *
         * @param chargePort the type of port that was connected
         */
        void onChargePortConnected(ChargePort chargePort);

        /**
         * Called when an existing charge port is disconnected.
         *
         * @param chargePort the type of port that was disconnected
         */
        void onChargePortDisconnected(ChargePort chargePort);
    }

    @NonNull
    /**
     * Returns the current sticky {@link Intent#ACTION_BATTERY_CHANGED} intent by registering a
     * null receiver.
     *
     * @param context the application context
     * @return the battery-changed intent; never null
     * @throws IllegalArgumentException if {@code context} is null
     */
    public static Intent getBatteryChargingIntent(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //noinspection ConstantConditions
        return context.registerReceiver(null, intentFilter);
    }

    /**
     * Returns true if the device is currently charging or the battery is full.
     *
     * @param context the application context
     * @return true if charging or full
     */
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

    /**
     * Returns a list of charge ports currently connected to the device.
     *
     * @param context the application context
     * @return a list of active {@link ChargePort} values; empty if nothing is plugged in
     */
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

    /**
     * Returns a list of localised display names for all charge ports currently connected.
     *
     * @param context the application context used to resolve string resources
     * @return a list of localised charge-port name strings; empty if nothing is plugged in
     */
    public static List<String> getChargingPortsNames(@NonNull Context context)
    {
        return getChargePortNames(context, getChargingPorts(context));
    }

    /**
     * Returns a list of localised display names for the given charge ports.
     *
     * @param context     the application context used to resolve string resources
     * @param chargePorts the ports to name; null entries are silently skipped
     * @return a list of localised charge-port name strings
     */
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

    private final Context                                                     mContext;
    private final FooListenerAutoStartManager<FooChargePortListenerCallbacks> mListenerManager;
    private final FooScreenBroadcastReceiver                                  mScreenBroadcastReceiver;

    /**
     * Constructs a new listener bound to the given context.
     *
     * @param context the application context used to register/unregister the broadcast receiver
     * @throws IllegalArgumentException if {@code context} is null
     */
    public FooChargePortListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
            /** Starts the charge-port broadcast receiver when the first external callback is attached. */
            @Override
            public void onFirstAttach()
            {
                mScreenBroadcastReceiver.start(new FooChargePortListenerCallbacks()
                {
                    /** Delegates to all attached {@link FooChargePortListenerCallbacks#onChargePortConnected} listeners. */
                    @Override
                    public void onChargePortConnected(ChargePort chargePort)
                    {
                        for (FooChargePortListenerCallbacks callbacks : mListenerManager.beginTraversing())
                        {
                            callbacks.onChargePortConnected(chargePort);
                        }
                        mListenerManager.endTraversing();
                    }

                    /** Delegates to all attached {@link FooChargePortListenerCallbacks#onChargePortDisconnected} listeners. */
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

            /**
             * Stops the charge-port broadcast receiver when the last external callback is detached.
             *
             * @return {@code false} to allow the listener manager to remove the internal callback entry
             */
            @Override
            public boolean onLastDetach()
            {
                mScreenBroadcastReceiver.stop();
                return false;
            }
        });
        mScreenBroadcastReceiver = new FooScreenBroadcastReceiver(context);
    }

    /**
     * Returns true if the device is currently charging or the battery is full.
     *
     * @return true if charging or full
     */
    public boolean isCharging()
    {
        return isCharging(mContext);
    }

    /**
     * Returns a list of charge ports currently connected to the device.
     *
     * @return a list of active {@link ChargePort} values; empty if nothing is plugged in
     */
    public List<ChargePort> getChargingPorts()
    {
        return getChargingPorts(mContext);
    }

    /**
     * Registers {@code callbacks} to receive charge-port connection events.
     *
     * <p>The underlying broadcast receiver is registered automatically when the first callbacks
     * instance is attached.</p>
     *
     * @param callbacks the listener to register; no-op if already registered
     */
    public void attach(FooChargePortListenerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
    }

    /**
     * Unregisters previously registered {@code callbacks}.
     *
     * <p>The underlying broadcast receiver is unregistered automatically when the last callbacks
     * instance is detached.</p>
     *
     * @param callbacks the listener to remove; no-op if not registered
     */
    public void detach(FooChargePortListenerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
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

        /**
         * Returns {@code true} if this receiver is currently registered with the system.
         *
         * @return {@code true} if started
         */
        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

        /**
         * Registers this receiver to listen for power-connected and power-disconnected broadcasts.
         * Captures the current charging port state as a baseline for diffing. Does nothing if
         * already started.
         *
         * @param callbacks the callbacks to notify on charge-port events; must not be null
         */
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

        /**
         * Unregisters this receiver from the system. Does nothing if not currently started.
         */
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

        /**
         * Handles {@link Intent#ACTION_POWER_CONNECTED} and {@link Intent#ACTION_POWER_DISCONNECTED}
         * broadcasts by diffing the current charging ports against the previous snapshot and
         * dispatching the appropriate connected/disconnected callbacks.
         *
         * @param context the context in which the receiver is running
         * @param intent  the received power intent
         */
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
