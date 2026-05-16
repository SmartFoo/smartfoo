package com.smartfoo.android.core.logging;

/**
 * Base class for log output destinations registered with {@link FooLog}.
 *
 * <p>Subclasses implement {@link #printlnInternal} to direct log records to their target
 * (LogCat, console, file, …). Returning {@code false} from {@link #printlnInternal} permanently
 * disables the printer via {@link #setEnabled(boolean)}.</p>
 *
 * <p><strong>Important:</strong> Implementations must not call any {@link FooLog} method
 * ({@link FooLog#v}, {@link FooLog#d}, etc.) as that would cause infinite recursion.</p>
 */
public abstract class FooLogPrinter
{
    protected boolean mIsEnabled = true;

    public void setEnabled(boolean enabled)
    {
        mIsEnabled = enabled;
    }

    public boolean isEnabled()
    {
        return mIsEnabled;
    }

    /**
     * Must not call {@link FooLog#v}, {@link FooLog#d}, {@link FooLog#i}, {@link FooLog#w}, {@link FooLog#e}, or
     * {@link FooLog#f}, or infinite recursion might occur.
     *
     * @param tag   tag
     * @param level level
     * @param msg   msg
     * @param e     e
     */
    public void println(String tag, int level, String msg, Throwable e)
    {
        if (!mIsEnabled)
        {
            return;
        }

        if (!printlnInternal(tag, level, msg, e))
        {
            setEnabled(false);
        }
    }

    /**
     * Must not call {@link FooLog#v}, {@link FooLog#d}, {@link FooLog#i}, {@link FooLog#w}, {@link FooLog#e}, or
     * {@link FooLog#f}, or infinite recursion might occur.
     *
     * @param tag   tag
     * @param level level
     * @param msg   msg
     * @param e     e
     * @return true for success and to remain enabled, false for failure and to disable this printer
     */
    protected abstract boolean printlnInternal(String tag, int level, String msg, Throwable e);

    public abstract void clear();
}
