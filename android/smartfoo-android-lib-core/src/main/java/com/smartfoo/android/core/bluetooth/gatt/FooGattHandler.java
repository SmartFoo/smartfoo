package com.smartfoo.android.core.bluetooth.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.gatt.FooGattHandler.GattHandlerListener.DisconnectReason;
import com.smartfoo.android.core.bluetooth.gatt.FooGattHandler.GattHandlerListener.GattOperation;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooHandler;
import com.smartfoo.android.core.platform.FooHandlerThread;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
     * Various wrappers around {@link BluetoothGattCallback} methods
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
         * @param gattHandler
         * @param operation
         * @param elapsedMillis
         * @return true to forcibly stay connected, false to allow disconnect
         */
        public boolean onDeviceOperationTimeout(FooGattHandler gattHandler, GattOperation operation, long elapsedMillis)
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
         * @param gattHandler
         * @param status        same as status in {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt,
         *                      int, int)}, or -1 if unknown
         * @param reason
         * @param elapsedMillis
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

    @NonNull
    public static String gattDeviceAddressString(BluetoothGatt gatt)
    {
        if (gatt == null)
        {
            return "00:00:00:00:00:00";
        }

        BluetoothDevice bluetoothDevice = gatt.getDevice();
        if (bluetoothDevice == null)
        {
            throw new IllegalStateException("unexpected bluetoothDevice == null");
        }

        String bluetoothDeviceAddressString = bluetoothDevice.getAddress();
        if (FooString.isNullOrEmpty(bluetoothDeviceAddressString))
        {
            throw new IllegalStateException("unexpected bluetoothDevice.getAddress() == null/\"\"");
        }

        return bluetoothDeviceAddressString;
    }

    public static long gattDeviceAddressLong(BluetoothGatt gatt)
    {
        String bluetoothDeviceAddressString = gattDeviceAddressString(gatt);
        return deviceAddressStringToLong(bluetoothDeviceAddressString);
    }

    public static long deviceAddressStringToLong(String deviceAddressString)
    {
        if (FooString.isNullOrEmpty(deviceAddressString))
        {
            throw new IllegalArgumentException("unexpected deviceAddressString == null/\"\"");
        }
        deviceAddressString = deviceAddressString.replace(":", "");
        return Long.parseLong(deviceAddressString, 16);
    }

    @NonNull
    public static String deviceAddressLongToString(long deviceAddressLong)
    {
        //noinspection PointlessBitwiseExpression
        return String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X",
                (byte) ((deviceAddressLong >> 40) & 0xff),
                (byte) ((deviceAddressLong >> 32) & 0xff),
                (byte) ((deviceAddressLong >> 24) & 0xff),
                (byte) ((deviceAddressLong >> 16) & 0xff),
                (byte) ((deviceAddressLong >> 8) & 0xff),
                (byte) ((deviceAddressLong >> 0) & 0xff));
    }

    public static String bluetoothProfileStateToString(int state)
    {
        String text;
        switch (state)
        {
            case BluetoothProfile.STATE_DISCONNECTED:
                text = "STATE_DISCONNECTED";
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                text = "STATE_DISCONNECTING";
                break;
            case BluetoothProfile.STATE_CONNECTING:
                text = "STATE_CONNECTING";
                break;
            case BluetoothProfile.STATE_CONNECTED:
                text = "STATE_CONNECTED";
                break;
            default:
                text = "STATE_UNKNOWN";
                break;
        }
        return text + '(' + state + ')';
    }

    public static void throwExceptionIfInvalidBluetoothAddress(long deviceAddress)
    {
        if (!(deviceAddress != 0 && deviceAddress != -1))
        {
            throw new IllegalArgumentException("deviceAddress invalid");
        }
    }

    public static void safeDisconnectAndClose(BluetoothGatt gatt)
    {
        String mDeviceAddressString = gattDeviceAddressString(gatt);
        FooLog.v(TAG, mDeviceAddressString + " safeDisconnectAndClose(gatt=" + gatt + ')');

        if (gatt == null)
        {
            FooLog.w(TAG, mDeviceAddressString + " safeDisconnectAndClose: gatt == null; ignoring");
            return;
        }

        try
        {
            FooLog.v(TAG, mDeviceAddressString + " safeDisconnectAndClose: gatt.disconnect()");
            gatt.disconnect();
        }
        catch (Exception e)
        {
            FooLog.w(TAG, mDeviceAddressString + " safeDisconnectAndClose: gatt.disconnect() EXCEPTION; ignoring", e);
        }

        try
        {
            FooLog.v(TAG, mDeviceAddressString + " safeDisconnectAndClose: gatt.close()");
            gatt.close();
        }
        catch (Exception e)
        {
            FooLog.w(TAG, mDeviceAddressString + " safeDisconnectAndClose: gatt.close() EXCEPTION; ignoring", e);
        }
    }

    private final FooGattManager           mGattManager;
    private final Context                  mContext;
    private final long                     mDeviceAddressLong;
    private final String                   mDeviceAddressString;
    /**
     * NOTE: All calls to BleGattCallback methods should be inside mHandlerMain's Looper thread
     */
    private final Set<GattHandlerListener> mListeners;
    private final Set<GattHandlerListener> mListenersToAdd;
    private final Set<GattHandlerListener> mListenersToRemove;

    //
    // initialized in #ensureInitialized(): BEGIN
    //
    private FooHandler               mHandlerMain;
    /**
     * NOTE: There is no real benefit to using a {@link java.util.concurrent.ScheduledExecutorService} over a
     * background
     * Handler.<br>
     * {@link Handler#post(Runnable)} is arguably more efficient than {@link ScheduledThreadPoolExecutor#submit(Runnable)}.<br>
     * The only operation *scheduled* is a disconnect timeout, which doesn't happen very often.<br>
     * The only real benefit of an ExecutorService is to use a ThreadPool (ex: if GattManager wants to control LOTS of
     * devices).<br>
     * However, <b>posted/submitted Runnables still need to run in serial per device</b>, so this may <b>greatly</b>
     * complicate this class if submitting to a ThreadPool capable of multiple concurrent threads.
     */
    private FooHandler               mHandlerBackground;
    private BluetoothAdapter         mBluetoothAdapter;
    private Map<GattOperation, Long> mStartTimes;
    private AutoResetEvent           mPendingOperationSignal;
    private BluetoothGattCallback    mBluetoothGattCallback;
    //
    // initialized in #ensureInitialized(): END
    //

    private boolean mIsLoopingListeners;

    private BluetoothGatt mGattConnectingOrConnected;
    private BluetoothGatt mGattSolicitedDisconnecting;

    //package
    FooGattHandler(FooGattManager gattManager, long deviceAddress)
    {
        if (gattManager == null)
        {
            throw new IllegalArgumentException("gattManager must not be null");
        }

        throwExceptionIfInvalidBluetoothAddress(deviceAddress);

        mGattManager = gattManager;

        mContext = gattManager.getContext();

        mDeviceAddressLong = deviceAddress;
        mDeviceAddressString = deviceAddressLongToString(deviceAddress);

        mListeners = new LinkedHashSet<>();
        mListenersToAdd = new LinkedHashSet<>();
        mListenersToRemove = new LinkedHashSet<>();
    }

    private void ensureInitialized()
    {
        if (mHandlerMain != null)
        {
            return;
        }

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

        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mStartTimes = new HashMap<>();

        mPendingOperationSignal = new AutoResetEvent();

        mBluetoothGattCallback = new BluetoothGattCallback()
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

    public long getDeviceAddressLong()
    {
        return mDeviceAddressLong;
    }

    public String getDeviceAddressString()
    {
        return mDeviceAddressString;
    }

    public void close()
    {
        close(true);
    }

    //package
    void close(boolean remove)
    {
        if (remove)
        {
            mGattManager.removeGattHandler(this);
        }

        disconnect();
        // TODO:()pv) clear the listeners after disconnect...
        //clearListeners();
    }

    public BluetoothDevice getBluetoothDevice()
    {
        BluetoothGatt gatt = mGattConnectingOrConnected;
        return gatt != null ? gatt.getDevice() : null;
    }

    private boolean isBackgroundThread()
    {
        return mHandlerBackground != null && mHandlerBackground.getLooper() == Looper.myLooper();
    }

    private boolean ensureConnected(String callerName, long timeoutMillis)
    {
        ensureInitialized();

        if (mGattConnectingOrConnected != null)
        {
            mHandlerMain.removeMessages(Messages.SolicitedDisconnectInternalTimeout);
            return true;
        }

        if (!connect(timeoutMillis))
        {
            FooLog.w(TAG, mDeviceAddressString + ' ' + callerName + ": Failed to connect");
            return false;
        }

        return true;
    }

    public void addListener(GattHandlerListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("listener must not be null");
        }

        synchronized (mListeners)
        {
            Set<GattHandlerListener> listeners;
            if (mIsLoopingListeners)
            {
                listeners = mListenersToAdd;
            }
            else
            {
                listeners = mListeners;
            }

            listeners.add(listener);
            //FooLog.e(TAG, mDeviceAddressString + " addListener: mListeners=" + mListeners);
        }
    }

    public void removeListener(GattHandlerListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("listener must not be null");
        }

        synchronized (mListeners)
        {
            if (mIsLoopingListeners)
            {
                mListenersToRemove.add(listener);
            }
            else
            {
                mListeners.remove(listener);
            }
        }
    }

    public void clearListeners()
    {
        synchronized (mListeners)
        {
            if (mIsLoopingListeners)
            {
                for (GattHandlerListener listener : mListeners)
                {
                    mListenersToRemove.add(listener);
                }
                for (GattHandlerListener listener : mListenersToAdd)
                {
                    mListenersToRemove.add(listener);
                }
            }
            else
            {
                mListeners.clear();
            }
        }
    }

    /**
     * Must be called from *inside* of a <pre>synchronized (mListeners)</pre> block
     */
    private void onLoopingListenersBegin()
    {
        mIsLoopingListeners = true;
    }

    /**
     * Must be called from *inside* of a <pre>synchronized (mListeners)</pre> block
     */
    private void onLoopingListenersEnd()
    {
        mIsLoopingListeners = false;

        Iterator<GattHandlerListener> it;

        it = mListenersToAdd.iterator();
        while (it.hasNext())
        {
            mListeners.add(it.next());
            it.remove();
        }

        it = mListenersToRemove.iterator();
        while (it.hasNext())
        {
            mListeners.remove(it.next());
            it.remove();
        }
    }

    private void timerStart(GattOperation operation)
    {
        mStartTimes.put(operation, System.currentTimeMillis());
    }

    private long timerElapsed(GattOperation operation, boolean remove)
    {
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
        if (startTimeMillis != null)
        {
            elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        }
        return elapsedMillis;
    }

    /**
     * @return true if the connect request was enqueued, otherwise false
     */
    public boolean connect()
    {
        return connect(sDefaultTimeoutMillis);
    }

    /**
     * @param timeoutMillis
     * @return true if the connect request was enqueued, otherwise false
     */
    public boolean connect(long timeoutMillis)
    {
        return connect(false, timeoutMillis);
    }

    /**
     * @param autoConnect
     * @return true if the connect request was enqueued, otherwise false
     */
    public boolean connect(boolean autoConnect)
    {
        return connect(autoConnect, sDefaultTimeoutMillis);
    }

    /**
     * @param autoConnect
     * @param timeoutMillis
     * @return true if the connect request was enqueued, otherwise false
     */
    public boolean connect(final boolean autoConnect, final long timeoutMillis)
    {
        FooLog.i(TAG, mDeviceAddressString +
                      " connect(autoConnect=" + autoConnect + ", timeoutMillis=" + timeoutMillis + ')');

        ensureInitialized();

        if (mBluetoothAdapter == null)
        {
            FooLog.w(TAG, mDeviceAddressString + " connect: BluetoothAdapter is not initialized; ignoring");
            return false;
        }

        if (mGattConnectingOrConnected != null)
        {
            mHandlerMain.removeMessages(Messages.SolicitedDisconnectInternalTimeout);
        }

        timerStart(GattOperation.Connect);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                FooLog.i(TAG, mDeviceAddressString +
                              " +connect.run(): autoConnect=" + autoConnect + ", timeoutMillis=" + timeoutMillis);

                BluetoothGatt gatt = mGattConnectingOrConnected;

                BluetoothDevice bluetoothDevice;
                if (gatt == null)
                {
                    bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddressString);
                }
                else
                {
                    //
                    // Always reset the connection to minimize the chance of status 133 (too many connections) errors.
                    // There is no need to tell any listeners about the disconnect.
                    //
                    safeDisconnectAndClose(gatt);

                    bluetoothDevice = gatt.getDevice();
                }

                onDeviceConnecting();

                //
                // Some Gatt operations, especially connecting, can take "up to" (read "over") 30 seconds, per:
                //  http://stackoverflow.com/a/18889509/252308
                //  http://e2e.ti.com/support/wireless_connectivity/f/538/p/281081/985950#985950
                //

                mPendingOperationSignal.reset();

                FooLog.v(TAG, mDeviceAddressString +
                              " connect.run: +bluetoothDevice.connectGatt(...)");
                gatt = bluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback);
                FooLog.v(TAG, mDeviceAddressString +
                              " connect.run: -bluetoothDevice.connectGatt(...) returned " + gatt);
                if (gatt == null)
                {
                    FooLog.e(TAG, mDeviceAddressString + " connect.run: bluetoothDevice.connectGatt(...) failed");
                    onDeviceDisconnected(null, -1, DisconnectReason.ConnectFailed, false);
                }
                else
                {
                    // Only set here and in #disconnect
                    mGattConnectingOrConnected = gatt;

                    waitForOperationCompletion(GattOperation.Connect, timeoutMillis);
                }

                FooLog.i(TAG, mDeviceAddressString +
                              " -connect.run(): autoConnect=" + autoConnect + ", timeoutMillis=" + timeoutMillis);
            }
        });

        return true;
    }

    public void disconnect()
    {
        disconnect(sDefaultDisconnectTimeoutMillis);
    }

    public void disconnect(long timeoutMillis)
    {
        FooLog.i(TAG, mDeviceAddressString +
                      " disconnect(timeoutMillis=" + timeoutMillis + ')');

        ensureInitialized();

        BluetoothGatt gatt = mGattConnectingOrConnected;

        if (gatt == null)
        {
            FooLog.w(TAG, mDeviceAddressString + " disconnect: Not connected to device; ignoring");
            return;
        }

        // Only set here and in #onDeviceDisconnected
        mGattSolicitedDisconnecting = gatt;

        // Only set here and in #connect
        mGattConnectingOrConnected = null;

        mHandlerMain.removeMessages(Messages.SolicitedDisconnectInternalTimeout);

        //
        // Timeout is needed since onConnectionStateChange(..., newState=STATE_DISCONNECTED) isn't always called
        //
        mHandlerMain.sendEmptyMessageDelayed(Messages.SolicitedDisconnectInternalTimeout,
                timeoutMillis);

        //
        // To be safe, always disconnect from the same mHandlerBackground thread that the connection was made on
        //
        if (isBackgroundThread())
        {
            safeDisconnectAndClose(mGattSolicitedDisconnecting);
        }
        else
        {
            mPendingOperationSignal.reset();

            mHandlerBackground.post(new Runnable()
            {
                public void run()
                {
                    safeDisconnectAndClose(mGattSolicitedDisconnecting);
                }
            });
        }
    }

    private void logStatus(String callerName, int status, String text)
    {
        String message;

        switch (status)
        {
            case BluetoothGatt.GATT_SUCCESS:
                // ignore
                return;
            case 133:
                //
                // Too many device connections (hard coded limit of ~4-7ish?)
                // https://code.google.com/p/android/issues/detail?id=58381
                // NOTE:(pv) This problem can sometimes be induced by not calling "gatt.close()" when disconnecting
                //
                message = "Got the status 133 bug (too many connections?)";
                break;
            default:
                message = "error status=" + status;
                break;
        }

        if (!FooString.isNullOrEmpty(text))
        {
            message += ' ' + text;
        }

        FooLog.e(TAG, mDeviceAddressString + ' ' + callerName + ": " + message);
    }

    /**
     * NOTE: Status codes can be found at...
     * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
     * ...not that they are very descriptive or helpful or anything like that! :/
     * <p/>
     * See {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)}
     */
    private void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
    {
        String newStateString = bluetoothProfileStateToString(newState);

        FooLog.v(TAG, mDeviceAddressString +
                      " onConnectionStateChange(gatt, status=" + status + ", newState=" + newStateString +
                      ')');

        final boolean debugFakeError = false;
        //noinspection ConstantConditions
        if (debugFakeError)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                status = 133;
                FooLog.e(TAG, mDeviceAddressString +
                              " onConnectionStateChange: ****SIMULATING STATUS " + status + " ERROR****");
            }
            else
            {
                FooLog.e(TAG,
                        mDeviceAddressString +
                        " onConnectionStateChange: ****REAL STATUS " + status + " ERROR****");
            }
        }

        boolean disconnect;
        boolean logStatus = false;

        if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS)
        {
            // TODO:(pv) What if mGattSolicitedDisconnecting != null?

            onDeviceConnected();

            timerStart(GattOperation.DiscoverServices);
            if (!gatt.discoverServices())
            {
                FooLog.e(TAG, mDeviceAddressString +
                              " onConnectionStateChange: gatt.discoverServices() failed; disconnecting...");
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
                FooLog.e(TAG, mDeviceAddressString +
                              " onConnectionStateChange: UNEXPECTED newState=" + newStateString +
                              ", status=" + status +
                              "; disconnecting...");
            }
        }

        FooLog.i(TAG, mDeviceAddressString + " onConnectionStateChange: disconnect=" + disconnect);
        if (disconnect)
        {
            DisconnectReason reason = mGattSolicitedDisconnecting != null ?
                    DisconnectReason.SolicitedDisconnect : DisconnectReason.UnsolicitedDisconnect;
            onDeviceDisconnected(gatt, status, reason, logStatus);
        }
    }

    private void onDeviceOperationTimeout(final GattOperation gattOperation,
                                          final long elapsedMillis)
    {
        mHandlerMain.post(new Runnable()
        {
            @Override
            public void run()
            {
                boolean disconnect = true;

                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        if (deviceListener.onDeviceOperationTimeout(FooGattHandler.this,
                                gattOperation,
                                elapsedMillis))
                        {
                            disconnect = false;
                        }
                    }

                    onLoopingListenersEnd();
                }

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
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        deviceListener.onDeviceConnecting(FooGattHandler.this);
                    }

                    onLoopingListenersEnd();
                }
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
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        deviceListener.onDeviceConnected(FooGattHandler.this,
                                elapsedMillis);
                    }

                    onLoopingListenersEnd();
                }
            }
        });
    }

    protected void onDeviceDisconnected(BluetoothGatt gatt,
                                        final int status,
                                        final DisconnectReason reason,
                                        boolean logStatus)
    {
        FooLog.i(TAG, mDeviceAddressString +
                      " onDeviceDisconnected(gatt, status=" + status + ", reason=" + reason + ')');

        final int elapsedMillis = (int) timerElapsed(GattOperation.Connect, true);

        if (logStatus)
        {
            logStatus("onDeviceDisconnected", status, null);
        }

        mStartTimes.clear();

        mIsWaitingForCharacteristicSetNotification = false;

        // Only set here and in #disconnect
        mGattSolicitedDisconnecting = null;

        //
        // Call disconnect to handle unsolicited disconnected; safe to call if already disconnected
        //
        disconnect();

        //mHandlerMain.removeCallbacksAndMessages(deviceAddress);
        //mHandlerBackground.removeCallbacksAndMessages(deviceAddress);

        mHandlerMain.post(new Runnable()
        {
            @Override
            public void run()
            {
                FooLog.d(TAG, mDeviceAddressString +
                              " onDeviceDisconnected: +deviceListener(s).onDeviceDisconnected");
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    //FooLog.e(TAG, mDeviceAddressString +
                    //             " onDeviceDisconnected: mListeners=" + mListeners);

                    Iterator<GattHandlerListener> it = mListeners.iterator();
                    while (it.hasNext())
                    {
                        GattHandlerListener deviceListener = it.next();

                        it.remove();

                        deviceListener.onDeviceDisconnected(FooGattHandler.this,
                                status,
                                reason,
                                elapsedMillis);
                    }

                    onLoopingListenersEnd();
                }
                FooLog.d(TAG, mDeviceAddressString +
                              " onDeviceDisconnected: -deviceListener(s).onDeviceDisconnected");
            }
        });
    }

    private void onServicesDiscovered(BluetoothGatt gatt, int status)
    {
        int elapsedMillis = (int) timerElapsed(GattOperation.DiscoverServices, true);

        FooLog.v(TAG, mDeviceAddressString +
                      " onServicesDiscovered(gatt, status=" + status + ')');

        logStatus("onServicesDiscovered", status, null);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        mPendingOperationSignal.set();

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
        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        deviceListener.onDeviceServicesDiscovered(FooGattHandler.this,
                                services,
                                success,
                                elapsedMillis);
                    }

                    onLoopingListenersEnd();
                }
            }
        });
    }

    private BluetoothGattCharacteristic createBluetoothGattCharacteristic(UUID serviceUuid, UUID characteristicUuid)
    {
        BluetoothGattService service = new BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(characteristicUuid, 0, 0);
        service.addCharacteristic(characteristic);
        return characteristic;
    }

    public void characteristicRead(UUID serviceUuid, UUID characteristicUuid)
    {
        characteristicRead(serviceUuid, characteristicUuid, sDefaultTimeoutMillis);
    }

    public void characteristicRead(final UUID serviceUuid, final UUID characteristicUuid,
                                   final long timeoutMillis)
    {
        if (serviceUuid == null)
        {
            throw new IllegalArgumentException("serviceUuid must not be null");
        }

        if (characteristicUuid == null)
        {
            throw new IllegalArgumentException("characteristicUuid must not be null");
        }

        if (!ensureConnected("characteristicRead", timeoutMillis))
        {
            return;
        }

        timerStart(GattOperation.CharacteristicRead);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                FooLog.v(TAG, mDeviceAddressString +
                              " characteristicRead(serviceUuid=" + serviceUuid +
                              ", characteristicUuid=" + characteristicUuid +
                              ", timeoutMillis=" + timeoutMillis + ')');

                mPendingOperationSignal.reset();

                BluetoothGattCharacteristic characteristic;

                BluetoothGatt gatt = mGattConnectingOrConnected;

                if (gatt == null)
                {
                    FooLog.w(TAG,
                            mDeviceAddressString + " characteristicRead: Not connected to device; ignoring");
                    onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                    return;
                }

                BluetoothGattService service = gatt.getService(serviceUuid);
                if (service == null)
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicRead: gatt.getService(" +
                                  serviceUuid +
                                  ") failed");
                    onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                    return;
                }

                characteristic = service.getCharacteristic(characteristicUuid);
                if (characteristic == null)
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicRead: service.getCharacteristic(" + characteristicUuid +
                                  ") failed");
                    onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                    return;
                }

                if (!gatt.readCharacteristic(characteristic))
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicRead: gatt.characteristicRead(...) failed for characteristic " +
                                  characteristicUuid);
                    onDeviceCharacteristicRead(serviceUuid, characteristicUuid, false);
                    return;
                }

                waitForOperationCompletion(GattOperation.CharacteristicRead, timeoutMillis);
            }
        });
    }

    private void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        UUID characteristicUuid = characteristic.getUuid();
        FooLog.v(TAG, mDeviceAddressString +
                      " onCharacteristicRead(gatt, characteristic=" + characteristicUuid + ", status=" +
                      status +
                      ')');

        logStatus("onCharacteristicRead", status, "for characteristic " + characteristicUuid);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        mPendingOperationSignal.set();

        onDeviceCharacteristicRead(characteristic, success);
    }

    private void onDeviceCharacteristicRead(UUID serviceUuid, UUID characteristicUuid,
                                            boolean success)
    {
        BluetoothGattCharacteristic characteristic = createBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        onDeviceCharacteristicRead(characteristic, success);
    }

    private void onDeviceCharacteristicRead(final BluetoothGattCharacteristic characteristic,
                                            final boolean success)
    {
        final long elapsedMillis = timerElapsed(GattOperation.CharacteristicRead, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        deviceListener.onDeviceCharacteristicRead(FooGattHandler.this,
                                characteristic,
                                success,
                                elapsedMillis);
                    }

                    onLoopingListenersEnd();
                }
            }
        });
    }

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
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    String value)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, null);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param characteristicWriteType null to ignore
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    String value,
                                    CharacteristicWriteType characteristicWriteType)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param timeoutMillis
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    String value,
                                    long timeoutMillis)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, null, timeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    String value,
                                    CharacteristicWriteType characteristicWriteType,
                                    long timeoutMillis)
    {
        characteristicWrite(serviceUuid, characteristicUuid, toBytes(value), characteristicWriteType, timeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param formatType
     * @param offset
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int value, int formatType, int offset)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, formatType, offset, null);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param formatType
     * @param offset
     * @param characteristicWriteType null to ignore
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int value, int formatType, int offset,
                                    CharacteristicWriteType characteristicWriteType)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, formatType, offset, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param formatType
     * @param offset
     * @param timeoutMillis
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int value, int formatType, int offset,
                                    long timeoutMillis)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, formatType, offset, null, timeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param formatType
     * @param offset
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int value, int formatType, int offset,
                                    CharacteristicWriteType characteristicWriteType,
                                    long timeoutMillis)
    {
        characteristicWrite(serviceUuid, characteristicUuid, toBytes(value, formatType, offset), characteristicWriteType, timeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param mantissa
     * @param exponent
     * @param formatType
     * @param offset
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int mantissa, int exponent, int formatType, int offset)
    {
        characteristicWrite(serviceUuid, characteristicUuid, mantissa, exponent, formatType, offset, null);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param mantissa
     * @param exponent
     * @param formatType
     * @param offset
     * @param characteristicWriteType null to ignore
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int mantissa, int exponent, int formatType, int offset,
                                    CharacteristicWriteType characteristicWriteType)
    {
        characteristicWrite(serviceUuid, characteristicUuid, mantissa, exponent, formatType, offset, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param mantissa
     * @param exponent
     * @param formatType
     * @param offset
     * @param timeoutMillis
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int mantissa, int exponent, int formatType, int offset,
                                    long timeoutMillis)
    {
        characteristicWrite(serviceUuid, characteristicUuid, mantissa, exponent, formatType, offset, null, timeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param mantissa
     * @param exponent
     * @param formatType
     * @param offset
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    int mantissa, int exponent, int formatType, int offset,
                                    CharacteristicWriteType characteristicWriteType,
                                    long timeoutMillis)
    {
        characteristicWrite(serviceUuid, characteristicUuid, toBytes(mantissa, exponent, formatType, offset), characteristicWriteType, timeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    byte[] value)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, null);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param characteristicWriteType null to ignore
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    byte[] value,
                                    CharacteristicWriteType characteristicWriteType)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, characteristicWriteType, sDefaultTimeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param timeoutMillis
     */
    public void characteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                    byte[] value,
                                    long timeoutMillis)
    {
        characteristicWrite(serviceUuid, characteristicUuid, value, null, timeoutMillis);
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param value
     * @param characteristicWriteType null to ignore
     * @param timeoutMillis
     */
    public void characteristicWrite(final UUID serviceUuid, final UUID characteristicUuid,
                                    final byte[] value,
                                    final CharacteristicWriteType characteristicWriteType,
                                    final long timeoutMillis)
    {
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

        if (!ensureConnected("characteristicWrite", timeoutMillis))
        {
            return;
        }

        timerStart(GattOperation.CharacteristicWrite);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                FooLog.v(TAG, mDeviceAddressString +
                              " characteristicWrite(serviceUuid=" + serviceUuid +
                              ", characteristicUuid=" + characteristicUuid +
                              ", value=" + Arrays.toString(value) +
                              ", characteristicWriteType=" + characteristicWriteType +
                              ", timeoutMillis=" + timeoutMillis + ')');

                mPendingOperationSignal.reset();

                BluetoothGattCharacteristic characteristic;

                BluetoothGatt gatt = mGattConnectingOrConnected;

                if (gatt == null)
                {
                    FooLog.w(TAG,
                            mDeviceAddressString + " characteristicWrite: Not connected to device; ignoring");
                    onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                    return;
                }

                BluetoothGattService service = gatt.getService(serviceUuid);
                if (service == null)
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicWrite: gatt.getService(" + serviceUuid + ") failed");
                    onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                    return;
                }

                characteristic = service.getCharacteristic(characteristicUuid);
                if (characteristic == null)
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicWrite: service.getCharacteristic(" + characteristicUuid + ") failed");
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
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicWrite: characteristic.setValue(" + Arrays.toString(value) +
                                  " failed for characteristic " + characteristicUuid);
                    onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                    return;
                }

                if (!gatt.writeCharacteristic(characteristic))
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicWrite: gatt.characteristicWrite(...) failed for characteristic " +
                                  characteristicUuid);
                    onDeviceCharacteristicWrite(serviceUuid, characteristicUuid, false);
                    return;
                }

                waitForOperationCompletion(GattOperation.CharacteristicWrite, timeoutMillis);
            }
        });
    }

    private void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        UUID characteristicUuid = characteristic.getUuid();
        FooLog.v(TAG, mDeviceAddressString +
                      " onCharacteristicWrite(gatt, characteristic=" + characteristicUuid + ", status=" +
                      status +
                      ')');

        logStatus("onCharacteristicWrite", status, "for characteristic " + characteristicUuid);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        mPendingOperationSignal.set();

        onDeviceCharacteristicWrite(characteristic, success);
    }

    private void onDeviceCharacteristicWrite(UUID serviceUuid, UUID characteristicUuid,
                                             boolean success)
    {
        BluetoothGattCharacteristic characteristic = createBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        onDeviceCharacteristicWrite(characteristic, success);
    }

    private void onDeviceCharacteristicWrite(final BluetoothGattCharacteristic characteristic,
                                             final boolean success)
    {
        final long elapsedMillis = timerElapsed(GattOperation.CharacteristicWrite, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        deviceListener.onDeviceCharacteristicWrite(FooGattHandler.this,
                                characteristic,
                                success,
                                elapsedMillis);
                    }

                    onLoopingListenersEnd();
                }
            }
        });
    }

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

    public void characteristicSetNotification(UUID serviceUuid, UUID characteristicUuid,
                                              CharacteristicNotificationDescriptorType characteristicNotificationDescriptorType)
    {
        characteristicSetNotification(serviceUuid, characteristicUuid, characteristicNotificationDescriptorType, true);
    }

    public void characteristicSetNotification(UUID serviceUuid, UUID characteristicUuid,
                                              CharacteristicNotificationDescriptorType characteristicNotificationDescriptorType,
                                              boolean setDescriptorClientCharacteristicConfig)
    {
        characteristicSetNotification(serviceUuid, characteristicUuid, characteristicNotificationDescriptorType, setDescriptorClientCharacteristicConfig, sDefaultTimeoutMillis);
    }

    public void characteristicSetNotification(final UUID serviceUuid, final UUID characteristicUuid,
                                              final CharacteristicNotificationDescriptorType characteristicNotificationDescriptorType,
                                              final boolean setDescriptorClientCharacteristicConfig,
                                              final long timeoutMillis)
    {
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

        if (!ensureConnected("characteristicSetNotification", timeoutMillis))
        {
            return;
        }

        timerStart(GattOperation.CharacteristicSetNotification);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                FooLog.v(TAG, mDeviceAddressString +
                              " characteristicSetNotification(serviceUuid=" + serviceUuid +
                              ", characteristicUuid=" + characteristicUuid +
                              ", characteristicNotificationDescriptorType=" + characteristicNotificationDescriptorType +
                              ", setDescriptorClientCharacteristicConfig=" + setDescriptorClientCharacteristicConfig +
                              ", timeoutMillis=" + timeoutMillis + ')');

                mPendingOperationSignal.reset();

                BluetoothGattCharacteristic characteristic;

                BluetoothGatt gatt = mGattConnectingOrConnected;

                if (gatt == null)
                {
                    FooLog.w(TAG, mDeviceAddressString +
                                  " characteristicSetNotification: Not connected to device; ignoring");
                    onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                    return;
                }

                BluetoothGattService service = gatt.getService(serviceUuid);
                if (service == null)
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicSetNotification: gatt.getService(" +
                                  serviceUuid +
                                  ") failed");
                    onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                    return;
                }

                characteristic = service.getCharacteristic(characteristicUuid);
                if (characteristic == null)
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicSetNotification: service.getCharacteristic(" +
                                  characteristicUuid +
                                  ") failed");
                    onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                    return;
                }

                boolean enable =
                        characteristicNotificationDescriptorType !=
                        CharacteristicNotificationDescriptorType.Disable;

                if (!gatt.setCharacteristicNotification(characteristic, enable))
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicSetNotification: mGattConnectingOrConnected.characteristicSetNotification(..., enable=" +
                                  enable + ") failed for characteristic " + characteristicUuid);
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
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicSetNotification: characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG" +
                                  ") failed for characteristic " + characteristicUuid);
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
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicSetNotification: descriptor.setValue(" +
                                  Arrays.toString(descriptorValue) +
                                  ") failed for descriptor CLIENT_CHARACTERISTIC_CONFIG for characteristic " +
                                  characteristicUuid);
                    onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                    return;
                }

                if (!gatt.writeDescriptor(descriptor))
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " characteristicSetNotification: mGattConnectingOrConnected.writeDescriptor(...) failed descriptor CLIENT_CHARACTERISTIC_CONFIG");
                    onDeviceCharacteristicSetNotification(serviceUuid, characteristicUuid, false);
                    return;
                }

                mIsWaitingForCharacteristicSetNotification = true;

                waitForOperationCompletion(GattOperation.CharacteristicSetNotification, timeoutMillis);
            }
        });
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

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

        FooLog.v(TAG, mDeviceAddressString +
                      " onDescriptorWrite(gatt, descriptor=CLIENT_CHARACTERISTIC_CONFIG, status=" + status + ')');

        logStatus("onDescriptorWrite", status,
                "for descriptor CLIENT_CHARACTERISTIC_CONFIG for characteristic " + characteristic.getUuid());

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        mPendingOperationSignal.set();

        onDeviceCharacteristicSetNotification(characteristic, success);
    }

    private void onDeviceCharacteristicSetNotification(UUID serviceUuid, UUID characteristicUuid,
                                                       boolean success)
    {
        BluetoothGattCharacteristic characteristic = createBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        onDeviceCharacteristicSetNotification(characteristic, success);
    }

    private void onDeviceCharacteristicSetNotification(final BluetoothGattCharacteristic characteristic,
                                                       final boolean success)
    {
        final long elapsedMillis = timerElapsed(GattOperation.CharacteristicSetNotification, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        deviceListener.onDeviceCharacteristicSetNotification(FooGattHandler.this,
                                characteristic,
                                success,
                                elapsedMillis);
                    }

                    onLoopingListenersEnd();
                }
            }
        });
    }

    private static abstract class Messages
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

    private void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        if (VERBOSE_LOG_CHARACTERISTIC_CHANGE)
        {
            FooLog.v(TAG,
                    mDeviceAddressString + " onCharacteristicChanged: characteristic=" + characteristic.getUuid());
        }

        //
        // Handle the case where disconnect has been called, but the OS has queued up lots of characteristic changes
        //
        if (mGattConnectingOrConnected == null)
        {
            FooLog.v(TAG, mDeviceAddressString +
                          " onCharacteristicChanged: Not connected to device; ignoring");
            return;
        }

        //
        // NOTE:(pv) This method may stream LOTS of data.
        // To avoid excessive memory allocations, this method intentionally deviates from the other methods' uses of
        // "mHandler.post(new Runnable() ...)"
        //
        mHandlerMain.obtainAndSendMessage(Messages.onCharacteristicChanged,
                characteristic);
    }

    private void handleCharacteristicChanged(BluetoothGattCharacteristic characteristic)
    {
        if (mGattConnectingOrConnected == null)
        {
            FooLog.v(TAG, mDeviceAddressString +
                          " handleCharacteristicChanged: Not connected to device; ignoring");
            return;
        }

        // No need to call mHandlerMain.post(...); We are already inside mHandlerMain's Looper thread
        synchronized (mListeners)
        {
            onLoopingListenersBegin();

            for (GattHandlerListener deviceListener : mListeners)
            {
                deviceListener.onDeviceCharacteristicChanged(FooGattHandler.this,
                        characteristic);
            }

            onLoopingListenersEnd();
        }
    }

    private boolean handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case Messages.SolicitedDisconnectInternalTimeout:
            {
                FooLog.v(TAG, mDeviceAddressString + " handleMessage: SolicitedDisconnectInternalTimeout");
                onDeviceDisconnected(mGattSolicitedDisconnecting, -1, DisconnectReason.SolicitedDisconnectTimeout, false);
                break;
            }
            case Messages.onCharacteristicChanged:
            {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) msg.obj;
                if (VERBOSE_LOG_CHARACTERISTIC_CHANGE)
                {
                    FooLog.v(TAG, mDeviceAddressString +
                                  " handleMessage.onCharacteristicChanged: characteristic=" +
                                  characteristic.getUuid());
                }
                handleCharacteristicChanged(characteristic);
                break;
            }
        }
        return false;
    }

    public void readRemoteRssi()
    {
        readRemoteRssi(sDefaultTimeoutMillis);
    }

    public void readRemoteRssi(final long timeoutMillis)
    {
        if (!ensureConnected("readRemoteRssi", timeoutMillis))
        {
            return;
        }

        timerStart(GattOperation.ReadRemoteRssi);

        mHandlerBackground.post(new Runnable()
        {
            public void run()
            {
                FooLog.v(TAG, mDeviceAddressString +
                              " readRemoteRssi(timeoutMillis=" + timeoutMillis + ')');

                mPendingOperationSignal.reset();

                BluetoothGatt gatt = mGattConnectingOrConnected;

                if (gatt == null)
                {
                    FooLog.w(TAG, mDeviceAddressString +
                                  " readRemoteRssi: Not connected to device; ignoring");
                    onDeviceReadRemoteRssi(-1, false);
                    return;
                }

                if (!gatt.readRemoteRssi())
                {
                    FooLog.e(TAG, mDeviceAddressString +
                                  " readRemoteRssi: gatt.readRemoteRssi() failed");
                    onDeviceReadRemoteRssi(-1, false);
                    return;
                }

                waitForOperationCompletion(GattOperation.ReadRemoteRssi, timeoutMillis);
            }
        });
    }

    private void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
    {
        long elapsedMillis = timerElapsed(GattOperation.ReadRemoteRssi, true);

        FooLog.v(TAG, mDeviceAddressString +
                      " onReadRemoteRssi(gatt, rssi=" + rssi + ", status=" + status + ')');

        logStatus("onReadRemoteRssi", status, ", rssi=" + rssi);

        boolean success = status == BluetoothGatt.GATT_SUCCESS;

        mPendingOperationSignal.set();

        onDeviceReadRemoteRssi(rssi, success);
    }

    private void onDeviceReadRemoteRssi(final int rssi,
                                        final boolean success)
    {
        final long elapsedMillis = timerElapsed(GattOperation.ReadRemoteRssi, true);

        mHandlerMain.post(new Runnable()
        {
            public void run()
            {
                synchronized (mListeners)
                {
                    onLoopingListenersBegin();

                    for (GattHandlerListener deviceListener : mListeners)
                    {
                        deviceListener.onDeviceReadRemoteRssi(FooGattHandler.this,
                                rssi,
                                success,
                                elapsedMillis);
                    }

                    onLoopingListenersEnd();
                }
            }
        });
    }

    public static class AutoResetEvent
    {
        private final Object mEvent = new Object();

        private boolean mIsSignaled;

        public AutoResetEvent()
        {
            this(false);
        }

        public AutoResetEvent(boolean signaled)
        {
            mIsSignaled = signaled;
        }

        /**
         * @param timeoutMillis
         * @return true if the current instance receives a signal, otherwise false
         * @throws InterruptedException
         */
        public boolean waitOne(long timeoutMillis)
        {
            synchronized (mEvent)
            {
                if (!mIsSignaled)
                {
                    try
                    {
                        if (timeoutMillis > 0)
                        {
                            long startMillis = System.currentTimeMillis();
                            mEvent.wait(timeoutMillis);
                            long elapsedMillis = System.currentTimeMillis() - startMillis;
                            if (elapsedMillis >= timeoutMillis)
                            {
                                return false;
                            }
                        }
                        else
                        {
                            mEvent.wait();
                        }

                        return mIsSignaled;
                    }
                    catch (InterruptedException e)
                    {
                        return false;
                    }
                    finally
                    {
                        mIsSignaled = false;
                    }
                }
            }

            return true;
        }

        public void set()
        {
            synchronized (mEvent)
            {
                mIsSignaled = true;
                mEvent.notify();
            }
        }

        public void reset()
        {
            synchronized (mEvent)
            {
                mIsSignaled = false;
                mEvent.notify();
            }
        }
    }

    /**
     * @param operation
     * @param timeoutMillis
     */
    private void waitForOperationCompletion(GattOperation operation, long timeoutMillis)
    {
        FooLog.v(TAG, mDeviceAddressString +
                      " waitForOperationCompletion: operation=" + operation +
                      ", timeoutMillis=" + timeoutMillis);

        boolean signaled = mPendingOperationSignal.waitOne(timeoutMillis);
        int elapsedMillis = (int) timerElapsed(operation, false);

        switch (operation)
        {
            case CharacteristicSetNotification:
                mIsWaitingForCharacteristicSetNotification = false;
                break;
        }

        boolean canceledOrInterrupted = elapsedMillis < timeoutMillis;
        if (canceledOrInterrupted)
        {
            FooLog.v(TAG, mDeviceAddressString +
                          " waitForOperationCompletion: operation=" + operation +
                          ", elapsedMillis=" + elapsedMillis + "; CANCELED OR INTERRUPTED");
            return;
        }

        if (signaled)
        {
            FooLog.v(TAG, mDeviceAddressString +
                          " waitForOperationCompletion: operation=" + operation +
                          ", elapsedMillis=" + elapsedMillis + "; SIGNALED");
            return;
        }

        FooLog.w(TAG, mDeviceAddressString +
                      " waitForOperationCompletion: operation=" + operation +
                      ", elapsedMillis=" + elapsedMillis + "; *TIMED OUT*");
        onDeviceOperationTimeout(operation, elapsedMillis);
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#setValue(int, int, int)}
     *
     * @param value      New value for this characteristic
     * @param formatType Integer format type used to transform the value parameter
     * @param offset     Offset at which the value should be placed
     * @return
     */
    private static byte[] toBytes(int value, int formatType, int offset)
    {
        byte[] bytes = new byte[offset + getTypeLen(formatType)];

        switch (formatType)
        {
            case BluetoothGattCharacteristic.FORMAT_SINT8:
                value = intToSignedBits(value, 8);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT8:
                bytes[offset] = (byte) (value & 0xFF);
                break;

            case BluetoothGattCharacteristic.FORMAT_SINT16:
                value = intToSignedBits(value, 16);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT16:
                bytes[offset++] = (byte) (value & 0xFF);
                bytes[offset] = (byte) ((value >> 8) & 0xFF);
                break;

            case BluetoothGattCharacteristic.FORMAT_SINT32:
                value = intToSignedBits(value, 32);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT32:
                bytes[offset++] = (byte) (value & 0xFF);
                bytes[offset++] = (byte) ((value >> 8) & 0xFF);
                bytes[offset++] = (byte) ((value >> 16) & 0xFF);
                bytes[offset] = (byte) ((value >> 24) & 0xFF);
                break;

            default:
                throw new NumberFormatException("Unknown formatType " + formatType);
        }
        return bytes;
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#setValue(int, int, int, int)}
     *
     * @param mantissa   Mantissa for this characteristic
     * @param exponent   exponent value for this characteristic
     * @param formatType Float format type used to transform the value parameter
     * @param offset     Offset at which the value should be placed
     * @return
     */
    private static byte[] toBytes(int mantissa, int exponent, int formatType, int offset)
    {
        byte[] bytes = new byte[offset + getTypeLen(formatType)];

        switch (formatType)
        {
            case BluetoothGattCharacteristic.FORMAT_SFLOAT:
                mantissa = intToSignedBits(mantissa, 12);
                exponent = intToSignedBits(exponent, 4);
                bytes[offset++] = (byte) (mantissa & 0xFF);
                bytes[offset] = (byte) ((mantissa >> 8) & 0x0F);
                bytes[offset] += (byte) ((exponent & 0x0F) << 4);
                break;

            case BluetoothGattCharacteristic.FORMAT_FLOAT:
                mantissa = intToSignedBits(mantissa, 24);
                exponent = intToSignedBits(exponent, 8);
                bytes[offset++] = (byte) (mantissa & 0xFF);
                bytes[offset++] = (byte) ((mantissa >> 8) & 0xFF);
                bytes[offset++] = (byte) ((mantissa >> 16) & 0xFF);
                bytes[offset] += (byte) (exponent & 0xFF);
                break;

            default:
                throw new NumberFormatException("Unknown formatType " + formatType);
        }

        return bytes;
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#setValue(String)}
     *
     * @param value New value for this characteristic
     * @return true if the locally stored value has been set
     */
    private static byte[] toBytes(String value)
    {
        return (value != null) ? value.getBytes() : new byte[] {};
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#getTypeLen(int)}
     * Returns the size of a give value type.
     */
    @SuppressWarnings("JavadocReference")
    private static int getTypeLen(int formatType)
    {
        return formatType & 0xF;
    }

    /**
     * Code taken from {@link BluetoothGattCharacteristic#intToSignedBits(int, int)}
     * Convert an integer into the signed bits of a given length.
     */
    @SuppressWarnings("JavadocReference")
    private static int intToSignedBits(int i, int size)
    {
        if (i < 0)
        {
            i = (1 << size - 1) + (i & ((1 << size - 1) - 1));
        }
        return i;
    }
}
