package com.smartfoo.android.core;

import android.support.annotation.NonNull;

import com.smartfoo.android.core.annotations.NonNullNonEmpty;

public class FooRun
{
    private FooRun()
    {
    }

    public static <T> void throwIllegalArgumentExceptionIfNull(T paramValue,
                                                               @NonNullNonEmpty String paramName)
    {
        throwIllegalArgumentExceptionNullOrEmpty(paramName, "paramName");
        if (paramValue == null)
        {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    public static void throwIllegalArgumentExceptionNullOrEmpty(String paramValue,
                                                                @NonNullNonEmpty String paramName)
    {
        if (FooString.isNullOrEmpty(paramName))
        {
            throw new IllegalArgumentException("paramName must not be null/\"\"");
        }
        if (FooString.isNullOrEmpty(paramValue))
        {
            throw new IllegalArgumentException(paramName + " must not be null/\"\"");
        }
    }

    @NonNull
    public static <T> T toNonNull(T paramValue,
                                  @NonNullNonEmpty String paramName)
    {
        throwIllegalArgumentExceptionIfNull(paramValue, paramName);
        return paramValue;
    }

    @SuppressWarnings("InfiniteRecursion")
    @NonNullNonEmpty
    public static String toNonNullNonEmpty(
            String paramValue,
            @NonNullNonEmpty String paramName)
    {
        throwIllegalArgumentExceptionNullOrEmpty(paramValue, paramName);
        return paramValue;
    }
}
