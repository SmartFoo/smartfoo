package com.smartfoo.android.core.logging;

import android.util.Log;

import com.smartfoo.android.core.FooString;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class FooLogFormatter
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    private static final String[] LEVEL_NAMES = new String[]
            {
                    "?", // 0
                    "?", // 1
                    "V", // 2 PbLog.LogLevel.Verbose
                    "D", // 3 PbLog.LogLevel.Debug
                    "I", // 4 PbLog.LogLevel.Info
                    "W", // 5 PbLog.LogLevel.Warn
                    "E", // 6 PbLog.LogLevel.Error
                    "F", // 7 PbLog.LogLevel.Fatal
            };

    protected abstract int getPid();

    protected abstract int getTid();

    public String format(String tag, int level, String msg, Throwable e)
    {
        StringBuilder sb = new StringBuilder() //
                .append(DATE_FORMAT.format(new Date())) //
                .append(' ').append(FooString.padNumber(getPid(), ' ', 5)) //
                .append(' ').append(FooString.padNumber(getTid(), ' ', 5)) //
                .append(' ').append(LEVEL_NAMES[level]) //
                .append(' ').append(tag) //
                .append(' ').append(msg);
        if (e != null)
        {
            sb.append(": throwable=").append(Log.getStackTraceString(e));
        }
        return sb.toString();
    }
}
