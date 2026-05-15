package com.smartfoo.android.core.view

import android.util.SparseArray
import android.view.View

/**
 * From http://www.piwai.info/android-adapter-good-practices/#Update
 */
@Suppress("unused")
object FooViewHolder {
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