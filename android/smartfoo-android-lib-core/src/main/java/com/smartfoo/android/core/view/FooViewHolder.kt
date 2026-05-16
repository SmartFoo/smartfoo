package com.smartfoo.android.core.view

import android.util.SparseArray
import android.view.View

/**
 * From http://www.piwai.info/android-adapter-good-practices/#Update
 */
@Suppress("unused")
object FooViewHolder {
    /**
     * Retrieves a child view from [view] by [id], caching the result in the view's tag using a
     * [android.util.SparseArray] to avoid repeated [View.findViewById] calls.
     *
     * @param view the parent view whose tag is used as the cache
     * @param id   the child view resource ID to look up
     * @param T    the expected type of the child view
     * @return the child view cast to [T], or null if not found
     */
    fun <T : View?> get(view: View, id: Int): T? {
        var viewHolder = view.tag as SparseArray<View?>?
        if (viewHolder == null) {
            viewHolder = SparseArray<View?>()
            view.tag = viewHolder
        }
        var childView = viewHolder.get(id)
        if (childView == null) {
            childView = view.findViewById(id)
            viewHolder.put(id, childView)
        }
        return childView as T?
    }
}