package com.smartfoo.android.core.logging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spanned;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog.FooLogLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * References:
 * <ul>
 * <li>ACRA: https://code.google.com/p/acra/source/browse/trunk/acra/src/main/java/org/acra/collector/LogCatCollector.java</li>
 * <li>alogcat: https://code.google.com/p/alogcat/source/browse/#svn%2Ftrunk%2Fsrc%2Forg%2Fjtb%2Falogcat</li>
 * </ul>
 * JellyBean Woes: https://code.google.com/p/alogcat/issues/detail?id=42
 */
public class FooLogCat
{
    private static final String TAG = FooLog.TAG(FooLogCat.class);

    /**
     * Too large of an email will cause Intent.createChooser to throw android.os.TransactionTooLargeException.<br>
     * 56kb seems to reliably launch Intents and not throw TransactionTooLargeException.<br>
     * Set to &lt;= 0 to disable.<br>
     * TODO:(pv) This is still crashing some times! :(
     */
    public static int EMAIL_MAX_KILOBYTES_DEFAULT = 56;
    /** Maximum log size in bytes for email attachments; derived from {@link #EMAIL_MAX_KILOBYTES_DEFAULT}. */
    public static int EMAIL_MAX_BYTES_DEFAULT     = EMAIL_MAX_KILOBYTES_DEFAULT * 1024;

    /**
     * How many log lines to read before calling publishProgress to update the main UI thread.<br>
     * 250 seems to provide the best performance.<br>
     * I have tried spot values between 1 and 500.<br>
     */
    public static int ACCUMULATOR_MAX = 250;

    /** Line-feed character used when building multi-line log strings. */
    public static final String LINEFEED = FooString.LINEFEED;

    /** Typeface family name applied to log text views (e.g. in the debug activity). */
    public static final String TYPEFACE_FAMILY = "monospace";
    public static final float  TYPEFACE_SIZE   = 0.8f;

    // TODO:(pv) Rename these to indicate which version of Android they came from
    /** Header line emitted by older Android versions when logcat starts (pre-JellyBean format). */
    public static final String HEADER_DEV_LOG_MAIN1 = "--------- beginning of /dev/log/main";
    /** Header line emitted by Android JellyBean and later when logcat starts. */
    public static final String HEADER_DEV_LOG_MAIN2 = "--------- beginning of main";

    /**
     * Clears the Android logcat ring-buffer by executing {@code logcat -c}.
     * Errors are logged via {@link FooLog} and silently swallowed.
     */
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
                FooLog.d(TAG, "Working around JellyBean's 'feature'…");
                try
                {
                    CMDLINE_GRANTPERMS[2] = String.format("pm grant %s android.permission.READ_LOGS", pname);
                    Process p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS, null, null);
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
     * Reads the current logcat output using the {@code threadtime} format.
     * A synthetic summary line is appended so consumers can detect when the load finished.
     *
     * @param limitBytes the maximum number of bytes to return ({@code > 0} to limit,
     *                   {@code <= 0} for no limit); the <em>tail</em> of the log is returned
     *                   when the raw output exceeds this value
     * @return the logcat output as a string, or {@code null} if an {@link java.io.IOException}
     *         occurs
     */
    public static String load(int limitBytes)
    {
        return load(limitBytes, null);
    }

    private static final FooLogAndroidFormatter FAKE_LAST_LINE_FORMATTER = new FooLogAndroidFormatter();

