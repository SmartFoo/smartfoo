package com.smartfoo.android.core.bluetooth.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.smartfoo.android.core.BuildConfig;
import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.FooBluetoothUtils;
import com.smartfoo.android.core.bluetooth.gatt.FooGattHandler.GattHandlerListener.DisconnectReason;
import com.smartfoo.android.core.bluetooth.gatt.FooGattHandler.GattHandlerListener.GattOperation;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooHandler;
import com.smartfoo.android.core.platform.FooHandlerThread;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@SuppressWarnings({ "JavaDoc", "unused" })
public class FooGattHandler
{
    private static final String TAG = FooLog.TAG(FooGattHandler.class);

    public static boolean VERBOSE_LOG_CHARACTERISTIC_CHANGE = false;

    public static final long DEFAULT_TIMEOUT_MILLIS            = 10 * 1000;
    public static final long DEFAULT_DISCONNECT_TIMEOUT_MILLIS = 250;

    private static long sDefaultTimeoutMillis           = DEFAULT_TIMEOUT_MILLIS;
    private static long sDefaultDisconnectTimeoutMillis = DEFAULT_DISCONNECT_TIMEOUT_MILLIS;

    public static long getDefaultTimeoutMillis()
    {
        return sDefaultTimeoutMillis;
    }

    public static void setDefaultTimeoutMillis(long timeoutMillis)
    {
        sDefaultTimeoutMillis = timeoutMillis;
    }

    public static long getDefaultDisconnectTimeoutMillis()
    {
        return sDefaultDisconnectTimeoutMillis;
    }

    public static void setDefaultDisconnectTimeoutMillis(long timeoutMillis)
    {
        sDefaultDisconnectTimeoutMillis = timeoutMillis;
    }

    /**
     * Well-Known UUID for <a href="https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml">
     * Client Characteristic Configuration</a>
     */
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = FooGattUuids.CLIENT_CHARACTERISTIC_CONFIG.getUuid();

    /**
     * Various wrappers around {@link android.bluetooth.BluetoothGattCallback} methods
     */
    public static abstract class GattHandlerListener
    {
        public enum GattOperation
        {
            Connect,
            DiscoverServices,
            CharacteristicRead,
            CharacteristicWrite,
            CharacteristicSetNotification,
            ReadRemoteRssi,
        }

        /**
         * @param gattHandler   gattHandler
         * @param operation     operation
         * @param timeoutMillis The requested timeout milliseconds
         * @param elapsedMillis The actual elapsed milliseconds
         * @return true to forcibly stay connected, false to allow disconnect
         */
        public boolean onDeviceOperationTimeout(FooGattHandler gattHandler, GattOperation operation, long timeoutMillis, long elapsedMillis)
        {
            return false;
        }

        public void onDeviceConnecting(FooGattHandler gattHandler)
        {
        }

        public void onDeviceConnected(FooGattHandler gattHandler, long elapsedMillis)
        {
        }

        public enum DisconnectReason
        {
            ConnectFailed,
            SolicitedDisconnect,
            SolicitedDisconnectTimeout,
            UnsolicitedDisconnect,
        }

        /**
         * @param gattHandler   gattHandler
         * @param status        same as status in {@link android.bluetooth.BluetoothGattCallback#onConnectionStateChange(BluetoothGatt,
         *                      int, int)}, or -1 if unknown
         * @param reason        reason
         * @param elapsedMillis elapsedMillis
         */
        public void onDeviceDisconnected(FooGattHandler gattHandler, int status, DisconnectReason reason, long elapsedMillis)
        {
        }

        public void onDeviceServicesDiscovered(FooGattHandler gattHandler, List<BluetoothGattService> services, boolean success, long elapsedMillis)
        {
        }

        public void onDeviceCharacteristicRead(FooGattHandler gattHandler, BluetoothGattCharacteristic characteristic, boolean success, long elapsedMillis)
        {
        }

        public void onDeviceCharacteristicWrite(FooGattHandler gattHandler, BluetoothGattCharacteristic characteristic, boolean success, long elapsedMillis)
        {
        }

        public void onDeviceCharacteristicSetNotification(FooGattHandler gattHandler, BluetoothGattCharacteristic characteristic, boolean success, long elapsedMillis)
        {
        }

        public void onDeviceCharacteristicChanged(FooGattHandler gattHandler, BluetoothGattCharacteristic characteristic)
        {
        }

        public void onDeviceReadRemoteRssi(FooGattHandler gattHandler, int rssi, boolean success, long elapsedMillis)
        {
        }
    }

    private final FooGattManager                          mGattManager;
    private final Context                                 mContext;
    private final long                                    mDeviceAddressLong;
    private final String                                  mDeviceAddressString;
    /**
     * NOTE: All calls to GattHandlerListener methods should be inside mHandlerMain's Looper thread
     */
    private final FooListenerManager<GattHandlerListener> mListenerManager;
    private final FooHandler                              mHandlerMain;
    /**
     * NOTE(pv): I do not believe that there is any real benefit to using a
     * {@link java.util.concurrent.ScheduledExecutorService} over a background Handler.<br>
     * {@link Handler#post(Runnable)} is arguably more efficient than {@link ScheduledThreadPoolExecutor#submit(Runnable)}.<br>
     * The only operation *scheduled* is a disconnect timeout, which doesn't happen very often.<br>
     * The only real benefit of an ExecutorService is to use a ThreadPool (ex: if GattManager wants to control LOTS of
     * devices).<br>
     * However, <b>posted/submitted Runnables still need to run in serial per device</b>, so this may <b>greatly</b>
     * complicate this class if submitting to a ThreadPool capable of multiple concurrent threads.
     */
    private final FooHandler                              mHandlerBackground;
    private final BluetoothAdapter                        mBluetoothAdapter;
    private final Map<GattOperation, Long>                mStartTimes;
    private final AutoResetEvent                          mBackgroundPendingOperationSignal;
    private final BluetoothGattCallback                   mBackgroundBluetoothGattCallback;

    /**
     * synchronized behind mGattManager
     */
    private BluetoothGatt mBluetoothGatt;
    /**
     * synchronized behind mGattManager
     */
    private boolean       mIsSolicitedDisconnecting;

