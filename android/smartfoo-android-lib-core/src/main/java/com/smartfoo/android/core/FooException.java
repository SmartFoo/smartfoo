package com.smartfoo.android.core;

public class FooException
        extends RuntimeException
{
    private final String    mSource;
    private final Exception mInnerException;

    public FooException(String source, String message)
    {
        this(source, message, null);
    }

    public FooException(String source, Exception innerException)
    {
        this(source, null, innerException);
    }

    public FooException(String source, String message, Exception innerException)
    {
        super(message);
        mSource = source;
        mInnerException = innerException;
    }

    public Exception getInnerException()
    {
        return mInnerException;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(FooString.getShortClassName(this)).append('(');
        String message = getMessage();
        if (!FooString.isNullOrEmpty(message))
        {
            sb.append('\"').append(message).append('\"');
            if (mInnerException != null)
            {
                sb.append(", ");
            }
        }
        if (mInnerException != null)
        {
            sb.append("mInnerException=").append(FooString.getShortClassName(mInnerException)).append('(');
            message = mInnerException.getMessage();
            if (!FooString.isNullOrEmpty(message))
            {
                sb.append('\"').append(message).append('\"');
            }
            sb.append(')');
        }
        sb.append(')');
        return sb.toString();
    }
}