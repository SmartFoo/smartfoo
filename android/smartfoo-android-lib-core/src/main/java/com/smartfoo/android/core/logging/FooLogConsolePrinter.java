package com.smartfoo.android.core.logging;

public class FooLogConsolePrinter
        extends FooLogPrinter
{
    private static FooLogConsolePrinter sInstance;

    public static FooLogConsolePrinter getInstance(FooLogFormatter formatter)
    {
        if (sInstance == null)
        {
            sInstance = new FooLogConsolePrinter(formatter);
        }
        return sInstance;
    }

    private final FooLogFormatter mFormatter;

    private FooLogConsolePrinter(FooLogFormatter formatter)
    {
        mFormatter = formatter;
    }

    @Override
    protected boolean printlnInternal(String tag, int level, String msg, Throwable e)
    {
        System.out.println(mFormatter.format(level, tag, msg, e));
        return true;
    }

    @Override
    public void clear()
    {
        // ignore
    }
}