    //package
    FooGattHandler(FooGattManager gattManager, long deviceAddress)
    {
        if (gattManager == null)
        {
            throw new IllegalArgumentException("gattManager must not be null");
        }

        FooGattUtils.throwExceptionIfInvalidBluetoothAddress(deviceAddress);

        mGattManager = gattManager;

        mContext = gattManager.getContext();

        mDeviceAddressLong = deviceAddress;
        mDeviceAddressString = FooGattUtils.deviceAddressLongToString(deviceAddress);

        mListenerManager = new FooListenerManager<>();

        mHandlerMain = new FooHandler(mGattManager.getLooper(), new Handler.Callback()
        {
            @Override
            public boolean handleMessage(Message msg)
            {
                return FooGattHandler.this.handleMessage(msg);
            }
        });

        FooHandlerThread handlerThreadBackground = new FooHandlerThread(
                "\"" + mDeviceAddressString + "\".mHandlerBackground");
        handlerThreadBackground.start();
        Looper looperBackground = handlerThreadBackground.getLooper();
        mHandlerBackground = new FooHandler(looperBackground);

        mBluetoothAdapter = FooBluetoothUtils.getBluetoothAdapter(mContext);

        mStartTimes = new HashMap<>();

        mBackgroundPendingOperationSignal = new AutoResetEvent();

        mBackgroundBluetoothGattCallback = new BluetoothGattCallback()
        {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                FooGattHandler.this.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                FooGattHandler.this.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                FooGattHandler.this.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                FooGattHandler.this.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
                FooGattHandler.this.onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
                FooGattHandler.this.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
            {
                FooGattHandler.this.onReadRemoteRssi(gatt, rssi, status);
            }
        };
    }

    //
    //
    //

    public BluetoothAdapter getBluetoothAdapter()
    {
        return mBluetoothAdapter;
    }

    public boolean isBluetoothAdapterEnabled(String callerName)
    {
        if (mBluetoothAdapter == null)
        {
            FooLog.w(TAG, logPrefix(callerName + ": mBluetoothAdapter == null; ignoring"));
            return false;
        }

        if (!mBluetoothAdapter.isEnabled())
        {
            FooLog.w(TAG, logPrefix(callerName + ": mBluetoothAdapter.isEnabled() == false; ignoring"));
            return false;
        }

        return true;
    }

    public long getDeviceAddressLong()
    {
        return mDeviceAddressLong;
    }

    public String getDeviceAddressString()
    {
        return mDeviceAddressString;
    }

    public BluetoothDevice getBluetoothDevice()
    {
        BluetoothGatt gatt = getBluetoothGatt(true);
        return gatt != null ? gatt.getDevice() : null;
    }

    public void close()
    {
        close(true);
    }

    //package
    void close(boolean remove)
    {
        synchronized (mGattManager)
        {
            if (remove)
            {
                mGattManager.removeGattHandler(this);
            }

            disconnect();
        }
    }

    private String logPrefix(String message)
    {
        return mDeviceAddressString + " " + message;
    }

    private boolean isBackgroundThread()
    {
        return mHandlerBackground.getLooper() == Looper.myLooper();
    }

    private boolean isConnectingOrConnectedAndNotDisconnecting(String callerName)
    {
        //FooLog.e(TAG, "isConnectingOrConnectedAndNotDisconnecting(callerName=" + callerName + ')');
        if (getBluetoothGatt(true) == null)
        {
            return false;
        }

        FooLog.w(TAG, logPrefix(callerName + ": isConnectingOrConnectedAndNotDisconnecting(...) == true; ignoring"));
        return true;
    }

    private boolean isDisconnectingOrDisconnected(String callerName)
    {
        //FooLog.e(TAG, "isConnectingOrConnectedAndNotDisconnecting(callerName=" + callerName + ')');
        if (getBluetoothGatt(false) != null)
        {
            return false;
        }

        FooLog.w(TAG, logPrefix(callerName + ": isDisconnectingOrDisconnected(...) == true; ignoring"));
        return true;
    }

    private BluetoothGatt getBluetoothGatt(boolean onlyIfConnectingOrConnectedAndNotDisconnecting)
    {
        //FooLog.e(TAG, "getBluetoothGatt(onlyIfConnectingOrConnectedAndNotDisconnecting=" +
        //             onlyIfConnectingOrConnectedAndNotDisconnecting + ')');
        synchronized (mGattManager)
        {
            //FooLog.e(TAG, "getBluetoothGatt: mBluetoothGatt=" + mBluetoothGatt);
            BluetoothGatt gatt = mBluetoothGatt;
            if (gatt != null)
            {
                //FooLog.e(TAG, "getBluetoothGatt: mIsSolicitedDisconnecting=" + mIsSolicitedDisconnecting);
                if (onlyIfConnectingOrConnectedAndNotDisconnecting && mIsSolicitedDisconnecting)
                {
                    gatt = null;
                }
            }
            return gatt;
        }
    }

    //
    //
    //

    public void addListener(GattHandlerListener listener)
    {
        mListenerManager.attach(listener);
    }

    public void removeListener(GattHandlerListener listener)
    {
        mListenerManager.detach(listener);
    }

    public void clearListeners()
    {
        mListenerManager.clear();
    }

    private long timerStart(GattOperation operation)
    {
        //FooLog.e(TAG, logPrefix("timerStart(operation=" + operation + ')'));
        long startTimeMillis = System.currentTimeMillis();
        //FooLog.e(TAG, logPrefix("timerStart: startTimeMillis=" + startTimeMillis));
        mStartTimes.put(operation, startTimeMillis);
        return startTimeMillis;
    }

    private long timerElapsed(GattOperation operation, boolean remove)
    {
        //FooLog.e(TAG, logPrefix("timerElapsed(operation=" + operation + ", remove=" + remove + ')'));
        long elapsedMillis = -1;
        Long startTimeMillis;
        if (remove)
        {
            startTimeMillis = mStartTimes.remove(operation);
        }
        else
        {
            startTimeMillis = mStartTimes.get(operation);
        }
        //FooLog.e(TAG, logPrefix("timerElapsed: startTimeMillis=" + startTimeMillis));
        if (startTimeMillis != null)
        {
            elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        }
        //FooLog.e(TAG, logPrefix("timerElapsed: elapsedMillis=" + elapsedMillis));
        return elapsedMillis;
    }

    //
    //
    //

    /**
     * @return true if the connect request was enqueued, otherwise false
     */
    public boolean connect()
    {
        return connect(sDefaultTimeoutMillis);
    }

    /**
     * @param timeoutMillis timeoutMillis
     * @return true if the connect request was enqueued, otherwise false
     */
    public boolean connect(long timeoutMillis)
    {
        return connect(false, timeoutMillis);
    }

    /**
     * @param autoConnect autoConnect
     * @return true if the connect request was enqueued, otherwise false
     */
    public boolean connect(boolean autoConnect)
    {
        return connect(autoConnect, sDefaultTimeoutMillis);
    }

