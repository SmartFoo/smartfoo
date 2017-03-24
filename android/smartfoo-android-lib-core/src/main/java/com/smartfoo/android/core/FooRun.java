package com.smartfoo.android.core;

import android.support.annotation.NonNull;

import com.smartfoo.android.core.annotations.NonNullNonEmpty;

public class FooRun
{
    private FooRun()
    {
    }

    @NonNull
    public static <T> T throwIllegalArgumentExceptionIfNull(T paramValue,
                                                            @NonNullNonEmpty String paramName)
    {
        throwIllegalArgumentExceptionIfNullOrEmpty(paramName, "paramName");
        if (paramValue == null)
        {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
        return paramValue;
    }

    @NonNullNonEmpty
    public static String throwIllegalArgumentExceptionIfNullOrEmpty(String paramValue,
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
        return paramName;
    }

    /**
     * Alias for {@link #throwIllegalArgumentExceptionIfNull(Object, String)}
     *
     * @param paramValue paramValue
     * @param paramName  paramName
     * @param <T>        type
     * @return paramValue
     */
    @NonNull
    public static <T> T toNonNull(T paramValue,
                                  @NonNullNonEmpty String paramName)
    {
        throwIllegalArgumentExceptionIfNull(paramValue, paramName);
        return paramValue;
    }

    /**
     * Alias for {@link #throwIllegalArgumentExceptionIfNullOrEmpty(String, String)}
     *
     * @param paramValue paramValue
     * @param paramName  paramName
     * @return paramValue
     */
    @NonNullNonEmpty
    public static String toNonNullNonEmpty(
            String paramValue,
            @NonNullNonEmpty String paramName)
    {
        throwIllegalArgumentExceptionIfNullOrEmpty(paramValue, paramName);
        return paramValue;
    }
}
