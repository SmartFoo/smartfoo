package com.smartfoo.android.core;

import android.content.Context;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.annotations.NonNullNonEmpty;

/**
 * Java to Kotlin Conversion:
 * This file is used almost exclusively by Java code to validate parameters.
 * It should probably be the last Java file converted to Kotlin.
 * Ideally this file would be obsolete/moot after the conversion and not needed at all and can be deleted.
 */
public class FooRun
{
    private FooRun()
    {
    }

    /**
     * This check may seem redundant to use using `@NonNull`, but at Runtime `@NonNull` is just
     * an honorary check and does not actually enforce that the parameter is not null.
     *
     * @param paramValue value of the typed parameter
     * @param paramName name of the parameter to show in the exception message
     * @return paramValue
     * @param <T> type
     */
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

    /**
     * Throws {@link IllegalArgumentException} if {@code paramValue} is null or empty.
     *
     * @param paramValue the string to validate
     * @param paramName  the name of the parameter; must not be null or empty (throws immediately
     *                   if violated)
     * @return {@code paramName} (not {@code paramValue}) — a design quirk; callers should rely
     *         on the side-effect throw rather than the return value
     * @throws IllegalArgumentException if {@code paramName} is null/empty, or if
     *                                  {@code paramValue} is null/empty
     */
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

    /**
     * Validates and returns the given context.
     *
     * @param context the context to validate; must not be null
     * @return {@code context}, never null
     * @throws IllegalArgumentException if {@code context} is null
     */
    @NonNull
    public static Context getContext(@NonNull Context context)
    {
        return toNonNull(context, "context");
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code paramValue} is null.
     *
     * <p>Unlike {@link #throwIllegalArgumentExceptionIfNull}, this method does not validate
     * {@code paramName} and does not return the value.</p>
     *
     * @param paramValue the object to test
     * @param paramName  the parameter name included in the exception message
     * @throws IllegalArgumentException if {@code paramValue} is null
     */
    public static void throwIfNull(Object paramValue,
                                   @NonNullNonEmpty String paramName) {
        if (paramValue == null) {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
    }
}
