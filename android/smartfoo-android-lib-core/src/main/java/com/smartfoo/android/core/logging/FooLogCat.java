package com.smartfoo.android.core.logging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog.FooLogLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

            final Process process = Runtime.getRuntime().exec(prog.toArray(new String[prog.size()]));
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

            String fakedLastLine = new FooLogAndroidFormatter().format(TAG, FooLogLevel.Debug,
                    "T" + FooString.padNumber(android.os.Process.myTid(), ' ', 5) +
                    " -load(): " + line, null);

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
