package com.smartfoo.android.core.logging

import android.content.Context
import com.smartfoo.android.core.BuildConfig
import com.smartfoo.android.core.FooReflection
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.texttospeech.FooTextToSpeech
import kotlin.reflect.KClass

/**
 * Central logging facade for the smartfoo-android-lib-core library.
 *
 * Supports multiple registered [FooLogPrinter] backends (ADB LogCat, console, file, …).
 * Logging is enabled automatically in debug builds and can be toggled at runtime via
 * [isEnabled]. The ADB printer is registered by default.
 *
 * Use [TAG] helpers to create tag strings that comply with Android's 23-character tag limit.
 * Use [v], [d], [i], [w], [e], and [f] to emit log messages at the corresponding levels.
 * Use [s] for text-to-speech log announcements (requires [initializeSpeech] first).
 *
 * Printers are stored in a [LinkedHashSet] so registration order is preserved and duplicate
 * registrations are silently ignored.
 */
@Suppress("unused")
object FooLog {
    //private val TAG = TAG(FooLog::class)

    private const val FORCE_TEXT_LOGGING = true

    private val sLogPrinters: MutableSet<FooLogPrinter>

    @JvmStatic
    var isEnabled: Boolean = false

    init {
        sLogPrinters = LinkedHashSet<FooLogPrinter>()

        addPrinter(FooLogAdbPrinter.getInstance())

        isEnabled = BuildConfig.DEBUG
    }

    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(o: Any) = TAG(FooReflection.getShortClassName(o))

    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(c: Class<*>) = TAG(FooReflection.getShortClassName(c))

    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(c: KClass<*>) = TAG(FooReflection.getShortClassName(c))

    /**
     * Per https://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String,%20int)
     */
    const val LOG_TAG_LENGTH_LIMIT: Int = 23

    /**
     * Limits the tag length to [.LOG_TAG_LENGTH_LIMIT]
     *
     * @param value tag
     * @return the tag limited to [.LOG_TAG_LENGTH_LIMIT]
     */
    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(value: String): String {
        var tag = value
        val length = tag.length
        if (length > LOG_TAG_LENGTH_LIMIT) {
            // Turn "ReallyLongClassName" to "ReallyLo…lassName";
            val half = LOG_TAG_LENGTH_LIMIT / 2
            tag = tag.substring(0, half) + '…' + tag.substring(length - half)
        }
        return tag
    }

    /**
     * Harmless if called multiple times with the same logPrinter
     *
     * @param logPrinter logPrinter
     */
    @JvmStatic
    fun addPrinter(logPrinter: FooLogPrinter?) {
        if (logPrinter == null) {
            return
        }
        synchronized(FooLog::class.java) {
            sLogPrinters.add(logPrinter)
        }
    }

    /**
     * Removes a previously registered [FooLogPrinter]. Harmless if the printer was never added.
     *
     * @param logPrinter the printer to remove; ignored if null
     */
    @JvmStatic
    fun removePrinter(logPrinter: FooLogPrinter?) {
        if (logPrinter == null) {
            return
        }
        synchronized(FooLog::class.java) {
            sLogPrinters.remove(logPrinter)
        }
    }

    /**
     * Returns true if the given printer has been registered with this logger.
     *
     * @param logPrinter the printer to check; returns false if null
     * @return true if the printer is currently registered
     */
    @JvmStatic
    fun isAdded(logPrinter: FooLogPrinter?): Boolean {
        if (logPrinter == null) {
            return false
        }
        synchronized(FooLog::class.java) {
            return sLogPrinters.contains(logPrinter)
        }
    }

    /**
     * Removes all registered printers. After this call no log output is produced until
     * at least one printer is added again via [addPrinter].
     */
    @JvmStatic
    fun clearPrinters() {
        synchronized(FooLog::class.java) {
            sLogPrinters.clear()
        }
    }

