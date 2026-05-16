package com.smartfoo.android.core.view

import android.view.View

/** View utility functions. */
object FooViewUtils {
    /**
     * Returns a human-readable label for a [android.view.View] visibility constant.
     *
     * @param visibility one of [android.view.View.VISIBLE], [android.view.View.INVISIBLE],
     *   or [android.view.View.GONE]
     * @return a descriptive name including the numeric value, e.g. `"VISIBLE(0))"`
     */
    fun viewVisibilityToString(visibility: Int): String {
        return when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }.let { "$it($visibility))" }
    }
}
