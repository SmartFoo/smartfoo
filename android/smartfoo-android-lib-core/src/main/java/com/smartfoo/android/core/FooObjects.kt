package com.smartfoo.android.core

import java.util.Objects

/**
 * Null-safe object utility functions.
 */
object FooObjects {
    /**
     * Returns true if [a] and [b] are equal according to [Objects.equals], which handles null
     * values without throwing.
     *
     * @param a first value, may be null
     * @param b second value, may be null
     * @return true if both are null, or if [a] equals [b]
     */
    fun equals(
        a: Any?,
        b: Any?,
    ): Boolean = Objects.equals(a, b)
}
