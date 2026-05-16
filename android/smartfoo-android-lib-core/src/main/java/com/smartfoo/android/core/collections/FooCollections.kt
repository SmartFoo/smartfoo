package com.smartfoo.android.core.collections

import com.smartfoo.android.core.FooObjects

object FooCollections {
    /**
     * Computes a order-sensitive hash code over all elements of [items].
     *
     * Equivalent to the hash code produced by [java.util.List] implementations that iterate
     * each element, combining via `31 * acc + element.hashCode()`.
     *
     * @param items the collection to hash
     * @param T     the element type
     * @return the combined hash code; 0 for an empty collection
     */
    @JvmStatic
    fun <T> hashCode(items: Collection<T>): Int {
        var hashCode = 0
        for (item in items) {
            hashCode = 31 * hashCode + item.hashCode()
        }
        return hashCode
    }

    /**
     * Returns true if [a] and [b] are the same object, or if they have the same size and every
     * element at the same iterator position is equal (order-sensitive, using [FooObjects.equals]).
     *
     * Returns false if either collection is null (but not both — reference equality is checked
     * first).
     *
     * @param a the first collection, or null
     * @param b the second collection, or null
     * @param T the element type
     * @return true if [a] and [b] are identical in size and element order
     */
    @JvmStatic
    fun <T> identical(
        a: Collection<T>?,
        b: Collection<T>?,
    ): Boolean {
        if (a === b) {
            return true
        }

        if (a == null || b == null) {
            return false
        }

        val length = a.size
        if (b.size != length) {
            return false
        }

        val itA = a.iterator()
        val itB = b.iterator()
        var identical = itA.hasNext() == itB.hasNext()
        while (identical && itA.hasNext() && itB.hasNext()) {
            identical = FooObjects.equals(itA.next(), itB.next())
            identical = identical and (itA.hasNext() == itB.hasNext())
        }

        return identical
    }

    /**
     * Returns true if [a] and [b] contain the same elements regardless of order or duplicates.
     *
     * Implemented by converting both collections to [LinkedHashSet] and comparing for equality,
     * so duplicate elements within a single collection are treated as one.
     *
     * @param a the first collection
     * @param b the second collection
     * @param T the element type
     * @return true if the two collections are set-equivalent
     */
    @JvmStatic
    fun <T> equivalent(
        a: Collection<T>,
        b: Collection<T>,
    ): Boolean {
        // TODO:(pv) Make this more efficient...

        if (false) {
            val t1 = a.containsAll(b)
            val t2 = b.containsAll(a)
            val result = t1 && t2
            return result
            //return a.containsAll(b) && b.containsAll(a);
            //return new TreeSet<>(a).equals(new TreeSet<>(b));
        } else {
            val a2 = LinkedHashSet(a)
            val b2 = LinkedHashSet(b)
            val result = a2 == b2
            return result
        }
    }
}
