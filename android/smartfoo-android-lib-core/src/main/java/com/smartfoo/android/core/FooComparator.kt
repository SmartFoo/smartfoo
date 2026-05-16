package com.smartfoo.android.core

/**
 * A typed [Comparator] that also accepts nullable elements.
 *
 * Extend or implement this interface when a comparator must handle null values in the element type
 * while remaining compatible with APIs that expect [Comparator].
 */
interface FooComparator<T> : Comparator<T?>
