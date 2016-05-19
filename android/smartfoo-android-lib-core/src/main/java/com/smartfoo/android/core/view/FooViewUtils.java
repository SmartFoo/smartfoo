package com.smartfoo.android.core.view;

import android.view.View;

public class FooViewUtils
{
    public static String viewVisibilityToString(int visibility)
    {
        String name;
        switch (visibility)
        {
            case View.VISIBLE:
                name = "VISIBLE";
                break;
            case View.INVISIBLE:
                name = "INVISIBLE";
                break;
            case View.GONE:
                name = "GONE";
                break;
            default:
                name = "UNKNOWN";
                break;
        }
        return name + '(' + visibility + ')';
    }

    private FooViewUtils()
    {
    }
}
