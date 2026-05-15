package com.smartfoo.android.core

import com.smartfoo.android.core.FooReflection.getShortClassName
import com.smartfoo.android.core.FooString.quote
import java.util.LinkedList

open class FooException
@JvmOverloads constructor(val source: String?, message: String?, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    constructor(source: String?, cause: Throwable?) : this(source, null, cause)

    override fun toString(): String {
        return toString(this, "mSource=" + quote(this.source), cause = false, stacktrace = false)
    }

    fun toDebugString(): String {
        return toString(this, "mSource=" + quote(this.source), cause = true, stacktrace = true)
    }

    companion object {
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