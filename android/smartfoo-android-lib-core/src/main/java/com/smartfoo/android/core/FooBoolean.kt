package com.smartfoo.android.core

object FooBoolean {
    @JvmOverloads
    fun toBoolean(value: Boolean?, defaultValue: Boolean = false): Boolean {
        if (value == null) {
            return defaultValue
        }

        return value
    }

    fun toByte(value: Boolean?): Byte {
        return FooBoolean.toByte(value != null && value)
    }

    fun toByte(value: Boolean): Byte {
        return (if (value) 1 else 0).toByte()
    }

    @JvmOverloads
    fun toBoolean(value: Number?, defaultValue: Boolean = false): Boolean {
        if (value == null) {
            return defaultValue
        }

        return value.toByte().toInt() != 0
    }

    fun toBoolean(value: String?, defaultValue: Boolean): Boolean {
        if (value == null) {
            return defaultValue
        }

        try {
            return toBoolean(value.toLong())
        } catch (e: NumberFormatException) {
            // ignore
        }

        if ("true".equals(value, ignoreCase = true)) {
            return true
        }

        if ("false".equals(value, ignoreCase = true)) {
            return false
        }

        if ("yes".equals(value, ignoreCase = true)) {
            return true
        }

        if ("no".equals(value, ignoreCase = true)) {
            return false
        }

        if ("y".equals(value, ignoreCase = true)) {
            return true
        }

        if ("n".equals(value, ignoreCase = true)) {
            return false
        }

        throw IllegalArgumentException("value must be an integer, \"true\", \"false\", \"yes\", \"no\", \"y\", or \"n\"")
    }

    fun toString(value: Boolean): String {
        return if (value) "true" else "false"
    }
}
