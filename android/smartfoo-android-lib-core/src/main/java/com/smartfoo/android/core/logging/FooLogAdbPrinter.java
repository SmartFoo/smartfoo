package com.smartfoo.android.core.logging;

import android.util.Log;

public class FooLogAdbPrinter
        extends FooLogPrinter
{
    private static FooLogAdbPrinter sInstance;

    public static FooLogAdbPrinter getInstance()
    {
        if (sInstance == null)
        {
            sInstance = new FooLogAdbPrinter();
        }
        return sInstance;
    }

    private FooLogAdbPrinter()
    {
    }

    /*
    public static final int VERBOSE = Log.VERBOSE;
    public static final int FATAL   = 0;          // Log.ASSERT;
    public static final int ERROR   = Log.ERROR;
    public static final int WARN    = Log.WARN;
    public static final int INFO    = Log.INFO;
    public static final int DEBUG   = Log.DEBUG;
    */

    private static final int[] sFooLogToAdbLogLevels =
            {
                    -1,   // 0
                    -1,   // 1
                    Log.VERBOSE,   // 2 FooLog.LogLevel.Verbose
                    Log.DEBUG, // 3 FooLog.LogLevel.Debug
                    Log.INFO, // 4 FooLog.LogLevel.Info
                    Log.WARN, // 5 FooLog.LogLevel.Warn
                    Log.ERROR, // 6 FooLog.LogLevel.Error
                    Log.ASSERT // 7 FooLog.LogLevel.Fatal
            };

    public static boolean isLoggable(String tag, int level)
    {
        return Log.isLoggable(tag, level);
    }

    /**
     * You can change the default level by setting a system property: 'setprop log.tag.&lt;YOUR_LOG_TAG&gt;
     * &lt;LEVEL&gt;'
     * Where level is either VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, or SUPPRESS.
     * SUPPRESS will turn off all logging for your tag.
     * You can also create a local.prop file that with the following in it:
     * 'log.tag.&lt;YOUR_LOG_TAG&gt;=&lt;LEVEL&gt;' and place that in /data/local.prop.
     *
     * @param tag   tag
     * @param level level
     */
    public static void setTagLevel(String tag, int level)
    {
        // ignore
    }

    /**
     * Prints a line to LogCat.
     * On Android, "System.out.println(...)" also prints to LogCat.
     * Do *NOT* "System.out.println(...)"; it would add a [near] duplicated line to LogCat.
     * <p>
     * {@inheritDoc}
     *
     * @param tag   tag
     * @param level level
     * @param msg   msg
     * @param e     e
     */
    @Override
    protected boolean printlnInternal(String tag, int level, String msg, Throwable e)
    {
        // LogCat does not output the Thread ID; prepend msg with it here.
        StringBuilder sb = new StringBuilder()
                //.append('T').append(Process.myTid()).append(' ')
                .append(msg);

        // LogCat does not output the exception; append msg with it here.
        if (e != null)
        {
            sb.append(": throwable=").append(Log.getStackTraceString(e));
        }

        //noinspection WrongConstant
        Log.println(sFooLogToAdbLogLevels[level], tag, sb.toString());

        return true;
    }

    @Override
    public void clear()
    {
        FooLogCat.clear();
    }

    public static String getStackTraceString(Object caller, Throwable throwable)
    {
        return Log.getStackTraceString(throwable);
    }
}
