package com.smartfoo.android.core.logging;

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
     * @param tag
     * @param level
     * @param msg
     * @param e
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
     * @param tag
     * @param level
     * @param msg
     * @param e
     * @return true for success and to remain enabled, false for failure and to disable this printer
     */
    protected abstract boolean printlnInternal(String tag, int level, String msg, Throwable e);

    public abstract void clear();
}
