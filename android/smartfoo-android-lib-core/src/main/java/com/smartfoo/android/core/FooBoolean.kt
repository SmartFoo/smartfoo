package com.smartfoo.android.core

/**
 * Conversion utilities for boolean values across multiple representations.
 *
 * Supports converting to/from [Boolean], [Byte], [Number], and [String]. The string conversion
 * recognises numeric values as well as the literals `"true"`, `"false"`, `"yes"`, `"no"`,
 * `"y"`, and `"n"` (case-insensitive).
 */
object FooBoolean {
    /**
     * Returns [value] if non-null, otherwise returns [defaultValue].
     *
     * @param value the nullable Boolean to unwrap
     * @param defaultValue value to use when [value] is null; defaults to false
     * @return the unwrapped value or [defaultValue]
     */
    @JvmOverloads
    fun toBoolean(value: Boolean?, defaultValue: Boolean = false): Boolean {
        if (value == null) {
            return defaultValue
        }

        return value
    }

    /**
     * Converts a nullable [Boolean] to a [Byte]: `1` for true, `0` for false or null.
     *
     * @param value the value to convert; null is treated as false
     * @return `1` if value is non-null and true, otherwise `0`
     */
    fun toByte(value: Boolean?): Byte {
        return FooBoolean.toByte(value != null && value)
    }

    /**
     * Converts a [Boolean] to a [Byte]: `1` for true, `0` for false.
     *
     * @param value the value to convert
     * @return `1` if true, `0` if false
     */
    fun toByte(value: Boolean): Byte {
        return (if (value) 1 else 0).toByte()
    }

    /**
     * Returns true if [value] is non-null and non-zero, otherwise returns [defaultValue].
     *
     * @param value the nullable Number to test
     * @param defaultValue value to use when [value] is null; defaults to false
     * @return true if non-null and non-zero
     */
    @JvmOverloads
    fun toBoolean(value: Number?, defaultValue: Boolean = false): Boolean {
        if (value == null) {
            return defaultValue
        }

        return value.toByte().toInt() != 0
    }

    /**
     * Parses [value] as a boolean.
     *
     * Accepted values (case-insensitive): numeric strings (non-zero = true), `"true"`, `"false"`,
     * `"yes"`, `"no"`, `"y"`, `"n"`. Returns [defaultValue] if [value] is null.
     *
     * @param value the string to parse
     * @param defaultValue value to return when [value] is null
     * @return the parsed boolean value
     * @throws IllegalArgumentException if [value] is non-null but cannot be parsed
     */
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
