package com.smartfoo.android.core.logging;

import android.util.Log;

import com.smartfoo.android.core.FooString;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formats log records into a human-readable string for use by {@link FooLogPrinter} implementations.
 *
 * <p>The default format mirrors Android LogCat:
 * {@code MM-DD HH:mm:ss.SSS PID-TID L/TAG: message [: throwable=stacktrace]}.</p>
 *
 * <p>Subclasses must implement {@link #getPid()} and {@link #getTid()} to supply the
 * process/thread IDs appropriate for their runtime environment (Android vs. plain JVM).</p>
 */
public abstract class FooLogFormatter
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    private static final String[] LEVEL_NAMES = new String[]
            {
                    "?", // 0
                    "?", // 1
                    "V", // 2 FooLog.LogLevel.Verbose
                    "D", // 3 FooLog.LogLevel.Debug
                    "I", // 4 FooLog.LogLevel.Info
                    "W", // 5 FooLog.LogLevel.Warn
                    "E", // 6 FooLog.LogLevel.Error
                    "F", // 7 FooLog.LogLevel.Fatal
            };

    protected abstract int getPid();

    protected abstract int getTid();

    /**
     * Formats a log record using the current date/time.
     *
     * @param level the log level (one of {@link FooLog.FooLogLevel} constants)
     * @param tag   the log tag
     * @param msg   the log message
     * @param e     an optional throwable whose stack trace is appended; may be null
     * @return the formatted log line, never null
     */
    public String format(int level, String tag, String msg, Throwable e)
    {
        return format(new Date(), level, tag, msg, e);
    }

    public String format(Date dateTime, int level, String tag, String msg, Throwable e)
    {
        return format(dateTime, getPid(), getTid(), level, tag, msg, e);
    }

    public String format(Date dateTime, int pid, int tid, int level, String tag, String msg, Throwable e)
    {
        StringBuilder sb = new StringBuilder() //
                .append(DATE_FORMAT.format(dateTime)) //
                .append(' ').append(FooString.padNumber(pid, ' ', 5)) //
                .append('-').append(FooString.padNumber(tid, ' ', 5)) //
                .append(' ').append(LEVEL_NAMES[level]) //
                .append('/').append(tag) //
                .append(": ").append(msg);
        if (e != null)
        {
            sb.append(": throwable=").append(Log.getStackTraceString(e));
        }
        return sb.toString();
    }
}
