package com.smartfoo.android.core

import com.smartfoo.android.core.FooReflection.getShortClassName
import com.smartfoo.android.core.FooString.quote
import java.util.LinkedList

/**
 * Base runtime exception for the SmartFoo library.
 *
 * Carries an optional [source] string that identifies the origin of the exception (typically a
 * class or method name). Use [toDebugString] for a verbose representation that includes the cause
 * chain and stack trace.
 *
 * @property source a human-readable origin identifier, or null
 */
open class FooException
@JvmOverloads constructor(val source: String?, message: String?, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    constructor(source: String?, cause: Throwable?) : this(source, null, cause)

    override fun toString(): String {
        return toString(this, "mSource=" + quote(this.source), cause = false, stacktrace = false)
    }

    /**
     * Returns a verbose string representation that includes the cause chain and full stack trace.
     *
     * @return a multi-line debug string
     */
    fun toDebugString(): String {
        return toString(this, "mSource=" + quote(this.source), cause = true, stacktrace = true)
    }

    companion object {
        /**
         * Formats a [Throwable] into a human-readable string.
         *
         * @param throwable the exception to format, or null
         * @param fieldsPrefix extra fields to include before the message, or null
         * @param cause if true, recursively includes the cause chain
         * @param stacktrace if true, appends the stack trace
         * @return formatted string, or "null" when [throwable] is null
         */
        fun toString(
            throwable: Throwable?,
            fieldsPrefix: String?,
            cause: Boolean,
            stacktrace: Boolean
        ): String {
            val sb = StringBuilder()

            if (throwable == null) {
                sb.append("null")
            } else {
                sb.append(getShortClassName(throwable))
                val parts: MutableList<String?> = LinkedList<String?>()
                if (!FooString.isNullOrEmpty(fieldsPrefix)) {
                    parts.add(fieldsPrefix)
                }
                parts.add("message=" + quote(throwable.message))
                if (cause) {
                    val throwableCause = throwable.cause
                    parts.add("cause=" + toString(throwableCause, null, true, stacktrace))
                }
                if (stacktrace) {
                    val throwableStackTrace: String? = toStackTraceString(throwable)
                    parts.add("stacktrace=$throwableStackTrace")
                }

                if (parts.isNotEmpty()) {
                    sb.append("{")
                    val it = parts.iterator()
                    while (it.hasNext()) {
                        sb.append(' ').append(it.next())
                        if (it.hasNext()) {
                            sb.append(',')
                        }
                    }
                    sb.append(" }")
                }
            }

            return sb.toString()
        }

        /**
         * Converts the stack trace of [throwable] to a formatted string.
         *
         * @param throwable the exception whose stack trace should be formatted, or null
         * @return a newline-separated stack trace string, or null if [throwable] is null or has no trace
         */
        fun toStackTraceString(throwable: Throwable?): String? {
            var stackTrace: String? = null
            if (throwable != null) {
                val sb = StringBuilder()
                val stackTraceElements = throwable.stackTrace
                if (stackTraceElements != null) {
                    for (stackTraceElement in stackTraceElements) {
                        sb.append("\n    at ").append(stackTraceElement)
                    }
                    stackTrace = sb.toString()
                }
            }
            return stackTrace
        }
    }
}