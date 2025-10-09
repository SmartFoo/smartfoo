package com.smartfoo.android.core.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import com.smartfoo.android.core.FooListenerManager
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.platform.FooHandler
import com.smartfoo.android.core.platform.FooPlatformUtils

@Suppress("unused")
@SuppressLint("ObsoleteSdkInt")
@RequiresApi(18)
class FooNotificationListenerManager
private constructor() {
    companion object {
        private val TAG = FooLog.TAG(FooNotificationListenerManager::class.java)

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
            activeNotifications: List<StatusBarNotification>
        ): Boolean

        fun onNotificationListenerServiceNotConnected(
            reason: NotConnectedReason,
            elapsedMillis: Long
        )

        fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?)

        fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int)
    }

    private val mSyncLock = Any()

    /**
     * NOTE: **Purposefully not using FooListenerAutoStartManager due to incompatible logic in [attach]
     */
    private val mListenerManager = FooListenerManager<FooNotificationListenerManagerCallbacks>(this)
    private val mHandler = FooHandler()

    @GuardedBy("mSyncLock")
    private var mNotificationListenerService: FooNotificationListenerService? = null

    private var mNotificationListenerServiceConnectedTimeoutMillis =
        FooNotificationListener.NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.NORMAL.toLong()
    private var mNotificationListenerServiceConnectedTimeoutStartMillis: Long = -1

    /**
     * **Set to slow mode for debug builds.**
     *
     * Sets timeout based on [FooNotificationListener.NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.getRecommendedTimeout]
     *
     * To set a more precise timeout, use [setTimeout]
     */
    fun setSlowMode(value: Boolean) {
        val timeoutMillis =
            FooNotificationListener.NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS.getRecommendedTimeout(value)
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
        if (FooNotificationListener.hasNotificationListenerAccess(context, FooNotificationListenerService::class.java)) {
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

    //
    //region ActiveNotifications
    //

    /**
     * Holds an immutable snapshot of active notifications and their ranking.
     *
     * Call [snapshot] to populate from a NotificationListenerService, or [reset] to clear.
     */
    class ActiveNotificationsSnapshot {
        /** Snapshot of the [NotificationListenerService] used at the last [snapshot] call; null after [reset]. */
        var notificationListenerService: FooNotificationListenerService? = null
            private set

        /** Snapshot of active notifications at the last [snapshot] call; null after [reset]. */
        var activeNotifications: List<StatusBarNotification>? = null
            private set

        /** Snapshot of the system RankingMap at the last [snapshot] call; null after [reset]. */
        var currentRanking: RankingMap? = null
            private set

        private var _activeNotificationsRankedDelegate = lazy { createActiveNotificationsRanked() }

        /** Ranked view (top → bottom), cached after the first computation per snapshot. */
        val activeNotificationsRanked: List<StatusBarNotification> by _activeNotificationsRankedDelegate

        private fun createActiveNotificationsRanked(): List<StatusBarNotification> {
            val ranked = shadeSort(activeNotifications, currentRanking)
            @Suppress("ConstantConditionIf")
            if (false) {
                for (sbn in ranked) {
                    FooLog.e(TAG, "createActiveNotificationsRanked: notification=${toString(sbn, showAllExtras = false)}")
                }
                FooLog.e(TAG, "createActiveNotificationsRanked: EOL")
            }
            return ranked
        }

        /** Clears all state back to defaults (empty notifications, null ranking map). */
        fun reset() {
            notificationListenerService = null
            activeNotifications = null
            currentRanking = null
            _activeNotificationsRankedDelegate = lazy { createActiveNotificationsRanked() }
        }

        /**
         * Replaces current state with a fresh snapshot from [service].
         * If [service] is null, behaves like [reset].
         */
        @WorkerThread
        fun snapshot(service: FooNotificationListenerService?): ActiveNotificationsSnapshot {
            reset()
            notificationListenerService = service
            if (service != null) {
                activeNotifications = service.activeNotifications?.toList()
                currentRanking = service.currentRanking
            }
            return this
        }

        /**
         * Top to bottom order of appearance in the Notification Shade.
         */
        private enum class UiBucket(val order: Int) {
            MEDIA(0),
            CONVERSATION(1),
            ALERTING(2),
            SILENT(3),
        }

        private fun isMediaNotificationCompat(n: Notification): Boolean {
            val extras = n.extras
            val hasMediaSession = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
            val isTransport = n.category == Notification.CATEGORY_TRANSPORT
            val template = extras?.getString(Notification.EXTRA_TEMPLATE)
            // Accept framework or compat styles; the literal string contains '$'
            val isMediaStyle = template?.endsWith("\$MediaStyle") == true ||
                    template?.contains("MediaStyle") == true
            return hasMediaSession || isTransport || isMediaStyle
        }

        private fun bucketOfWithRank(sbn: StatusBarNotification, r: Ranking): UiBucket {
            if (isMediaNotificationCompat(sbn.notification)) return UiBucket.MEDIA
            if (r.isConversation) return UiBucket.CONVERSATION
            val isSilent = r.isAmbient || r.importance <= NotificationManager.IMPORTANCE_LOW
            return if (isSilent) UiBucket.SILENT else UiBucket.ALERTING
        }

        /** Fallback when RankingMap is null: heuristic using flags/category/priority. */
        private fun bucketOfNoRank(sbn: StatusBarNotification): UiBucket {
            val n = sbn.notification
            if (isMediaNotificationCompat(n)) return UiBucket.MEDIA
            // Heuristic: treat PRIORITY_LOW or below as silent when no ranking is available
            @Suppress("DEPRECATION")
            val silent = n.priority <= Notification.PRIORITY_LOW
            return if (silent) UiBucket.SILENT else UiBucket.ALERTING
        }

        private fun shadeSort(
            actives: List<StatusBarNotification>?,
            rankingMap: RankingMap?
        ): List<StatusBarNotification> {
            val list = actives ?: return emptyList()
            if (list.isEmpty()) return emptyList()

            // Collapse groups: prefer GROUP_SUMMARY when present
            val summariesByGroup = list
                .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
                .associateBy { it.notification.group }

            val collapsed = buildList {
                val seen = HashSet<String>()
                list.filter { it.notification.group.isNullOrEmpty() }
                    .forEach { if (seen.add(it.key)) add(it) }
                list.groupBy { it.notification.group }.forEach { (g, members) ->
                    if (g.isNullOrEmpty()) return@forEach
                    val summary = summariesByGroup[g]
                    if (summary != null) {
                        if (seen.add(summary.key)) add(summary)
                    } else {
                        members.forEach { if (seen.add(it.key)) add(it) }
                    }
                }
            }

            // System order index (may be empty if rankingMap == null)
            val sysOrder: Map<String, Int> =
                rankingMap?.orderedKeys?.withIndex()?.associate { it.value to it.index } ?: emptyMap()

            data class K(val bucket: UiBucket, val sys: Int, val tiebreak: Long)
            val keys = HashMap<String, K>(collapsed.size * 2)

            for (sbn in collapsed) {
                val n = sbn.notification
                val sortKey = n.sortKey
                val tiebreak = if (!sortKey.isNullOrEmpty()) sortKey.hashCode().toLong() else -sbn.postTime

                val (bucket, sysIdx) = if (rankingMap != null) {
                    val r = Ranking()
                    val has = rankingMap.getRanking(sbn.key, r)
                    val b = if (has) bucketOfWithRank(sbn, r) else bucketOfNoRank(sbn)
                    val idx = sysOrder[sbn.key] ?: Int.MAX_VALUE
                    b to idx
                } else {
                    bucketOfNoRank(sbn) to Int.MAX_VALUE
                }

                keys[sbn.key] = K(bucket, sysIdx, tiebreak)
            }

            return collapsed.sortedWith(
                compareBy<StatusBarNotification> { keys[it.key]!!.bucket }
                    .thenBy { keys[it.key]!!.sys }
                    .thenBy { keys[it.key]!!.tiebreak }
            )
        }

        companion object {
            fun toString(ranking: Ranking): String {
                return "{key=${ranking.key}, rank=${ranking.rank}}"
            }

            fun toString(sbn: StatusBarNotification, showAllExtras: Boolean = false): String {
                val notification = sbn.notification
                val extras = notification.extras
                val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
                var text = extras?.getCharSequence(Notification.EXTRA_TEXT)
                if (text != null) {
                    text = if (text.length > 33) {
                        "(${text.length})${FooString.quote(text.substring(0, 32)).replaceAfterLast("\"", "…\"")}"
                    } else {
                        FooString.quote(text)
                    }
                }
                val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)

                val sb = StringBuilder("{ ")
                if (title != null || text != null || subText != null) {
                    sb.append("extras={ ")
                }
                if (title != null) {
                    sb.append("${Notification.EXTRA_TITLE}=${FooString.quote(title)}")
                }
                if (text != null) {
                    sb.append(", ${Notification.EXTRA_TEXT}=$text")
                }
                if (subText != null) {
                    sb.append(", ${Notification.EXTRA_SUB_TEXT}=${FooString.quote(subText)}")
                }
                if (title != null || text != null || subText != null) {
                    sb.append(" }, ")
                }
                sb.append(
                    "id=${sbn.id}, key=${FooString.quote(sbn.key)}, packageName=${
                        FooString.quote(sbn.packageName)
                    }, notification={ $notification"
                )
                if (showAllExtras) {
                    sb.append(", extras=")
                    if (extras != null) {
                        extras.remove(Notification.EXTRA_TITLE)
                        extras.remove(Notification.EXTRA_TEXT)
                        extras.remove(Notification.EXTRA_SUB_TEXT)
                    }
                    sb.append(FooPlatformUtils.toString(extras))
                }
                sb.append(" } }")
                return sb.toString()
            }
        }
    }

    val activeNotifications: List<StatusBarNotification>?
        get() = getActiveNotificationsSnapshot(mNotificationListenerService).activeNotifications

    val activeNotificationsRanked: List<StatusBarNotification>?
        get() = getActiveNotificationsSnapshot(mNotificationListenerService).activeNotificationsRanked

    private val activeNotificationsSnapshot = ActiveNotificationsSnapshot()

    private fun getActiveNotificationsSnapshot(notificationListenerService: FooNotificationListenerService?): ActiveNotificationsSnapshot {
        return activeNotificationsSnapshot.snapshot(notificationListenerService)
    }

    fun initializeActiveNotifications() {
        val notificationListenerService = mNotificationListenerService
        val activeNotificationsSnapshot = getActiveNotificationsSnapshot(notificationListenerService)
        initializeActiveNotifications(activeNotificationsSnapshot)
    }

    private fun initializeActiveNotifications(activeNotificationsSnapshot: ActiveNotificationsSnapshot) {
        for (activeNotification in activeNotificationsSnapshot.activeNotificationsRanked) {
            FooLog.v(TAG, "initializeActiveNotifications: activeNotification=$activeNotification")
            onNotificationPosted(activeNotificationsSnapshot.notificationListenerService!!, activeNotification, activeNotificationsSnapshot.currentRanking)
        }
    }

    //
    //endregion ActiveNotifications
    //

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
    ) {
        synchronized(mSyncLock) {
            notificationListenerServiceConnectedTimeoutStop()
            mNotificationListenerService = notificationListenerService
            val activeNotificationsSnapshot = getActiveNotificationsSnapshot(notificationListenerService)
            var initializeActiveNotifications = true
            for (callbacks in mListenerManager.beginTraversing()) {
                initializeActiveNotifications =
                    initializeActiveNotifications and !callbacks.onNotificationListenerServiceConnected(
                        activeNotificationsSnapshot.activeNotificationsRanked
                    )
            }
            mListenerManager.endTraversing()
            if (initializeActiveNotifications) {
                initializeActiveNotifications(activeNotificationsSnapshot)
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
        sbn: StatusBarNotification,
        rankingMap: RankingMap?
    ) {
        synchronized(mSyncLock) {
            if (mNotificationListenerService !== notificationListenerService) {
                return
            }
            for (callbacks in mListenerManager.beginTraversing()) {
                callbacks.onNotificationPosted(sbn, rankingMap)
            }
            mListenerManager.endTraversing()
        }
    }

    private fun onNotificationRemoved(
        notificationListenerService: FooNotificationListenerService,
        sbn: StatusBarNotification,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        synchronized(mSyncLock) {
            if (mNotificationListenerService !== notificationListenerService) {
                return
            }
            for (callbacks in mListenerManager.beginTraversing()) {
                callbacks.onNotificationRemoved(sbn, rankingMap, reason)
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
            mOnListenerConnectedStartMillis = System.currentTimeMillis()
            mNotificationListenerManager.onNotificationListenerConnected(this)
        }

        override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
            FooLog.v(TAG, "onNotificationPosted(...)")
            mNotificationListenerManager.onNotificationPosted(this, sbn, rankingMap)
        }

        override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
            FooLog.v(TAG, "onNotificationRankingUpdate(...)")
            super.onNotificationRankingUpdate(rankingMap)
        }

        override fun onListenerHintsChanged(hints: Int) {
            FooLog.v(TAG, "onListenerHintsChanged(...)")
            super.onListenerHintsChanged(hints)
        }

        override fun onSilentStatusBarIconsVisibilityChanged(hideSilentStatusIcons: Boolean) {
            FooLog.v(TAG, "onSilentStatusBarIconsVisibilityChanged(...)")
            super.onSilentStatusBarIconsVisibilityChanged(hideSilentStatusIcons)
        }

        override fun onNotificationChannelModified(
            pkg: String?,
            user: UserHandle?,
            channel: NotificationChannel?,
            modificationType: Int
        ) {
            FooLog.v(TAG, "onNotificationChannelModified(...)")
            super.onNotificationChannelModified(pkg, user, channel, modificationType)
        }

        override fun onNotificationChannelGroupModified(
            pkg: String?,
            user: UserHandle?,
            group: NotificationChannelGroup?,
            modificationType: Int
        ) {
            FooLog.v(TAG, "onNotificationChannelGroupModified(...)")
            super.onNotificationChannelGroupModified(pkg, user, group, modificationType)
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            FooLog.v(TAG, "onInterruptionFilterChanged(...)")
            super.onInterruptionFilterChanged(interruptionFilter)
        }

        override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) {
            FooLog.v(TAG, "onNotificationRemoved(...)")
            mNotificationListenerManager.onNotificationRemoved(this, sbn, rankingMap, reason)
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
