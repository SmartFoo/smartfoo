package com.smartfoo.android.core.platform;

import android.content.Context;

import com.smartfoo.android.core.FooRun;

public class FooRes
{
    private FooRes()
    {
    }

    public static String getString(Context context, int resId, Object... formatArgs)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        return context.getString(resId, formatArgs);
    }
}