    /**
     * @param autoConnect   autoConnect
     * @param timeoutMillis timeoutMillis
     * @return true if already connected and *NOT* disconnecting, or the connect request was enqueued, otherwise false
     */
    public boolean connect(final boolean autoConnect, final long timeoutMillis)
    {
        FooLog.i(TAG, logPrefix("connect(autoConnect=" + autoConnect +
                                ", timeoutMillis=" + timeoutMillis + ')'));

        if (!isBluetoothAdapterEnabled("connect"))
        {
            return false;
        }

        if (isConnectingOrConnectedAndNotDisconnecting("connect"))
        {
            return false;
        }

        final GattOperation operation = GattOperation.Connect;

        final long startTimeMillis = timerStart(operation);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                try
                {
                    FooLog.v(TAG, logPrefix("+connect.run(): autoConnect=" + autoConnect +
                                            ", timeoutMillis=" + timeoutMillis));

                    if (isConnectingOrConnectedAndNotDisconnecting("connect.run"))
                    {
                        return;
                    }

                    BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddressString);

                    onDeviceConnecting();

                    //
                    // NOTE: Some Gatt operations, especially connecting, can take "up to" (meaning "over") 30 seconds, per:
                    //  http://stackoverflow.com/a/18889509/252308
                    //  http://e2e.ti.com/support/wireless_connectivity/f/538/p/281081/985950#985950
                    //

                    mBackgroundPendingOperationSignal.reset(startTimeMillis);

                    synchronized (mGattManager)
                    {
                        FooLog.v(TAG, logPrefix("connect.run: +bluetoothDevice.connectGatt(...)"));
                        // Only set here and in #onDeviceDisconnected
                        mBluetoothGatt = bluetoothDevice.connectGatt(mContext, autoConnect, mBackgroundBluetoothGattCallback);
                        FooLog.v(TAG, logPrefix("connect.run: -bluetoothDevice.connectGatt(...) returned " +
                                                mBluetoothGatt));
                    }

                    if (mBluetoothGatt == null)
                    {
                        FooLog.w(TAG, logPrefix("connect.run: bluetoothDevice.connectGatt(...) failed"));
                        onDeviceDisconnected(null, -1, DisconnectReason.ConnectFailed, false);
                    }
                    else
                    {
                        pendingOperationWait(operation, timeoutMillis);
                    }
                }
                finally
                {
                    FooLog.v(TAG, logPrefix("-connect.run(): autoConnect=" + autoConnect +
                                            ", timeoutMillis=" + timeoutMillis));
                }
            }
        });

        return true;
    }

    public boolean disconnect()
    {
        return disconnect(sDefaultDisconnectTimeoutMillis);
    }

    public boolean disconnect(final long timeoutMillis)
    {
        try
        {
            FooLog.i(TAG, logPrefix("+disconnect(timeoutMillis=" + timeoutMillis + ')'));

            synchronized (mGattManager)
            {
                if (mBluetoothGatt == null)
                {
                    FooLog.w(TAG, logPrefix("disconnect: mBluetoothGatt == null; ignoring"));
                    return false;
                }

                if (mIsSolicitedDisconnecting)
                {
                    FooLog.w(TAG, logPrefix("disconnect: mIsDisconnecting == true; ignoring"));
                    return false;
                }

                //
                // To be safe, always disconnect from the same mHandlerBackground thread that the connection was made on
                //
                if (!isBackgroundThread())
                {
                    FooLog.v(TAG, logPrefix("disconnect: isBackgroundThread() == false; posting disconnect() to mHandlerBackground;"));
                    mHandlerBackground.post(new Runnable()
                    {
                        public void run()
                        {
                            disconnect(timeoutMillis);
                        }
                    });
                    return false;
                }

                mIsSolicitedDisconnecting = true;

                mBackgroundPendingOperationSignal.cancel();

                if (FooGattUtils.safeDisconnect("disconnect(timeoutMillis=" + timeoutMillis + ')', mBluetoothGatt))
                {
                    //
                    // Timeout is needed since BluetoothGatt#disconnect() doesn't always call onConnectionStateChange(..., newState=STATE_DISCONNECTED)
                    //
                    mHandlerMain.sendEmptyMessageDelayed(HandlerMainMessages.SolicitedDisconnectInternalTimeout, timeoutMillis);
                }
                else
                {
                    onDeviceDisconnected(mBluetoothGatt, -1, DisconnectReason.SolicitedDisconnect, true);
                }
            }

            return true;
        }
        finally
        {
            FooLog.i(TAG, logPrefix("-disconnect(timeoutMillis=" + timeoutMillis + ')'));
        }
    }

    /**
     * Consolidates logic for solicited connect failed, solicited disconnect success, solicited disconnect timeout, and
     * unsolicited disconnect.
     *
     * @param gatt              gatt
     * @param status            status
     * @param reason            reason
     * @param logStatusAndState logStatusAndState
     */
    protected void onDeviceDisconnected(BluetoothGatt gatt,
                                        final int status,
                                        final DisconnectReason reason,
                                        boolean logStatusAndState)
    {
        FooLog.i(TAG, logPrefix("onDeviceDisconnected(gatt, status=" + status +
                                ", reason=" + reason +
                                ", logStatusAndState=" + logStatusAndState + ')'));

        synchronized (mGattManager)
        {
            if (mBluetoothGatt == null)
            {
                FooLog.w(TAG, logPrefix("onDeviceDisconnected: mBluetoothGatt == null; ignoring"));
                return;
            }

            // Only set here and in #connect
            mBluetoothGatt = null;

            final int elapsedMillis = (int) timerElapsed(GattOperation.Connect, true);

            mBackgroundPendingOperationSignal.cancel();

            mHandlerMain.removeMessages(HandlerMainMessages.SolicitedDisconnectInternalTimeout);

            mStartTimes.clear();

            if (logStatusAndState)
            {
                logStatusIfNotSuccess("onDeviceDisconnected", status, null);
            }

            mIsWaitingForCharacteristicSetNotification = false;

            // Only set here and in #disconnect
            mIsSolicitedDisconnecting = false;

            FooGattUtils.safeClose("onDeviceDisconnected", gatt);

            mHandlerMain.post(new Runnable()
            {
                @Override
                public void run()
                {
                    FooLog.d(TAG, logPrefix("onDeviceDisconnected: +deviceListener(s).onDeviceDisconnected"));
                    for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                    {
                        mListenerManager.detach(deviceListener);
                        deviceListener.onDeviceDisconnected(FooGattHandler.this,
                                status,
                                reason,
                                elapsedMillis);
                    }
                    mListenerManager.endTraversing();
                    FooLog.d(TAG, logPrefix("onDeviceDisconnected: -deviceListener(s).onDeviceDisconnected"));
                }
            });
        }
    }

    /**
     * NOTE: Some status codes can be found at…
     * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
     * ...not that they are very descriptive or helpful or anything like that! :/
     *
     * @param callerName callerName
     * @param status     status
     * @param text       text
     */
    private void logStatusIfNotSuccess(String callerName, int status, String text)
    {
        String message;

        switch (status)
        {
            case BluetoothGatt.GATT_SUCCESS:
                // ignore
                return;
            case 133:
                //
                // https://code.google.com/p/android/issues/detail?id=58381
                // Too many device connections? (hard coded limit of ~4-7ish?)
                // NOTE:(pv) This problem can supposedly be sometimes induced by not calling "gatt.close()" when disconnecting
                //
                message = "Got the status 133 bug (too many connections?); see https://code.google.com/p/android/issues/detail?id=58381";
                break;
            case 257:
                //
                // https://code.google.com/p/android/issues/detail?id=183108
                // NOTE:(pv) This problem can supposedly be sometimes induced by calling "gatt.close()" before "onConnectionStateChange" is called by "gatt.disconnect()"
                //
                message = "Got the status 257 bug (disconnect()->close()); see https://code.google.com/p/android/issues/detail?id=183108";
                break;
            default:
                message = "error status=" + status;
                break;
        }

        if (!FooString.isNullOrEmpty(text))
        {
            message += ' ' + text;
        }

        FooLog.e(TAG, logPrefix(callerName + ": " + message));
    }

    /**
     * NOTE: Some status codes can be found at…
     * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
     * ...not that they are very descriptive or helpful or anything like that! :/
     * <p/>
     * See {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)}
     *
     * @param gatt     gatt
     * @param status   status
     * @param newState newState
     */
    private void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
    {
        String newStateString = FooGattUtils.bluetoothProfileStateToString(newState);

        FooLog.v(TAG, logPrefix("onConnectionStateChange(gatt, status=" + status +
                                ", newState=" + newStateString + ')'));

        //noinspection UnusedAssignment
        final int DEBUG_FAKE_STATUS_ERROR = BluetoothGatt.GATT_SUCCESS;
        //noinspection ConstantConditions
        if (BuildConfig.DEBUG && DEBUG_FAKE_STATUS_ERROR != BluetoothGatt.GATT_SUCCESS)
        {
            status = DEBUG_FAKE_STATUS_ERROR;
            FooLog.e(TAG, logPrefix("onConnectionStateChange: ***FAKE*** STATUS " + status + " ERROR"));
        }
        else if (status != BluetoothGatt.GATT_SUCCESS)
        {
            FooLog.e(TAG, logPrefix("onConnectionStateChange: ***REAL*** STATUS " + status + " ERROR"));
        }

        boolean disconnect;
        boolean logStatus = false;

        if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS)
        {
            onDeviceConnected();

            timerStart(GattOperation.DiscoverServices);

            if (!gatt.discoverServices())
            {
                FooLog.e(TAG, logPrefix("onConnectionStateChange: gatt.discoverServices() failed; disconnecting…"));
                disconnect = true;
            }
            else
            {
                disconnect = false;
            }
        }
        else
        {
            disconnect = true;
            logStatus = true;

            if (newState != BluetoothProfile.STATE_DISCONNECTED)
            {
                FooLog.e(TAG, logPrefix("onConnectionStateChange: UNEXPECTED newState=" + newStateString +
                                        ", status=" + status +
                                        "; disconnecting…"));
            }
        }

        FooLog.d(TAG, logPrefix("onConnectionStateChange: disconnect=" + disconnect));
        if (disconnect)
        {
            synchronized (mGattManager)
            {
                DisconnectReason reason = mIsSolicitedDisconnecting ?
                        DisconnectReason.SolicitedDisconnect : DisconnectReason.UnsolicitedDisconnect;
                onDeviceDisconnected(gatt, status, reason, logStatus);
            }
        }
    }

    private void onDeviceOperationTimeout(final GattOperation gattOperation,
                                          final long timeoutMillis,
                                          final long elapsedMillis)
    {
        mHandlerMain.post(new Runnable()
        {
            @Override
            public void run()
            {
                boolean disconnect = true;

                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    if (deviceListener.onDeviceOperationTimeout(FooGattHandler.this,
                            gattOperation,
                            timeoutMillis,
                            elapsedMillis))
                    {
                        disconnect = false;
                    }
                }
                mListenerManager.endTraversing();

                if (disconnect)
                {
                    disconnect();
                }
            }
        });
    }

    private void onDeviceConnecting()
    {
        mHandlerMain.post(new Runnable()
        {
            @Override
            public void run()
            {
                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceConnecting(FooGattHandler.this);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    private void onDeviceConnected()
    {
        final long elapsedMillis = timerElapsed(GattOperation.Connect, false);

        mHandlerMain.post(new Runnable()
        {
            @Override
            public void run()
            {
                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceConnected(FooGattHandler.this,
                            elapsedMillis);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    private void onServicesDiscovered(BluetoothGatt gatt, int status)
    {
        FooLog.v(TAG, logPrefix("onServicesDiscovered(gatt, status=" + status + ')'));

        int elapsedMillis = (int) timerElapsed(GattOperation.DiscoverServices, true);

        logStatusIfNotSuccess("onServicesDiscovered", status, null);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        List<BluetoothGattService> services;

        if (!success)
        {
            services = null;
        }
        else
        {
            services = gatt.getServices();
        }

        onDeviceServicesDiscovered(services, success, elapsedMillis);
    }

    protected void onDeviceServicesDiscovered(final List<BluetoothGattService> services,
                                              final boolean success,
                                              final long elapsedMillis)
    {
        pendingOperationWaitSignal();

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceServicesDiscovered(FooGattHandler.this,
                            services,
                            success,
                            elapsedMillis);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    //
    //
    //

    public boolean characteristicRead(UUID serviceUuid, UUID characteristicUuid)
    {
        return characteristicRead(serviceUuid, characteristicUuid, sDefaultTimeoutMillis);
    }

    public boolean characteristicRead(final UUID serviceUuid, final UUID characteristicUuid,
                                      final long timeoutMillis)
    {
        FooLog.i(TAG, logPrefix("characteristicRead(serviceUuid=" + serviceUuid +
                                ", characteristicUuid=" + characteristicUuid +
                                ", timeoutMillis=" + timeoutMillis + ')'));

        if (serviceUuid == null)
        {
            throw new IllegalArgumentException("serviceUuid must not be null");
        }

        if (characteristicUuid == null)
        {
            throw new IllegalArgumentException("characteristicUuid must not be null");
        }

        if (!isBluetoothAdapterEnabled("characteristicRead"))
        {
            return false;
        }

        if (isDisconnectingOrDisconnected("characteristicRead"))
        {
            return false;
        }

        final GattOperation operation = GattOperation.CharacteristicRead;

        final long startTimeMillis = timerStart(operation);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                try
                {
                    FooLog.v(TAG, logPrefix("+characteristicRead.run(): serviceUuid=" + serviceUuid +
                                            ", characteristicUuid=" + characteristicUuid +
                                            ", timeoutMillis=" + timeoutMillis));

                    BluetoothGatt gatt = pendingOperationWaitReset("characteristicRead", startTimeMillis);
                    if (gatt == null)
                    {
                        onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    BluetoothGattService service = gatt.getService(serviceUuid);
                    if (service == null)
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicRead: gatt.getService(" +
                                serviceUuid + ") failed"));
                        onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
                    if (characteristic == null)
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicRead: service.getCharacteristic(" +
                                characteristicUuid + ") failed"));
                        onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    if (!gatt.readCharacteristic(characteristic))
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicRead: gatt.characteristicRead(...) failed for characteristic " +
                                characteristicUuid));
                        onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    pendingOperationWait(operation, timeoutMillis);
                }
                finally
                {
                    FooLog.v(TAG, logPrefix("-characteristicRead.run(): serviceUuid=" + serviceUuid +
                                            ", characteristicUuid=" + characteristicUuid +
                                            ", timeoutMillis=" + timeoutMillis));
                }
            }
        });

        return true;
    }

    private void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        UUID characteristicUuid = characteristic.getUuid();
        FooLog.v(TAG, logPrefix("onCharacteristicRead(gatt, characteristic=" + characteristicUuid +
                                ", status=" + status + ')'));

        logStatusIfNotSuccess("onCharacteristicRead", status, "for characteristic " + characteristicUuid);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        onDeviceCharacteristicRead(characteristic, success);
    }

    private void onDeviceCharacteristicRead(UUID serviceUuid, UUID characteristicUuid,
                                            boolean success)
    {
        BluetoothGattCharacteristic characteristic = FooGattUtils.createBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        onDeviceCharacteristicRead(characteristic, success);
    }

    private void onDeviceCharacteristicRead(final BluetoothGattCharacteristic characteristic,
                                            final boolean success)
    {
        pendingOperationWaitSignal();

        final long elapsedMillis = timerElapsed(GattOperation.CharacteristicRead, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceCharacteristicRead(FooGattHandler.this,
                            characteristic,
                            success,
                            elapsedMillis);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    //
    //
    //

    public enum CharacteristicWriteType
    {
        /**
         * Results in writing a {@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT}
         */
        DefaultWithResponse,
        /**
         * Results in writing a {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE}
         */
        WithoutResponse,
        /**
         * Results in writing a {@link BluetoothGattCharacteristic#WRITE_TYPE_SIGNED}
         */
        Signed,
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param value              value
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       String value)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, null);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param value                   value
     * @param characteristicWriteType null to ignore
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       String value,
                                       CharacteristicWriteType characteristicWriteType)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param value              value
     * @param timeoutMillis      timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       String value,
                                       long timeoutMillis)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, null, timeoutMillis);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param value                   value
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis           timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       String value,
                                       CharacteristicWriteType characteristicWriteType,
                                       long timeoutMillis)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, FooGattUtils.toBytes(value), characteristicWriteType, timeoutMillis);
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param value              value
     * @param formatType         formatType
     * @param offset             offset
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int value, int formatType, int offset)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, formatType, offset, null);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param value                   value
     * @param formatType              formatType
     * @param offset                  offset
     * @param characteristicWriteType null to ignore
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int value, int formatType, int offset,
                                       CharacteristicWriteType characteristicWriteType)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, formatType, offset, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param value              value
     * @param formatType         formatType
     * @param offset             offset
     * @param timeoutMillis      timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int value, int formatType, int offset,
                                       long timeoutMillis)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, formatType, offset, null, timeoutMillis);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param value                   value
     * @param formatType              formatType
     * @param offset                  offset
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis           timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int value, int formatType, int offset,
                                       CharacteristicWriteType characteristicWriteType,
                                       long timeoutMillis)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, FooGattUtils.toBytes(value, formatType, offset), characteristicWriteType, timeoutMillis);
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param mantissa           mantissa
     * @param exponent           exponent
     * @param formatType         formatType
     * @param offset             offset
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int mantissa, int exponent, int formatType, int offset)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, mantissa, exponent, formatType, offset, null);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param mantissa                mantissa
     * @param exponent                exponent
     * @param formatType              formatType
     * @param offset                  offset
     * @param characteristicWriteType null to ignore
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int mantissa, int exponent, int formatType, int offset,
                                       CharacteristicWriteType characteristicWriteType)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, mantissa, exponent, formatType, offset, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param mantissa           mantissa
     * @param exponent           exponent
     * @param formatType         formatType
     * @param offset             offset
     * @param timeoutMillis      timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int mantissa, int exponent, int formatType, int offset,
                                       long timeoutMillis)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, mantissa, exponent, formatType, offset, null, timeoutMillis);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param mantissa                mantissa
     * @param exponent                exponent
     * @param formatType              formatType
     * @param offset                  offset
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis           timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       int mantissa, int exponent, int formatType, int offset,
                                       CharacteristicWriteType characteristicWriteType,
                                       long timeoutMillis)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, FooGattUtils.toBytes(mantissa, exponent, formatType, offset), characteristicWriteType, timeoutMillis);
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param value              value
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       byte[] value)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, null);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param value                   value
     * @param characteristicWriteType null to ignore
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       byte[] value,
                                       CharacteristicWriteType characteristicWriteType)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid        serviceUuid
     * @param characteristicUuid characteristicUuid
     * @param value              value
     * @param timeoutMillis      timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                       byte[] value,
                                       long timeoutMillis)
    {
        return characteristicWrite(serviceUuid, characteristicUuid, value, null, timeoutMillis);
    }

    /**
     * @param serviceUuid             serviceUuid
     * @param characteristicUuid      characteristicUuid
     * @param value                   value
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis           timeoutMillis
     * @return true if the request was enqueued, otherwise false
     */
    public boolean characteristicWrite(final UUID serviceUuid, final UUID characteristicUuid,
                                       final byte[] value,
                                       final CharacteristicWriteType characteristicWriteType,
                                       final long timeoutMillis)
    {
        FooLog.i(TAG, logPrefix("characteristicWrite(serviceUuid=" + serviceUuid +
                                ", characteristicUuid=" + characteristicUuid +
                                ", value=" + Arrays.toString(value) +
                                ", characteristicWriteType=" + characteristicWriteType +
                                ", timeoutMillis=" + timeoutMillis + ')'));

        if (serviceUuid == null)
        {
            throw new IllegalArgumentException("serviceUuid must not be null");
        }

        if (characteristicUuid == null)
        {
            throw new IllegalArgumentException("characteristicUuid must not be null");
        }

        if (value == null)
        {
            throw new IllegalArgumentException("value must not be null");
        }

        if (!isBluetoothAdapterEnabled("characteristicWrite"))
        {
            return false;
        }

        if (isDisconnectingOrDisconnected("characteristicWrite"))
        {
            return false;
        }

        final GattOperation operation = GattOperation.CharacteristicWrite;

        final long startTimeMillis = timerStart(operation);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                try
                {
                    FooLog.v(TAG, logPrefix("+characteristicWrite.run(): serviceUuid=" + serviceUuid +
                                            ", characteristicUuid=" + characteristicUuid +
                                            ", value=" + Arrays.toString(value) +
                                            ", characteristicWriteType=" + characteristicWriteType +
                                            ", timeoutMillis=" + timeoutMillis));

                    BluetoothGatt gatt = pendingOperationWaitReset("characteristicWrite", startTimeMillis);
                    if (gatt == null)
                    {
                        onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    BluetoothGattService service = gatt.getService(serviceUuid);
                    if (service == null)
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicWrite: gatt.getService(" +
                                serviceUuid + ") failed"));
                        onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
                    if (characteristic == null)
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicWrite: service.getCharacteristic(" +
                                characteristicUuid + ") failed"));
                        onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    if (characteristicWriteType != null)
                    {
                        int writeType;
                        switch (characteristicWriteType)
                        {
                            case WithoutResponse:
                                writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
                                break;
                            case Signed:
                                writeType = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;
                                break;
                            case DefaultWithResponse:
                            default:
                                writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
                                break;
                        }
                        characteristic.setWriteType(writeType);
                    }

                    if (!characteristic.setValue(value))
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicWrite: characteristic.setValue(" + Arrays.toString(value) +
                                " failed for characteristic " + characteristicUuid));
                        onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    if (!gatt.writeCharacteristic(characteristic))
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicWrite: gatt.characteristicWrite(...) failed for characteristic " +
                                characteristicUuid));
                        onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    pendingOperationWait(operation, timeoutMillis);
                }
                finally
                {
                    FooLog.v(TAG, logPrefix("-characteristicWrite.run(): serviceUuid=" + serviceUuid +
                                            ", characteristicUuid=" + characteristicUuid +
                                            ", value=" + Arrays.toString(value) +
                                            ", characteristicWriteType=" + characteristicWriteType +
                                            ", timeoutMillis=" + timeoutMillis));
                }
            }
        });

        return true;
    }

    private void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        UUID characteristicUuid = characteristic.getUuid();
        FooLog.v(TAG, logPrefix("onCharacteristicWrite(gatt, characteristic=" + characteristicUuid +
                                ", status=" + status + ')'));

        logStatusIfNotSuccess("onCharacteristicWrite", status, "for characteristic " + characteristicUuid);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        onDeviceCharacteristicWrite(characteristic, success);
    }

    private void onDeviceCharacteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                             boolean success)
    {
        BluetoothGattCharacteristic characteristic = FooGattUtils.createBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        onDeviceCharacteristicWrite(characteristic, success);
    }

    private void onDeviceCharacteristicWrite(final BluetoothGattCharacteristic characteristic,
                                             final boolean success)
    {
        pendingOperationWaitSignal();

        final long elapsedMillis = timerElapsed(GattOperation.CharacteristicWrite, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceCharacteristicWrite(FooGattHandler.this,
                            characteristic,
                            success,
                            elapsedMillis);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    //
    //
    //

    public enum CharacteristicNotificationDescriptorType
    {
        /**
         * Results in writing a {@link BluetoothGattDescriptor#DISABLE_NOTIFICATION_VALUE}
         */
        Disable,
        /**
         * Results in writing a {@link BluetoothGattDescriptor#ENABLE_NOTIFICATION_VALUE}
         */
        EnableWithoutResponse,
        /**
         * Results in writing a {@link BluetoothGattDescriptor#ENABLE_INDICATION_VALUE}
         */
        EnableWithResponse,
    }

    public boolean characteristicSetNotification(UUID serviceUuid, UUID characteristicUuid,
                                                 CharacteristicNotificationDescriptorType characteristicNotificationDescriptorType)
    {
        return characteristicSetNotification(serviceUuid, characteristicUuid, characteristicNotificationDescriptorType, true);
    }

    public boolean characteristicSetNotification(UUID serviceUuid, UUID characteristicUuid,
                                                 CharacteristicNotificationDescriptorType characteristicNotificationDescriptorType,
                                                 boolean setDescriptorClientCharacteristicConfig)
    {
        return characteristicSetNotification(serviceUuid, characteristicUuid, characteristicNotificationDescriptorType, setDescriptorClientCharacteristicConfig, sDefaultTimeoutMillis);
    }

    public boolean characteristicSetNotification(final UUID serviceUuid, final UUID characteristicUuid,
                                                 final CharacteristicNotificationDescriptorType characteristicNotificationDescriptorType,
                                                 final boolean setDescriptorClientCharacteristicConfig,
                                                 final long timeoutMillis)
    {
        FooLog.i(TAG, logPrefix("characteristicSetNotification(serviceUuid=" + serviceUuid +
                                ", characteristicUuid=" + characteristicUuid +
                                ", characteristicNotificationDescriptorType=" +
                                characteristicNotificationDescriptorType +
                                ", setDescriptorClientCharacteristicConfig=" + setDescriptorClientCharacteristicConfig +
                                ", timeoutMillis=" + timeoutMillis + ')'));

        if (serviceUuid == null)
        {
            throw new IllegalArgumentException("serviceUuid must not be null");
        }

        if (characteristicUuid == null)
        {
            throw new IllegalArgumentException("characteristicUuid must not be null");
        }

        if (characteristicNotificationDescriptorType == null)
        {
            throw new IllegalArgumentException("characteristicNotificationDescriptorType must not be null");
        }

        if (!isBluetoothAdapterEnabled("characteristicSetNotification"))
        {
            return false;
        }

        if (isDisconnectingOrDisconnected("characteristicSetNotification"))
        {
            return false;
        }

        final GattOperation operation = GattOperation.CharacteristicSetNotification;

        final long startTimeMillis = timerStart(operation);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                try
                {
                    FooLog.v(TAG, logPrefix("+characteristicSetNotification.run(): serviceUuid=" + serviceUuid +
                                            ", characteristicUuid=" + characteristicUuid +
                                            ", characteristicNotificationDescriptorType=" +
                                            characteristicNotificationDescriptorType +
                                            ", setDescriptorClientCharacteristicConfig=" +
                                            setDescriptorClientCharacteristicConfig +
                                            ", timeoutMillis=" + timeoutMillis));

                    BluetoothGatt gatt = pendingOperationWaitReset("characteristicSetNotification", startTimeMillis);
                    if (gatt == null)
                    {
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    BluetoothGattService service = gatt.getService(serviceUuid);
                    if (service == null)
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicSetNotification: gatt.getService(" +
                                serviceUuid + ") failed"));
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
                    if (characteristic == null)
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicSetNotification: service.getCharacteristic(" +
                                characteristicUuid + ") failed"));
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    boolean enable =
                            characteristicNotificationDescriptorType !=
                            CharacteristicNotificationDescriptorType.Disable;

                    if (!gatt.setCharacteristicNotification(characteristic, enable))
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicSetNotification: mGattConnectingOrConnected.characteristicSetNotification(..., enable=" +
                                enable + ") failed for characteristic " + characteristicUuid));
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    if (!setDescriptorClientCharacteristicConfig)
                    {
                        //
                        // Success
                        //
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (descriptor == null)
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicSetNotification: characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG" +
                                ") failed for characteristic " + characteristicUuid));
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    byte[] descriptorValue;
                    switch (characteristicNotificationDescriptorType)
                    {
                        case EnableWithoutResponse:
                            descriptorValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                            break;
                        case EnableWithResponse:
                            descriptorValue = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                            break;
                        case Disable:
                        default:
                            descriptorValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                            break;
                    }

                    if (!descriptor.setValue(descriptorValue))
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicSetNotification: descriptor.setValue(" +
                                Arrays.toString(descriptorValue) +
                                ") failed for descriptor CLIENT_CHARACTERISTIC_CONFIG for characteristic " +
                                characteristicUuid));
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    if (!gatt.writeDescriptor(descriptor))
                    {
                        FooLog.e(TAG, logPrefix(
                                "characteristicSetNotification: mGattConnectingOrConnected.writeDescriptor(...) failed descriptor CLIENT_CHARACTERISTIC_CONFIG"));
                        onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                        return;
                    }

                    mIsWaitingForCharacteristicSetNotification = true;

                    pendingOperationWait(operation, timeoutMillis);
                }
                finally
                {
                    FooLog.v(TAG, logPrefix("-characteristicSetNotification.run(): serviceUuid=" + serviceUuid +
                                            ", characteristicUuid=" + characteristicUuid +
                                            ", characteristicNotificationDescriptorType=" +
                                            characteristicNotificationDescriptorType +
                                            ", setDescriptorClientCharacteristicConfig=" +
                                            setDescriptorClientCharacteristicConfig +
                                            ", timeoutMillis=" + timeoutMillis));
                }
            }
        });

        return true;
    }

    private boolean mIsWaitingForCharacteristicSetNotification;

    private void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    {
        if (!mIsWaitingForCharacteristicSetNotification)
        {
            //
            // ignore
            //
            return;
        }

        mIsWaitingForCharacteristicSetNotification = false;

        FooLog.v(TAG, logPrefix(
                "onDescriptorWrite(gatt, descriptor=CLIENT_CHARACTERISTIC_CONFIG, status=" + status + ')'));

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

        logStatusIfNotSuccess("onDescriptorWrite", status,
                "for descriptor CLIENT_CHARACTERISTIC_CONFIG for characteristic " + characteristic.getUuid());

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        onDeviceCharacteristicSetNotification(characteristic, success);
    }

    private void onDeviceCharacteristicSetNotification(UUID serviceUuid, UUID characteristicUuid,
                                                       boolean success)
    {
        BluetoothGattCharacteristic characteristic = FooGattUtils.createBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        onDeviceCharacteristicSetNotification(characteristic, success);
    }

    private void onDeviceCharacteristicSetNotification(final BluetoothGattCharacteristic characteristic,
                                                       final boolean success)
    {
        pendingOperationWaitSignal();

        final long elapsedMillis = timerElapsed(GattOperation.CharacteristicSetNotification, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceCharacteristicSetNotification(FooGattHandler.this,
                            characteristic,
                            success,
                            elapsedMillis);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    private void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        if (VERBOSE_LOG_CHARACTERISTIC_CHANGE)
        {
            FooLog.v(TAG, logPrefix("onCharacteristicChanged: characteristic=" + characteristic.getUuid()));
        }

        //
        // Handle the case where disconnect has been called, but the OS has queued up lots of characteristic changes
        //
        if (isDisconnectingOrDisconnected("onCharacteristicChanged"))
        {
            return;
        }

        //
        // NOTE:(pv) This method may stream LOTS of data.
        // To avoid excessive memory allocations, this method intentionally deviates from the other methods' uses of
        // "mHandler.post(new Runnable() ...)"
        //
        mHandlerMain.obtainAndSendMessage(HandlerMainMessages.onCharacteristicChanged, characteristic);
    }

    private static abstract class HandlerMainMessages
    {
        /**
         * <ul>
         * <li>msg.arg1: ?</li>
         * <li>msg.arg2: ?</li>
         * <li>msg.obj: ?</li>
         * </li>
         * </ul>
         */
        private static final int SolicitedDisconnectInternalTimeout = 1;
        /**
         * <ul>
         * <li>msg.arg1: ?</li>
         * <li>msg.arg2: ?</li>
         * <li>msg.obj: BluetoothGattCharacteristic</li>
         * </li>
         * </ul>
         */
        private static final int onCharacteristicChanged            = 2;
    }

    private boolean handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case HandlerMainMessages.SolicitedDisconnectInternalTimeout:
            {
                FooLog.v(TAG, logPrefix("handleMessage: SolicitedDisconnectInternalTimeout"));
                onDeviceDisconnected(mBluetoothGatt, -1, DisconnectReason.SolicitedDisconnectTimeout, false);
                break;
            }
            case HandlerMainMessages.onCharacteristicChanged:
            {
                if (isDisconnectingOrDisconnected("handleMessage: onCharacteristicChanged"))
                {
                    return false;
                }

                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) msg.obj;
                if (VERBOSE_LOG_CHARACTERISTIC_CHANGE)
                {
                    FooLog.v(TAG, logPrefix("handleMessage: onCharacteristicChanged characteristic=" +
                                            characteristic.getUuid()));
                }

                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceCharacteristicChanged(FooGattHandler.this, characteristic);
                }
                mListenerManager.endTraversing();

                break;
            }
        }

        return false;
    }

    public boolean readRemoteRssi()
    {
        return readRemoteRssi(sDefaultTimeoutMillis);
    }

    public boolean readRemoteRssi(final long timeoutMillis)
    {
        FooLog.i(TAG, logPrefix("+readRemoteRssi(timeoutMillis=" + timeoutMillis + ')'));

        if (!isBluetoothAdapterEnabled("readRemoteRssi"))
        {
            return false;
        }

        if (isDisconnectingOrDisconnected("readRemoteRssi"))
        {
            return false;
        }

        final GattOperation operation = GattOperation.ReadRemoteRssi;

        final long startTimeMillis = timerStart(operation);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                try
                {
                    FooLog.v(TAG, logPrefix("+readRemoteRssi.run(): timeoutMillis=" + timeoutMillis + ')'));

                    BluetoothGatt gatt = pendingOperationWaitReset("readRemoteRssi", startTimeMillis);
                    if (gatt == null)
                    {
                        onDeviceReadRemoteRssi(-1, false);
                        return;
                    }

                    if (!gatt.readRemoteRssi())
                    {
                        FooLog.e(TAG, logPrefix("readRemoteRssi: gatt.readRemoteRssi() failed"));
                        onDeviceReadRemoteRssi(-1, false);
                        return;
                    }

                    pendingOperationWait(operation, timeoutMillis);
                }
                finally
                {
                    FooLog.v(TAG, logPrefix("-readRemoteRssi.run(): timeoutMillis=" + timeoutMillis + ')'));
                }
            }
        });

        return true;
    }

    private void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
    {
        FooLog.v(TAG, logPrefix("onReadRemoteRssi(gatt, rssi=" + rssi + ", status=" + status + ')'));

        logStatusIfNotSuccess("onReadRemoteRssi", status, ", rssi=" + rssi);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        onDeviceReadRemoteRssi(rssi, success);
    }

    private void onDeviceReadRemoteRssi(final int rssi,
                                        final boolean success)
    {
        pendingOperationWaitSignal();

        final long elapsedMillis = timerElapsed(GattOperation.ReadRemoteRssi, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                for (GattHandlerListener deviceListener : mListenerManager.beginTraversing())
                {
                    deviceListener.onDeviceReadRemoteRssi(FooGattHandler.this,
                            rssi,
                            success,
                            elapsedMillis);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    //
    //
    //

    public static class AutoResetEvent
    {
        private static final String TAG = FooLog.TAG(AutoResetEvent.class);

        public interface AutoResetEventListener
        {
            void onEventSignaledCompleted(long elapsedMillis);

            void onEventTimedOut(long elapsedMillis);

            void onEventCanceledInterrupted(long elapsedMillis);
        }

        private final Object mEvent = new Object();

        private long    mStartTimeMillis;
        private boolean mIsSignaled;

        public AutoResetEvent()
        {
            mStartTimeMillis = -1;
            mIsSignaled = false;
        }

        public void cancel()
        {
            reset(-1);
        }

        public void reset(long startTimeMillis)
        {
            synchronized (mEvent)
            {
                //FooLog.e(TAG, "reset(startTimeMillis=" + startTimeMillis + ')');
                mStartTimeMillis = startTimeMillis;
                mIsSignaled = false;
                mEvent.notify();
            }
        }

        public void signal()
        {
            synchronized (mEvent)
            {
                //FooLog.e(TAG, "set()");
                if (mStartTimeMillis == -1)
                {
                    throw new IllegalStateException("reset(long startTimeMillis) must be called before set()");
                }
                mIsSignaled = true;
                mEvent.notify();
            }
        }

        /**
         * @param timeoutMillis timeoutMillis
         * @return positive elapsed milliseconds if signaled, negative elapsed milliseconds if not signaled
         */
        public long waitOne(long timeoutMillis)
        {
            return waitOne(timeoutMillis, null);
        }

        /**
         * @param timeoutMillis timeoutMillis
         * @param listener      listener
         * @return positive elapsed milliseconds if signaled, negative elapsed milliseconds if not signaled
         */
        public long waitOne(long timeoutMillis, AutoResetEventListener listener)
        {
            synchronized (mEvent)
            {
                try
                {
                    if (mStartTimeMillis == -1)
                    {
                        throw new IllegalStateException("reset(long startTimeMillis) must be called before waitOne(long timeoutMillis)");
                    }

                    long startTimeMillis = mStartTimeMillis;

                    long waitTimeMillis;
                    if (timeoutMillis < 0)
                    {
                        waitTimeMillis = -1;
                    }
                    else
                    {
                        waitTimeMillis = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
                        if (waitTimeMillis < 0)
                        {
                            waitTimeMillis = 0;
                        }
                    }
                    //FooLog.e(TAG, "waitOne: waitTimeMillis=" + waitTimeMillis);

                    if (!mIsSignaled)
                    {
                        try
                        {
                            if (waitTimeMillis == -1)
                            {
                                mEvent.wait();
                            }
                            else
                            {
                                mEvent.wait(waitTimeMillis);
                            }
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                        }
                    }

                    long elapsedMillis = System.currentTimeMillis() - startTimeMillis;

                    if (mIsSignaled && elapsedMillis <= timeoutMillis)
                    {
                        FooLog.v(TAG, "waitOne: elapsedMillis=" + elapsedMillis + "; SIGNALED/COMPLETED");
                        if (listener != null)
                        {
                            listener.onEventSignaledCompleted(elapsedMillis);
                        }
                    }
                    else
                    {
                        if (elapsedMillis >= timeoutMillis)
                        {
                            FooLog.w(TAG, "waitOne: elapsedMillis=" + elapsedMillis + "; *TIMED OUT*");
                            if (listener != null)
                            {
                                listener.onEventTimedOut(elapsedMillis);
                            }
                        }
                        else
                        {
                            FooLog.w(TAG, "waitOne: elapsedMillis=" + elapsedMillis + "; CANCELED/INTERRUPTED");
                            if (listener != null)
                            {
                                listener.onEventCanceledInterrupted(elapsedMillis);
                            }
                        }

                        elapsedMillis = -elapsedMillis;
                    }

                    return elapsedMillis;
                }
                finally
                {
                    mStartTimeMillis = -1;
                    mIsSignaled = false;
                }
            }
        }
    }

    //
    //
    //

    private BluetoothGatt pendingOperationWaitReset(String callerName, long startTimeMillis)
    {
        mBackgroundPendingOperationSignal.reset(startTimeMillis);

        BluetoothGatt gatt = getBluetoothGatt(true);
        if (gatt == null)
        {
            FooLog.w(TAG, logPrefix(callerName + ": getBluetoothGatt(true) == null; ignoring"));
        }
        return gatt;
    }

    private void pendingOperationWaitSignal()
    {
        mBackgroundPendingOperationSignal.signal();
    }

    /**
     * @param operation     operation
     * @param timeoutMillis timeoutMillis
     */
    private void pendingOperationWait(GattOperation operation, long timeoutMillis)
    {
        FooLog.v(TAG, logPrefix("pendingOperationWait: operation=" + operation +
                                ", timeoutMillis=" + timeoutMillis));

        long elapsedMillis = mBackgroundPendingOperationSignal.waitOne(timeoutMillis);

        switch (operation)
        {
            case CharacteristicSetNotification:
                mIsWaitingForCharacteristicSetNotification = false;
                break;
        }

        boolean signaled;
        if (elapsedMillis < 0)
        {
            FooLog.w(TAG, logPrefix("pendingOperationWait: operation=" + operation +
                                    ", elapsedMillis=" + elapsedMillis));
            signaled = false;
            elapsedMillis = -elapsedMillis;
        }
        else
        {
            FooLog.v(TAG, logPrefix("pendingOperationWait: operation=" + operation +
                                    ", elapsedMillis=" + elapsedMillis));
            signaled = true;
        }

        if (signaled && elapsedMillis <= timeoutMillis)
        {
            FooLog.v(TAG, logPrefix("pendingOperationWait: operation=" + operation +
                                    ", elapsedMillis=" + elapsedMillis + "; SIGNALED/COMPLETED"));
        }
        else
        {
            if (elapsedMillis >= timeoutMillis)
            {
                FooLog.w(TAG, logPrefix("pendingOperationWait: operation=" + operation +
                                        ", elapsedMillis=" + elapsedMillis + "; *TIMED OUT*"));
            }
            else
            {
                FooLog.w(TAG, logPrefix("pendingOperationWait: operation=" + operation +
                                        ", elapsedMillis=" + elapsedMillis + "; CANCELED/INTERRUPTED"));
            }
            onDeviceOperationTimeout(operation, timeoutMillis, elapsedMillis);
        }
    }
}
