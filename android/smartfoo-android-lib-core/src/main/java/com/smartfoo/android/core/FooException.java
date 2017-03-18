package com.smartfoo.android.core;

import com.smartfoo.android.core.reflection.FooReflectionUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FooException
        extends RuntimeException
{
    public static String toString(Throwable throwable, String fieldsPrefix, boolean cause, boolean stacktrace)
    {
        StringBuilder sb = new StringBuilder();

        if (throwable == null)
        {
            sb.append("null");
        }
        else
        {
            sb.append(FooReflectionUtils.getShortClassName(throwable));
            List<String> parts = new LinkedList<>();
            if (!FooString.isNullOrEmpty(fieldsPrefix))
            {
                parts.add(fieldsPrefix);
            }
            parts.add("message=" + FooString.quote(throwable.getMessage()));
            if (cause)
            {
                Throwable throwableCause = throwable.getCause();
                parts.add("cause=" + toString(throwableCause, null, true, stacktrace));
            }
            if (stacktrace)
            {
                String throwableStackTrace = toStackTraceString(throwable);
                parts.add("stacktrace=" + throwableStackTrace);
            }

            if (parts.size() > 0)
            {
                sb.append("{");
                Iterator<String> it = parts.iterator();
                while (it.hasNext())
                {
                    sb.append(' ').append(it.next());
                    if (it.hasNext())
                    {
                        sb.append(',');
                    }
                }
                sb.append(" }");
            }
        }

        return sb.toString();
    }

    public static String toStackTraceString(Throwable throwable)
    {
        String stackTrace = null;
        if (throwable != null)
        {
            StringBuilder sb = new StringBuilder();
            StackTraceElement[] stackTraceElements = throwable.getStackTrace();
            if (stackTraceElements != null)
            {
                for (StackTraceElement stackTraceElement : stackTraceElements)
                {
                    sb.append("\n    at ").append(stackTraceElement);
                }
                stackTrace = sb.toString();
            }
        }
        return stackTrace;
    }

    private final String mSource;

    public FooException(String source, String message)
    {
        this(source, message, null);
    }

    public FooException(String source, Throwable cause)
    {
        this(source, null, cause);
    }

    public FooException(String source, String message, Throwable cause)
    {
        super(message, cause);
        mSource = source;
    }

    public String getSource()
    {
        return mSource;
    }

    @Override
    public String toString()
    {
        return toString(this, "mSource=" + FooString.quote(mSource), false, false);
    }

    public String toDebugString()
    {
        return toString(this, "mSource=" + FooString.quote(mSource), true, true);
    }
}