    /**
     * Reads the current logcat output using the {@code threadtime} format and stops early when
     * the optional terminator is found.
     *
     * @param limitBytes the maximum number of bytes to return ({@code > 0} to limit,
     *                   {@code <= 0} for no limit); the <em>tail</em> of the log is returned
     *                   when the raw output exceeds this value
     * @param terminator if non-null and non-empty, reading stops at the first line whose content
     *                   ends with this string; be as specific as possible to avoid false matches
     * @return the logcat output as a string (the tail when limited), or {@code null} if an
     *         {@link java.io.IOException} occurs
     */
    public static String load(int limitBytes, String terminator)
    {
        String debugInfo = null;

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

            final Process process = Runtime.getRuntime().exec(prog.toArray(new String[prog.size()]));
            final InputStreamReader reader = new InputStreamReader(process.getInputStream());

            boolean terminate = !FooString.isNullOrEmpty(terminator);

            String logLine;
            BufferedReader bufferedReader = new BufferedReader(reader);
            while ((logLine = bufferedReader.readLine()) != null)
            {
                sb.append(logLine).append(FooString.LINEFEED);
                if (terminate && logLine.endsWith(terminator))
                {
                    break;
                }
            }

            int start = 0;
            if (limitBytes > 0 && limitBytes < sb.length())
            {
                start = sb.length() - limitBytes;
            }

            String log = sb.substring(start);

            long timeStopMs = System.currentTimeMillis();
            long elapsedMs = timeStopMs - timeStartMs;

            debugInfo = "load took " + elapsedMs + "ms; log.length=" + ((log == null) ? null : log.length());

            String fakedLastLine = FAKE_LAST_LINE_FORMATTER.format(FooLogLevel.Info, TAG,
                    "T" + FooString.padNumber(android.os.Process.myTid(), ' ', 5) + " -load(): " + debugInfo,
                    null);

            return log + fakedLastLine;
        }
        catch (IOException e)
        {
            FooLog.e(TAG, "load: EXCEPTION", e);
            return null;
        }
        finally
        {
            FooLog.i(TAG, "-load(): " + debugInfo);
        }
    }

    public interface LogProcessCallbacks
    {
        int getColorAssert();

        int getColorError();

        int getColorWarn();

        int getColorInfo();

        int getColorDebug();

        int getColorVerbose();

        int getColorOther();

        /**
         * @return ex: "monospace"
         */
        String getTypefaceFamily();

        /**
         * @return ex: 0.8f
         */
        float getTypefaceSize();

        /**
         * How many log lines to read before calling publishProgress to update the main UI thread.<br>
         * 250 seems to provide the best performance.<br>
         * I have tried spot values between 1 and 500.<br>
         *
         * @return ex: 250
         */
        int getAccumulatorMax();

        void onLogLines(List<Spanned> logLines);
    }

    /**
     * Parses a raw logcat string and converts matching lines into colour-coded
     * {@link android.text.Spanned} objects, streaming them to the caller via
     * {@link LogProcessCallbacks#onLogLines}.
     *
     * <p>Lines belonging to a different PID are coloured with the "other" colour. Lines that
     * match one of the {@code HEADER_DEV_LOG_MAIN*} markers trigger a full list reset so that
     * only the content after the most recent log-start header is retained.</p>
     *
     * @param pid       the process ID whose lines should be colour-coded by log level;
     *                  lines from other PIDs receive the "other" colour
     * @param logRaw    the raw logcat output to parse; if null or empty the method uses
     *                  {@link #HEADER_DEV_LOG_MAIN2} as a placeholder
     * @param callbacks callback object supplying colour values, typeface info, batch size, and
     *                  the {@link LogProcessCallbacks#onLogLines} sink; must not be null
     * @return the last un-flushed accumulator list (may be null if everything was flushed)
     */
    public static List<Spanned> process(
            int pid,
            String logRaw,
            @NonNull
            final LogProcessCallbacks callbacks)
    {
        int colorAssert = callbacks.getColorAssert();
        int colorError = callbacks.getColorError();
        int colorWarn = callbacks.getColorWarn();
        int colorInfo = callbacks.getColorInfo();
        int colorDebug = callbacks.getColorDebug();
        int colorVerbose = callbacks.getColorVerbose();
        int colorOther = callbacks.getColorOther();
        String typefaceFamily = callbacks.getTypefaceFamily();
        float typefaceSize = callbacks.getTypefaceSize();

        //
        // Avoids ConcurrentModificationException if we were to pass a single collection to onProgressUpdate
        //
        List<Spanned> accumulator = null;

        try
        {
            int color;
            int style;

            if (FooString.isNullOrEmpty(logRaw))
            {
                logRaw = HEADER_DEV_LOG_MAIN2;
            }

            boolean firstLine = true;
            BufferedReader reader = new BufferedReader(new StringReader(logRaw));
            String logLine;
            LogInfo logInfo;
            while ((logLine = reader.readLine()) != null)
            {
                if (logLine.startsWith(HEADER_DEV_LOG_MAIN1) ||
                    logLine.startsWith(HEADER_DEV_LOG_MAIN2))
                {
                    // We have reached the *actual* beginning of the log.
                    // Clear the *list* and start building it from scratch again.
                    accumulator = accumulate(callbacks, accumulator, null);
                }
                else
                {
                    color = colorOther;

                    logInfo = getLogInfo(pid, logLine);
                    if (logInfo != null)
                    {
                        switch (logInfo.level)
                        {
                            case FooLogLevel.Fatal:
                                color = colorAssert;
                                break;
                            case FooLogLevel.Error:
                                color = colorError;
                                break;
                            case FooLogLevel.Warn:
                                color = colorWarn;
                                break;
                            case FooLogLevel.Info:
                                color = colorInfo;
                                break;
                            case FooLogLevel.Debug:
                                color = colorDebug;
                                break;
                            case FooLogLevel.Verbose:
                            default:
                                color = colorVerbose;
                                break;
                        }
                    }
                    else
                    {
                        if (firstLine)
                        {
                            firstLine = false;
                            continue;
                        }
                    }

                    style = (color == colorOther) ? Typeface.NORMAL : Typeface.BOLD;
                    accumulator =
                            accumulate(callbacks, accumulator,
                                    FooString.newSpannableString(logLine, color, -1,
                                            style, typefaceFamily, typefaceSize));
                }

                firstLine = false;
            }
            accumulator = flush(callbacks, accumulator);
            reader.close();
        }
        catch (Exception e)
        {
            FooLog.e(TAG, "doInBackground(...)", e);
            accumulator =
                    accumulate(callbacks, accumulator,
                            FooString.newSpannableString(
                                    "EXCEPTION doInBackground " + e, colorError, -1,
                                    Typeface.BOLD, typefaceFamily, typefaceSize));
            //noinspection UnusedAssignment
            accumulator = flush(callbacks, accumulator);
        }

        return accumulator;
    }

    /**
     * Returns the PID of the current process.
     *
     * @return the process ID
     */
    public static int getMyPid()
    {
        return android.os.Process.myPid();
    }

    //
    // Format of logcat -v threadtime
    // MM-DD HH:MM:SS.MMM PID TID LEVEL TAG : Message
    private static final Pattern LOGCAT_REGEX_FORMAT_THREADTIME = Pattern.compile("(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3}) +(\\d+) +(\\d+) (\\w) ([\\w…]+) *: (.*)");

    public static class LogInfo
    {
        public final Date   dateTime;
        public final int    level;
        public final int    pid;
        public final int    tid;
        public final String tag;
        public final String message;

        LogInfo(Date dateTime, int pid, int tid, int level, String tag, String message)
        {
            this.dateTime = dateTime;
            this.pid = pid;
            this.tid = tid;
            this.level = level;
            this.tag = tag;
            this.message = message;
        }

        /**
         * Returns a human-readable representation of this log entry, including all fields.
         *
         * @return a string containing the date/time, PID, TID, log level, tag, and message
         */
        @Override
        public String toString()
        {
            return "{ " +
                   " dateTime=" + dateTime +
                   ", pid=" + pid +
                   ", tid=" + tid +
                   ", level=" + level +
                   ", tag=" + FooString.quote(tag) +
                   ", message=" + FooString.quote(message) +
                   " }";
        }
    }

    public static final  DateFormat LOGCAT_DATE_TIME_FORMAT_THREADTIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final Calendar   sCalendar                          = Calendar.getInstance();

    /**
     * Parses a logcat date-time token (format {@code MM-dd HH:mm:ss.SSS}) into a {@link Date},
     * prepending the current calendar year because logcat does not include it.
     *
     * @param logDateTime the date-time portion of a logcat line, e.g. {@code "05-15 13:42:00.123"}
     * @return the parsed {@link Date}, or {@code null} if parsing fails
     */
    public static Date getDateTime(String logDateTime)
    {
        String year = Integer.toString(sCalendar.get(Calendar.YEAR));

        try
        {
            return LOGCAT_DATE_TIME_FORMAT_THREADTIME.parse(year + '-' + logDateTime);
        }
        catch (ParseException e)
        {
            return null;
        }
    }

    /**
     * Converts a single logcat level character to a {@link FooLog.FooLogLevel} constant.
     *
     * @param logLevel the logcat level character ({@code 'V'}, {@code 'D'}, {@code 'I'},
     *                 {@code 'W'}, {@code 'E'}, or {@code 'A'})
     * @return the matching {@link FooLog.FooLogLevel} constant, or {@code -1} if unknown
     */
    public static int getLogLevel(char logLevel)
    {
        switch (logLevel)
        {
            case 'A': // assert
                return FooLogLevel.Fatal;
            case 'E': // error
                return FooLogLevel.Error;
            case 'W': // warn
                return FooLogLevel.Warn;
            case 'I': // info
                return FooLogLevel.Info;
            case 'D': // debug
                return FooLogLevel.Debug;
            case 'V': // verbose
                return FooLogLevel.Verbose;
            default:
                return -1;
        }
    }

    /**
     * Parses a single logcat {@code threadtime}-format line and returns a {@link LogInfo} only
     * if the PID in the line matches the given {@code pid}.
     *
     * @param pid     the process ID to filter by
     * @param logLine a raw logcat line in {@code threadtime} format
     * @return the parsed {@link LogInfo}, or {@code null} if the line does not match the regex
     *         or belongs to a different process
     */
    public static LogInfo getLogInfo(int pid, String logLine)
    {
        LogInfo logInfo = null;

        Matcher matcher = LOGCAT_REGEX_FORMAT_THREADTIME.matcher(logLine);
        if (matcher.matches())
        {
            int logPid = Integer.parseInt(matcher.group(2));
            if (logPid == pid)
            {
                Date dateTime = getDateTime(matcher.group(1));
                int tid = Integer.parseInt(matcher.group(3));
                int level = getLogLevel(matcher.group(4).charAt(0));
                String tag = matcher.group(5);
                String message = matcher.group(6);

                logInfo = new LogInfo(dateTime, pid, tid, level, tag, message);
            }
        }
        return logInfo;
    }

    private static List<Spanned> accumulate(LogProcessCallbacks callbacks, List<Spanned> accumulator, Spanned value)
    {
        if (accumulator == null)
        {
            accumulator = new ArrayList<>();
        }

        accumulator.add(value);

        if (accumulator.size() >= callbacks.getAccumulatorMax())
        {
            accumulator = flush(callbacks, accumulator);
        }

        return accumulator;
    }

    private static List<Spanned> flush(LogProcessCallbacks callbacks, List<Spanned> accumulator)
    {
        if (accumulator != null)
        {
            callbacks.onLogLines(accumulator);
            accumulator = null;
        }
        //noinspection ConstantConditions
        return accumulator;
    }
}