    /**
     * Requests every registered [FooLogPrinter] to clear its stored log data (e.g. flush a
     * ring buffer). The semantics of "clear" are defined by each individual printer.
     */
    @JvmStatic
    fun clear() {
        synchronized(FooLog::class.java) {
            for (logPrinter in sLogPrinters) {
                logPrinter.clear()
            }
        }
    }

    internal fun println(tag: String?, level: Int, msg: String?, e: Throwable?) {
        synchronized(FooLog::class.java) {
            @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
            if (FORCE_TEXT_LOGGING || isEnabled)  // && FooLogPlatform.isLoggable(tag, level))
            {
                for (logPrinter in sLogPrinters) {
                    logPrinter.println(tag, level, msg, e)
                }
            }
        }
    }

    @JvmStatic
    fun v(
        tag: String?,
        msg: String?,
    ) {
        v(tag, msg, null)
    }

    @JvmStatic
    fun v(
        tag: String?,
        e: Throwable?,
    ) {
        v(tag, "Throwable", e)
    }

    @JvmStatic
    fun v(
        tag: String?,
        msg: String?,
        e: Throwable?,
    ) {
        println(tag, FooLogLevel.Verbose, msg, e)
    }

    @JvmStatic
    fun d(
        tag: String?,
        msg: String?,
    ) {
        d(tag, msg, null)
    }

    @JvmStatic
    fun d(
        tag: String?,
        e: Throwable?,
    ) {
        d(tag, "Throwable", e)
    }

    @JvmStatic
    fun d(
        tag: String?,
        msg: String?,
        e: Throwable?,
    ) {
        println(tag, FooLogLevel.Debug, msg, e)
    }

    @JvmStatic
    fun i(
        tag: String?,
        msg: String?,
    ) {
        i(tag, msg, null)
    }

    @JvmStatic
    fun i(
        tag: String?,
        e: Throwable?,
    ) {
        i(tag, "Throwable", e)
    }

    @JvmStatic
    fun i(
        tag: String?,
        msg: String?,
        e: Throwable?,
    ) {
        println(tag, FooLogLevel.Info, msg, e)
    }

    @JvmStatic
    fun w(
        tag: String?,
        msg: String?,
    ) {
        w(tag, msg, null)
    }

    @JvmStatic
    fun w(
        tag: String?,
        e: Throwable?,
    ) {
        w(tag, "Throwable", e)
    }

    @JvmStatic
    fun w(
        tag: String?,
        msg: String?,
        e: Throwable?,
    ) {
        println(tag, FooLogLevel.Warn, msg, e)
    }

    @JvmStatic
    fun e(
        tag: String?,
        msg: String?,
    ) {
        e(tag, msg, null)
    }

    @JvmStatic
    fun e(
        tag: String?,
        e: Throwable?,
    ) {
        e(tag, "Throwable", e)
    }

    @JvmStatic
    fun e(
        tag: String?,
        msg: String?,
        e: Throwable?,
    ) {
        println(tag, FooLogLevel.Error, msg, e)
    }

    @JvmStatic
    fun f(
        tag: String?,
        msg: String?,
    ) {
        f(tag, msg, null)
    }

    @JvmStatic
    fun f(
        tag: String?,
        e: Throwable?,
    ) {
        f(tag, "Throwable", e)
    }

    @JvmStatic
    fun f(
        tag: String?,
        msg: String?,
        e: Throwable?,
    ) {
        println(tag, FooLogLevel.Fatal, msg, e)
    }

    private var sTextToSpeech: FooTextToSpeech? = null

    /**
     * Initialises the text-to-speech engine used by [s]. Must be called before [s] is used.
     * Safe to call multiple times; only the first call has any effect.
     *
     * @param context used to initialise the [FooTextToSpeech] singleton
     */
    @JvmStatic
    fun initializeSpeech(context: Context) {
        if (sTextToSpeech == null) {
            sTextToSpeech = FooTextToSpeech.instance

            if (!sTextToSpeech!!.isStarted) {
                sTextToSpeech!!.start(context)
            }
        }
    }

