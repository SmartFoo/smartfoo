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
    /** Whether this printer is currently enabled; {@code false} causes all log records to be dropped. */
    protected boolean mIsEnabled = true;

    /**
     * Enables or disables this printer. A disabled printer silently drops all log records.
     * Subclasses may override to perform additional work (e.g. closing a file) when disabling.
     *
     * @param enabled {@code true} to enable; {@code false} to disable
     */
    public void setEnabled(boolean enabled)
    {
        mIsEnabled = enabled;
    }

    /**
     * Returns whether this printer is currently enabled.
     *
     * @return {@code true} if log records are being processed; {@code false} if they are dropped
     */
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

    /**
     * Clears any buffered or stored log content managed by this printer.
     * The exact behaviour is implementation-specific (e.g. deletes a log file, executes
     * {@code logcat -c}, or is a no-op).
     */
    public abstract void clear();
}
