package com.smartfoo.android.core.logging;

/**
 * A {@link FooLogPrinter} that writes formatted log messages to {@link System#out}, intended
 * for plain-JVM (non-Android) environments such as unit-test runners on Unix hosts.
 *
 * <p>Use {@link #getInstance(FooLogFormatter)} to obtain or create the shared singleton.
 * Pair with {@link FooLogUnixJavaFormatter} for Unix-appropriate PID/TID resolution.</p>
 */
public class FooLogUnixJavaPrinter
        extends FooLogPrinter
{
    private static FooLogUnixJavaPrinter sInstance;

    /**
     * Returns the singleton instance, creating it with the supplied formatter on first call.
     * Subsequent calls ignore the {@code formatter} argument.
     *
     * @param formatter the formatter to use when the instance is first created
     * @return the singleton instance, never null
     */
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
        System.out.println(mFormatter.format(level, tag, msg, e));
        return true;
    }

    @Override
    public void clear()
    {
        // ignore
    }
}
