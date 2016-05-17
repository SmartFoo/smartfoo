package com.smartfoo.android.core.view;

import android.util.SparseArray;
import android.view.View;

/**
 * From http://www.piwai.info/android-adapter-good-practices/#Update
 */
public class FooViewHolder
{
    @SuppressWarnings("unchecked")
    public static <T extends View> T get(View view, int id)
    {
        SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
        if (viewHolder == null)
        {
            viewHolder = new SparseArray<>();
            view.setTag(viewHolder);
        }
        View childView = viewHolder.get(id);
        if (childView == null)
        {
            childView = view.findViewById(id);
            viewHolder.put(id, childView);
        }
        return (T) childView;
    }
}