    /**
     * Speaks [text] via the text-to-speech engine (if [isEnabled] is true and text is non-empty).
     * Prepends the tag as camel-case-separated words for context. Requires [initializeSpeech]
     * to have been called first.
     *
     * @param tag  log tag, prepended to [text] when non-empty
     * @param text the text to speak
     */
    @JvmStatic
    fun s(tag: String?, text: String?) {
        s(tag, text, false)
    }

    /**
     * Speaks [text] via the text-to-speech engine (if [isEnabled] is true and text is non-empty).
     *
     * @param tag   log tag, prepended to [text] when non-empty
     * @param text  the text to speak
     * @param clear if true, clears the TTS queue before speaking
     */
    @JvmStatic
    fun s(tag: String?, text: String?, clear: Boolean) {
        var text = text
        check(!(sTextToSpeech == null || !sTextToSpeech!!.isStarted)) { "initializeSpeech(...) must be called first" }

        if (isEnabled && !FooString.isNullOrEmpty(text)) {
            if (!FooString.isNullOrEmpty(tag)) {
                text = FooString.separateCamelCaseWords(tag) + " " + text
            }

            sTextToSpeech!!.speak(text!!, FooTextToSpeech.QueuePlacement.CLEAR)
        }
    }

    /**
     * Logs a byte array at the given level, printing byte-index reference rows (1s, 10s, 100s)
     * alongside the hex-encoded array for easy visual alignment.
     *
     * @param tag   log tag
     * @param level one of the [FooLogLevel] constants
     * @param text  prefix text for each log line
     * @param name  label for the data row (used for padding alignment)
     * @param bytes the byte array to log; does nothing if null
     */
    @JvmStatic
    fun logBytes(tag: String?, level: Int, text: String?, name: String, bytes: ByteArray?) {
        if (bytes != null) {
            val bytesLength = bytes.size

            val reference1s = ByteArray(bytesLength)
            val reference10s = ByteArray(bytesLength)
            val reference100s = ByteArray(bytesLength)
            for (i in 0..<bytesLength) {
                reference1s[i] = (i % 10).toByte()
                reference10s[i] = (i / 10).toByte()
                reference100s[i] = (i / 100).toByte()
            }
            var padding: StringBuilder?
            if (bytesLength > 100) {
                padding = StringBuilder()
                for (i in 0..name.length - 4) {
                    padding.append(' ')
                }
                println(
                    tag, level, text + ":" + padding + "100s(" + bytesLength + ")=[" +
                            FooString.toHexString(reference100s) + "]", null
                )
            }
            if (bytesLength > 10) {
                padding = StringBuilder()
                for (i in 0..name.length - 3) {
                    padding.append(' ')
                }
                println(
                    tag, level, text + ":" + padding + "10s(" + bytesLength + ")=[" +
                            FooString.toHexString(reference10s) + "]", null
                )
            }
            padding = StringBuilder()
            for (i in 0..name.length - 2) {
                padding.append(' ')
            }
            println(
                tag, level, text + ":" + padding + "1s(" + bytesLength + ")=[" +
                        FooString.toHexString(reference1s) + "]", null
            )
            println(
                tag, level, text + ": " + name + "(" + bytesLength + ")=[" +
                        FooString.toHexString(bytes) + "]", null
            )
        }
    }

    interface FooLogLevel {
        companion object {
            /**
             * "Verbose should never be compiled into an application except during development."
             */
            const val Verbose: Int = 2

            /**
             * "Debug logs are compiled in but stripped at runtime."
             */
            const val Debug: Int = 3

            /**
             * "Error, warning and info logs are always kept."
             */
            const val Info: Int = 4

            /**
             * "Error, warning and info logs are always kept."
             */
            const val Warn: Int = 5

            /**
             * "Error, warning and info logs are always kept."
             */
            const val Error: Int = 6

            /**
             * "Report a condition that should never happen."
             */
            const val Fatal: Int = 7
        }
    }
}
