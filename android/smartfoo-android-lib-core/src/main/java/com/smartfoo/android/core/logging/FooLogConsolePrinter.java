package com.smartfoo.android.core.logging;

import com.smartfoo.android.core.FooRun;

/**
 * A {@link FooLogPrinter} that writes formatted log messages to {@link System#out}.
 *
 * <p>Delegates formatting to a supplied {@link FooLogFormatter}. Useful for unit tests,
 * desktop JVM contexts, or any environment where LogCat is unavailable. Use
 * {@link #getInstance(FooLogFormatter)} to obtain or create the shared singleton.</p>
 */
public class FooLogConsolePrinter
        extends FooLogPrinter
{
    private static FooLogConsolePrinter sInstance;

    /**
     * Returns the singleton instance, creating it with the supplied formatter on first call.
     * Subsequent calls ignore the {@code formatter} argument and return the existing instance.
     *
     * @param formatter the formatter to use when the instance is first created; must not be null
     * @return the singleton instance, never null
     */
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
        FooRun.throwIllegalArgumentExceptionIfNull(formatter, "formatter");

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
