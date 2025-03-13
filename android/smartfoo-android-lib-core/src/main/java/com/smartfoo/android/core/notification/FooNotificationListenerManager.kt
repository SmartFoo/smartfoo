package com.smartfoo.android.core.notification

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import com.smartfoo.android.core.FooListenerManager
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.platform.FooHandler

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(18)
class FooNotificationListenerManager
private constructor() {
    companion object {
        private val TAG: String = FooLog.TAG(FooNotificationListenerManager::class.java)

        /**
         * Needs to be reasonably longer than the app startup time.
         *
         * NOTE1 that the app startup time can be a few seconds when debugging.
         *
         * NOTE2 that this will time out if paused too long at a debug breakpoint while launching.
         */
        @Suppress("ClassName", "MemberVisibilityCanBePrivate")
        object NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS {
            const val NORMAL: Int = 1500
            const val SLOW: Int = 6000

            fun getRecommendedTimeout(slow: Boolean): Int {
                return if (slow) SLOW else NORMAL
            }
        }

        /**
         * Usually [Build.VERSION.SDK_INT], but may be used to force a specific OS Version #
         * **FOR TESTING PURPOSES**.
         */
        private val VERSION_SDK_INT = Build.VERSION.SDK_INT

        fun supportsNotificationListenerSettings(): Boolean {
            return VERSION_SDK_INT >= 19
        }

        /**
         * Per hidden field [android.provider.Settings.Secure] `ENABLED_NOTIFICATION_LISTENERS`
         */
        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

        @JvmOverloads
        @JvmStatic
        fun isNotificationAccessSettingConfirmedEnabled(
            context: Context,
            notificationListenerServiceClass: Class<out NotificationListenerService> = FooNotificationListenerService::class.java
        ): Boolean {
            if (supportsNotificationListenerSettings()) {
                val notificationListenerServiceLookingFor =
                    ComponentName(context, notificationListenerServiceClass)
                FooLog.d(TAG, "isNotificationAccessSettingConfirmedEnabled: notificationListenerServiceLookingFor=$notificationListenerServiceLookingFor")

                val contentResolver = context.contentResolver
                val notificationListenersString =
                    Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS)
                if (notificationListenersString != null) {
                    val notificationListeners = notificationListenersString.split(":".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (i in notificationListeners.indices) {
                        val notificationListener = ComponentName.unflattenFromString(
                            notificationListeners[i]
                        )
                        FooLog.d(TAG, "isNotificationAccessSettingConfirmedEnabled: notificationListeners[$i]=$notificationListener")
                        if (notificationListenerServiceLookingFor == notificationListener) {
                            FooLog.i(TAG, "isNotificationAccessSettingConfirmedEnabled: found match; return true")
                            return true
                        }
                    }
                }
            }

            FooLog.w(TAG, "isNotificationAccessSettingConfirmedEnabled: found NO match; return false")
            return false
        }

        @Suppress("LocalVariableName")
        @JvmStatic
        @get:SuppressLint("InlinedApi")
        val intentNotificationListenerSettings: Intent?
            /**
             * @return null if [supportsNotificationListenerSettings] == false
             */
            get() {
                var intent: Intent? = null
                if (supportsNotificationListenerSettings()) {
                    val ACTION_NOTIFICATION_LISTENER_SETTINGS = if (VERSION_SDK_INT >= 22) {
                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                    } else {
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                    }
                    intent = Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
                return intent
            }

        @JvmStatic
        fun startActivityNotificationListenerSettings(context: Context) {
            context.startActivity(intentNotificationListenerSettings)
        }

        @JvmStatic
        val instance: FooNotificationListenerManager by lazy {
            FooNotificationListenerManager()
        }
    }

    enum class NotConnectedReason {
        ConfirmedNotEnabled,
        ConnectedTimeout,
        Disconnected,
    }

    interface FooNotificationListenerManagerCallbacks {
        /**
         * @param activeNotifications Active StatusBar Notifications
         * @return true to prevent [initializeActiveNotifications] from being automatically called
         */
        fun onNotificationListenerServiceConnected(
            activeNotifications: Array<StatusBarNotification>
        ): Boolean

        fun onNotificationListenerServiceNotConnected(
            reason: NotConnectedReason,
            elapsedMillis: Long
        )

        fun onNotificationPosted(sbn: StatusBarNotification)

        fun onNotificationRemoved(sbn: StatusBarNotification)
    }

    private val mSyncLock = Any()

    /**
     * NOTE: **Purposefully not using FooListenerAutoStartManager due to incompatible logic in [attach]
     */
    private val mListenerManager =
        FooListenerManager<FooNotificationListenerManagerCallbacks>(this)
    private val mHandler = FooHandler()

    private var mNotificationListenerService: FooNotificationListenerService? = null

    private var mNotificationListenerServiceConnectedTimeoutMillis =
        NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.NORMAL.toLong()
    private var mNotificationListenerServiceConnectedTimeoutStartMillis: Long = -1

    /**
     * **Set to slow mode for debug builds.**
     *
     * Sets timeout based on [NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.getRecommendedTimeout]
     *
     * To set a more precise timeout, use [setTimeout]
     */
    fun setSlowMode(value: Boolean) {
        val timeoutMillis =
            NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.getRecommendedTimeout(value)
                .toLong()
        setTimeout(timeoutMillis)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTimeout(timeoutMillis: Long) {
        mNotificationListenerServiceConnectedTimeoutMillis = timeoutMillis
    }

    val isNotificationListenerServiceConnected: Boolean
        get() = mNotificationListenerService != null

    fun attach(
        context: Context,
        callbacks: FooNotificationListenerManagerCallbacks
    ) {
        mListenerManager.attach(callbacks)

        //
        // NOTE: **Purposefully not using FooListenerAutoStartManager due to the following incompatible logic**...
        //
        if (isNotificationAccessSettingConfirmedEnabled(context)) {
            if (mListenerManager.size() == 1) {
                if (mNotificationListenerServiceConnectedTimeoutStartMillis == -1L) {
                    notificationListenerServiceConnectedTimeoutStart(
                        mNotificationListenerServiceConnectedTimeoutMillis
                    )
                }
            }
        } else {
            callbacks.onNotificationListenerServiceNotConnected(
                NotConnectedReason.ConfirmedNotEnabled,
                0
            )
        }
    }

    fun detach(callbacks: FooNotificationListenerManagerCallbacks) {
        mListenerManager.detach(callbacks)

        if (mListenerManager.isEmpty) {
            notificationListenerServiceConnectedTimeoutStop()
        }
    }

    val activeNotifications: Array<StatusBarNotification>?
        get() {
            synchronized(mSyncLock) {
                return if (mNotificationListenerService != null) mNotificationListenerService!!.activeNotifications else null
            }
        }

    fun initializeActiveNotifications() {
        initializeActiveNotifications(activeNotifications)
    }

    private fun initializeActiveNotifications(activeNotifications: Array<StatusBarNotification>?) {
        if (activeNotifications == null) {
            return
        }
        synchronized(mSyncLock) {
            if (mNotificationListenerService == null) {
                return
            }
            for (sbn in activeNotifications) {
                onNotificationPosted(mNotificationListenerService!!, sbn)
            }
        }
    }

    /**
     * **_HACK_** required to detect any [NotificationListenerService] **NOT** calling
     * [NotificationListenerService.onListenerConnected] **after updating/re-installing app
     * _EVEN IF NOTIFICATION ACCESS SAYS/SHOWS IT IS ENABLED_!**:
     *
     * [http://stackoverflow.com/a/37081128/252308](http://stackoverflow.com/a/37081128/252308)
     *
     * Comment is from 2016; page has some updates since.
     *
     * **Background:**
     * After an Application &#91;that requires `BIND_NOTIFICATION_LISTENER_SERVICE` permission&#93; is
     * installed, the user needs to configure it to have `Notification Access`.
     *
     * A user would rarely know to do this on their own, so usually the newly installed app would
     * test that it does not have `Notification Access` and prompt the user to enable it.
     *
     * When enabled, the OS will start the app's [NotificationListenerService].
     *
     * When disabled, the OS will stop the app's [NotificationListenerService].
     *
     * In a perfect world this may seem all well and good, but Android messed up the implementation
     * **making it harder for a developer to develop the code** (and indirectly making things
     * worse for the user).
     *
     * A developer regularly tests their code changes by pushing app updates to the device.
     *
     * The problem is that when a developer updates their app **the OS kills the app's
     * [NotificationListenerService] *BUT DOES NOT RE-START IT!***
     *
     * When the developer launches their updated app, the OS did NOT restart the app's
     * [NotificationListenerService] and the app will not function as expected.
     *
     * In order to get the app's [NotificationListenerService] working again **the developer has to
     * remember to "turn it off back back on again"..._every single time they update their code!_**
     *
     * :/
     *
     * **WORKAROUND:**
     *  1. Launch Device Settings -> ... -> Notification Access
     *  1. Disable Notification Access
     *  1. Enable Notification Access
     *
     * _(NOTE: It is currently unknown if this problem is limited only to app updates during
     * development, or if it also affects user app updates through Google Play.)_
     *
     * Testing for isNotificationAccessSettingEnabled will only tell us if the app has Notification
     * Access enabled; **it will not tell the app if its [NotificationListenerService] is logically
     * _CONNECTED_** (called [NotificationListenerService.onListenerConnected]).
     *
     * Similarly, testing for running services will only tell us if the service is running; **it
     * will `[also]` not tell the app if its [NotificationListenerService] is logically
     * _CONNECTED_** (called [NotificationListenerService.onListenerConnected]).
     *
     * The best option &#91;unfortunately&#93; seems to be to timeout if [NotificationListenerService]
     * [NotificationListenerService.onListenerConnected] is not called within a small amount of time
     * (recommend <1.5s).
     *
     * If [FooNotificationListenerManager] does not call [FooNotificationListenerManager.FooNotificationListenerManagerCallbacks.onNotificationListenerServiceConnected]
     * within that time then the app should prompt the &#91;developer&#93; user to disable and re-enable
     * `Notification Access`.
     */
    private fun notificationListenerServiceConnectedTimeoutStart(timeoutMillis: Long) {
        FooLog.v(TAG, "+notificationListenerServiceConnectedTimeoutStart(timeoutMillis=$timeoutMillis)")
        if (mNotificationListenerServiceConnectedTimeoutStartMillis != -1L) {
            notificationListenerServiceConnectedTimeoutStop()
        }
        mNotificationListenerServiceConnectedTimeoutStartMillis = System.currentTimeMillis()
        mHandler.postDelayed(mNotificationListenerServiceConnectedTimeoutRunnable, timeoutMillis)
        FooLog.v(TAG, "-notificationListenerServiceConnectedTimeoutStart(timeoutMillis=$timeoutMillis)")
    }

    private fun notificationListenerServiceConnectedTimeoutStop() {
        FooLog.v(TAG, "+notificationListenerServiceConnectedTimeoutStop()")
        mNotificationListenerServiceConnectedTimeoutStartMillis = -1
        mHandler.removeCallbacks(mNotificationListenerServiceConnectedTimeoutRunnable)
        FooLog.v(TAG, "-notificationListenerServiceConnectedTimeoutStop()")
    }

    private val mNotificationListenerServiceConnectedTimeoutRunnable = Runnable {
        FooLog.v(TAG, "+mNotificationListenerServiceConnectedTimeoutRunnable.run()")
        val elapsedMillis =
            System.currentTimeMillis() - mNotificationListenerServiceConnectedTimeoutStartMillis
        onNotificationListenerNotConnected(NotConnectedReason.ConnectedTimeout, elapsedMillis)
        FooLog.v(TAG, "-mNotificationListenerServiceConnectedTimeoutRunnable.run()")
    }

    private fun onNotificationListenerConnected(
        notificationListenerService: FooNotificationListenerService,
        activeNotifications: Array<StatusBarNotification>
    ) {
        synchronized(mSyncLock) {
            notificationListenerServiceConnectedTimeoutStop()
            mNotificationListenerService = notificationListenerService
            var initializeActiveNotifications = true
            for (callbacks in mListenerManager.beginTraversing()) {
                initializeActiveNotifications =
                    initializeActiveNotifications and !callbacks.onNotificationListenerServiceConnected(
                        activeNotifications
                    )
            }
            mListenerManager.endTraversing()
            if (initializeActiveNotifications) {
                initializeActiveNotifications(activeNotifications)
            }
        }
    }

    private fun onNotificationListenerNotConnected(
        reason: NotConnectedReason,
        elapsedMillis: Long
    ) {
        FooLog.v(TAG, "+onNotificationListenerNotConnected(reason=$reason)")
        synchronized(mSyncLock) {
            notificationListenerServiceConnectedTimeoutStop()
            if (reason == NotConnectedReason.ConnectedTimeout && mNotificationListenerService != null) {
                return
            }
            mNotificationListenerService = null
            for (callbacks in mListenerManager.beginTraversing()) {
                callbacks.onNotificationListenerServiceNotConnected(reason, elapsedMillis)
            }
            mListenerManager.endTraversing()
        }
        FooLog.v(TAG, "-onNotificationListenerNotConnected(reason=$reason)")
    }

    private fun onNotificationPosted(
        notificationListenerService: FooNotificationListenerService,
        sbn: StatusBarNotification
    ) {
        synchronized(mSyncLock) {
            if (mNotificationListenerService !== notificationListenerService) {
                return
            }
            for (callbacks in mListenerManager.beginTraversing()) {
                callbacks.onNotificationPosted(sbn)
            }
            mListenerManager.endTraversing()
        }
    }

    private fun onNotificationRemoved(
        notificationListenerService: FooNotificationListenerService,
        sbn: StatusBarNotification
    ) {
        synchronized(mSyncLock) {
            if (mNotificationListenerService !== notificationListenerService) {
                return
            }
            for (callbacks in mListenerManager.beginTraversing()) {
                callbacks.onNotificationRemoved(sbn)
            }
            mListenerManager.endTraversing()
        }
    }

    @RequiresApi(18)
    class FooNotificationListenerService
        : NotificationListenerService()
        //RemoteController.OnClientUpdateListener
    {
        companion object {
            private val TAG: String = FooLog.TAG(FooNotificationListenerService::class.java)
        }

        private var mOnListenerConnectedStartMillis: Long = 0
        private lateinit var mNotificationListenerManager: FooNotificationListenerManager

        override fun onCreate() {
            FooLog.v(TAG, "+onCreate()")
            super.onCreate()

            mNotificationListenerManager = instance

            /*
            Context applicationContext = getApplicationContext();
            mRemoteController = new RemoteController(applicationContext, this);
            */

            FooLog.v(TAG, "-onCreate()")
        }

        override fun onListenerConnected() {
            FooLog.v(TAG, "onListenerConnected()")
            super.onListenerConnected()
            mOnListenerConnectedStartMillis = System.currentTimeMillis()
            val activeNotifications = activeNotifications
            mNotificationListenerManager.onNotificationListenerConnected(
                this,
                activeNotifications
            )
        }

        override fun onNotificationPosted(sbn: StatusBarNotification) {
            onNotificationPosted(sbn, null)
        }

        override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
            FooLog.v(TAG, "onNotificationPosted(...)")
            mNotificationListenerManager.onNotificationPosted(this, sbn)
        }

        override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
            FooLog.v(TAG, "onNotificationRankingUpdate(...)")
            super.onNotificationRankingUpdate(rankingMap)
        }

        override fun onListenerHintsChanged(hints: Int) {
            FooLog.v(TAG, "onListenerHintsChanged(...)")
            super.onListenerHintsChanged(hints)
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            FooLog.v(TAG, "onInterruptionFilterChanged(...)")
            super.onInterruptionFilterChanged(interruptionFilter)
        }

        override fun onNotificationRemoved(sbn: StatusBarNotification) {
            onNotificationRemoved(sbn, null)
        }

        override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?) {
            FooLog.v(TAG, "onNotificationRemoved(...)")
            mNotificationListenerManager.onNotificationRemoved(this, sbn)
        }

        override fun onListenerDisconnected() {
            FooLog.v(TAG, "onListenerDisconnected()")
            super.onListenerDisconnected()
            val elapsedMillis = System.currentTimeMillis() - mOnListenerConnectedStartMillis
            mNotificationListenerManager.onNotificationListenerNotConnected(
                NotConnectedReason.Disconnected,
                elapsedMillis
            )
        }

        /*

        //
        // RemoteController
        //

        public static final String ACTION_BIND_REMOTE_CONTROLLER =
                FooReflectionUtils.getClassName(FooNotificationListener.class) +
                ".ACTION_BIND_REMOTE_CONTROLLER";

        public class RemoteControllerBinder
                extends Binder
        {
            public FooNotificationListener getService()
            {
                return FooNotificationListener.this;
            }
        }

        private RemoteController mRemoteController;

        private final IBinder mRemoteControllerBinder = new RemoteControllerBinder();

        public RemoteController getRemoteController()
        {
            return mRemoteController;
        }

        @Override
        public IBinder onBind(Intent intent)
        {
            FooLog.v(TAG, "onBind(intent=" + FooPlatformUtils.toString(intent) + ')');

            if (ACTION_BIND_REMOTE_CONTROLLER.equals(intent.getAction()))
            {
                return mRemoteControllerBinder;
            }

            return super.onBind(intent);
        }

        //
        // RemoteController.OnClientUpdateListener
        //

        @Override
        public void onClientChange(boolean clearing)
        {
            FooLog.v(TAG, "onClientChange(...)");
        }

        @Override
        public void onClientPlaybackStateUpdate(int state)
        {
            FooLog.v(TAG, "onClientPlaybackStateUpdate(...)");
        }

        @Override
        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed)
        {
            FooLog.v(TAG, "onClientPlaybackStateUpdate(...)");
        }

        @Override
        public void onClientTransportControlUpdate(int transportControlFlags)
        {
            FooLog.v(TAG, "onClientTransportControlUpdate(...)");
        }

        @Override
        public void onClientMetadataUpdate(MetadataEditor metadataEditor)
        {
            FooLog.v(TAG, "onClientMetadataUpdate(...)");
        }
        */
    }
}
