package com.smartfoo.android.core.logging;

public class FooLogUnixJavaPrinter
        extends FooLogPrinter
{
    private static FooLogUnixJavaPrinter sInstance;

    public static FooLogUnixJavaPrinter getInstance(FooLogFormatter formatter)
    {
        if (sInstance == null)
        {
            sInstance = new FooLogUnixJavaPrinter(formatter);
        }
        return sInstance;
    }

    private final FooLogFormatter mFormatter;

    private FooLogUnixJavaPrinter(FooLogFormatter formatter)
    {
        mFormatter = formatter;
    }

    @Override
    protected boolean printlnInternal(String tag, int level, String msg, Throwable e)
    {
        System.out.println(mFormatter.format(tag, level, msg, e));
        return true;
    }

    @Override
    public void clear()
    {
        // ignore
    }
}
