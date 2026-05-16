package com.smartfoo.android.core

import java.util.Arrays
import java.util.Vector

/**
 * Utility functions for working with arrays.
 *
 * Provides null-safe equality, bulk copy, fill, and sort operations on both raw arrays
 * and [Vector] collections. All methods are available as static JVM calls.
 */
@Suppress("unused")
object FooArrays {
    /**
     * Returns true if the array is null or contains no elements.
     *
     * @param array the array to test
     * @return true if null or empty
     */
    @JvmStatic
    fun isNullOrEmpty(array: Array<Any>?): Boolean = array == null || array.isEmpty()

    /**
     * Returns true if both byte arrays have identical content, including the null == null case.
     *
     * @param a first array, may be null
     * @param b second array, may be null
     * @return true if the arrays are equal in content
     */
    @JvmStatic
    fun equals(
        a: ByteArray?,
        b: ByteArray?,
    ): Boolean = a.contentEquals(b)

    /**
     * Copies [count] bytes from [source] starting at [sourceOffset] into [destination] starting
     * at [destinationOffset].
     *
     * @param source the array to copy from
     * @param sourceOffset starting index in [source]
     * @param destination the array to copy into
     * @param destinationOffset starting index in [destination]
     * @param count number of bytes to copy
     * @return [destination] for chaining
     */
    @JvmStatic
    fun copy(
        source: ByteArray,
        sourceOffset: Int,
        destination: ByteArray,
        destinationOffset: Int,
        count: Int,
    ): ByteArray {
        System.arraycopy(source, sourceOffset, destination, destinationOffset, count)
        return destination
    }

    /**
     * Allocates a new [ByteArray] of length [count] and copies [count] bytes from [source]
     * starting at [offset].
     *
     * @param source the array to copy from
     * @param offset starting index in [source]
     * @param count number of bytes to copy
     * @return a new array containing the copied bytes
     */
    @JvmStatic
    fun copy(
        source: ByteArray,
        offset: Int,
        count: Int,
    ): ByteArray {
        val destination = ByteArray(count)
        System.arraycopy(source, offset, destination, 0, count)
        return destination
    }

    /**
     * Fills [length] bytes of [array] starting at [offset] with [element].
     *
     * @param array the array to fill
     * @param element the byte value to write
     * @param offset starting index (inclusive)
     * @param length end index (exclusive) — matches [java.util.Arrays.fill] semantics
     * @return [array] for chaining
     */
    @JvmStatic
    fun fill(
        array: ByteArray,
        element: Byte,
        offset: Int,
        length: Int,
    ): ByteArray {
        Arrays.fill(array, offset, length, element)
        return array
    }

    /**
     * Sorts [values] in-place using [comparator] and returns the same array.
     *
     * @param values the array to sort
     * @param comparator the comparator that defines the sort order
     * @return [values] after sorting
     */
    @JvmStatic
    fun <T> sort(
        values: Array<T>,
        comparator: FooComparator<T>,
    ): Array<T> {
        Arrays.sort<T>(values, comparator)
        return values
    }

    /**
     * Sorts [vector] in-place using [comparator] and returns the same vector.
     *
     * @param vector the vector to sort
     * @param comparator the comparator that defines the sort order
     * @return [vector] after sorting
     */
    @JvmStatic
    fun <T> sort(
        vector: Vector<T>,
        comparator: FooComparator<T>,
    ): Vector<T> {
        vector.sortWith(comparator)
        return vector
    }
}
