package com.smartfoo.android.core.logging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.smartfoo.android.core.FooString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/* package */ class PlatformLog
{
    public static final int VERBOSE = Log.VERBOSE;
    public static final int FATAL   = 0;           // Log.ASSERT;
    public static final int ERROR   = Log.ERROR;
    public static final int WARN    = Log.WARN;
    public static final int INFO    = Log.INFO;
    public static final int DEBUG   = Log.DEBUG;

    private static final String[] LEVEL_NAMES = new String[]
            {
                    "F", // 0
                    "?", // 1
                    "T", // 2
                    "D", // 3
                    "I", // 4
                    "W", // 5
                    "E", // 6
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
     * @param tag
     * @param level
     */
    public static void setTagLevel(String tag, int level)
    {
        // ignore
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    /**
     * @param tag
     * @param level
     * @param msg
     * @param e
     * @return "MM-dd HH:mm:ss.SSS PID TID Level Tag Message Throwable"
     */
    public static String format(String tag, int level, String msg, Throwable e)
    {
        StringBuilder sb = new StringBuilder() //
                .append(DATE_FORMAT.format(new Date())) //
                .append(' ').append(FooString.padNumber(Process.myPid(), ' ', 5)) //
                .append(' ').append(FooString.padNumber(Process.myTid(), ' ', 5)) //
                .append(' ').append(LEVEL_NAMES[level]) //
                .append(' ').append(tag) //
                .append(' ').append(msg);
        if (e != null)
        {
            sb.append(": throwable=").append(Log.getStackTraceString(e));
        }
        return sb.toString();
    }

    /**
     * Prints a line to LogCat.
     * On Android, "System.out.println(...)" also prints to LogCat.
     * Do *NOT* "System.out.println(...)"; it would add a [near] duplicated line to LogCat.
     *
     * @param tag
     * @param level
     * @param msg
     * @param e
     * @return the resulting formatted string
     */
    public static String println(String tag, int level, String msg, Throwable e)
    {
        // LogCat does its own formatting (time, level, pid, tag, message).
        // Delay most custom formatting until *after* the message is printed to LogCat.

        tag = FooLog.TAG(tag);

        // LogCat does not output the Thread ID; prepend msg with it here.
        StringBuilder sb = new StringBuilder();
        sb.append('T').append(Process.myTid()).append(' ').append(msg);

        // LogCat does not output the exception; append msg with it here.
        if (e != null)
        {
            sb.append(": throwable=").append(Log.getStackTraceString(e));
            // null the exception so that format(...) [below] doesn't append it to msg a second time.
            e = null;
        }

        msg = sb.toString();

        // print to LogCat
        Log.println(level, tag, msg);

        // Now we can format the message for use by the caller
        msg = format(tag, level, msg, e);

        // Again, do not "System.out.println(...)"; it would only add a [near] duplicate line to LogCat.

        return msg;
    }

    public static String getStackTraceString(Object caller, Throwable throwable)
    {
        return Log.getStackTraceString(throwable);
    }

    /**
     * References:
     * <ul>
     * <li>ACRA: https://code.google.com/p/acra/source/browse/trunk/acra/src/main/java/org/acra/collector/LogCatCollector.java</li>
     * <li>alogcat: https://code.google.com/p/alogcat/source/browse/#svn%2Ftrunk%2Fsrc%2Forg%2Fjtb%2Falogcat</li>
     * </ul>
     * JellyBean Woes: https://code.google.com/p/alogcat/issues/detail?id=42
     */
    public static class FooLogCat
    {
        private static final String TAG = FooLog.TAG("FooLogCat");

        // TODO:(pv) Rename these to indicate which version of Android they came from
        public static final String HEADER_DEV_LOG_MAIN1 = "--------- beginning of /dev/log/main";
        public static final String HEADER_DEV_LOG_MAIN2 = "--------- beginning of main";

        public static void clear()
        {
            try
            {
                String[] CMDLINE_LOGCAT_CLEAR =
                        {
                                "logcat", "-c"
                        };
                Runtime.getRuntime().exec(CMDLINE_LOGCAT_CLEAR);
            }
            catch (IOException e)
            {
                FooLog.e(TAG, "clear()", e);
            }
        }

        private static void checkPermission(Context context)
        {
            String pname = context.getPackageName();
            String[] CMDLINE_GRANTPERMS =
                    {
                            "su", "-c", null
                    };

            if (context.getPackageManager().checkPermission(Manifest.permission.READ_LOGS, pname) !=
                PackageManager.PERMISSION_GRANTED)
            {
                FooLog.d(TAG, "checkPermission(Manifest.permission.READ_LOGS, " + FooString.quote(pname) +
                              ") != PackageManager.PERMISSION_GRANTED");

                if (Build.VERSION.SDK_INT >= 16)
                {
                    FooLog.d(TAG, "Working around JellyBean's 'feature'...");
                    try
                    {
                        CMDLINE_GRANTPERMS[2] = String.format("pm grant %s android.permission.READ_LOGS", pname);
                        java.lang.Process p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS, null, null);
                        int res = p.waitFor();
                        FooLog.d(TAG, "exec returned: " + res);
                        if (res != 0)
                        {
                            throw new Exception("failed to become root");
                        }
                    }
                    catch (Exception e)
                    {
                        FooLog.d(TAG, "exec(): " + e);
                    }
                }
            }
            else
            {
                FooLog.d(TAG, "We have the READ_LOGS permission already!");
            }
        }

        /**
         * @param context
         * @param limit   &gt; 0 to limit the length of the String returned, &lt;= 0 to not limit the length of the
         *                String
         *                returned
         * @return String of the logcat output
         */
        public static String load(Context context, int limit)
        {
            return load(context, limit, null);
        }

        /**
         * @param context
         * @param limit      &gt; 0 to limit the length of the String returned, &lt;= 0 to not limit the length of the
         *                   String returned
         * @param terminator Ending content of last line to stop reading at (NOTE: be as specific as possible)
         * @return String of the logcat output
         */
        public static String load(Context context, int limit, String terminator)
        {
            String log = null;
            String line = null;

            try
            {
                FooLog.i(TAG, "+load()");

                long timeStartMs = System.currentTimeMillis();

                //checkPermission(context);

                StringBuilder sb = new StringBuilder();

                List<String> prog = new ArrayList<>();
                prog.add("logcat");
                prog.add("-v");
                prog.add("threadtime");
                prog.add("-d");

                final java.lang.Process process = Runtime.getRuntime().exec(prog.toArray(new String[prog.size()]));
                final InputStreamReader reader = new InputStreamReader(process.getInputStream());

                boolean terminate = !FooString.isNullOrEmpty(terminator);

                BufferedReader bufferedReader = new BufferedReader(reader);
                while ((line = bufferedReader.readLine()) != null)
                {
                    sb.append(line).append(FooString.LINEFEED);
                    if (terminate && line.endsWith(terminator))
                    {
                        break;
                    }
                }

                int start = 0;
                if (limit > 0 && limit < sb.length())
                {
                    start = sb.length() - limit;
                }
                log = sb.substring(start);

                long timeStopMs = System.currentTimeMillis();
                long elapsedMs = timeStopMs - timeStartMs;

                line = "load took " + elapsedMs + "ms; log.length=" + ((log == null) ? null : log.length());

                String fakedLastLine = PlatformLog.format(TAG, PlatformLog.DEBUG,
                        "T" + FooString.padNumber(android.os.Process.myTid(), ' ', 5) + " -load(): " + line, null);

                return log + fakedLastLine;
            }
            catch (IOException e)
            {
                FooLog.e(TAG, "load: EXCEPTION", e);
                return null;
            }
            finally
            {
                FooLog.i(TAG, "-load(): " + line);
            }
        }
    }
}
