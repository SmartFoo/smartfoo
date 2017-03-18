package com.smartfoo.android.core.logging;

import android.content.Context;

import com.smartfoo.android.core.BuildConfig;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.reflection.FooReflectionUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class FooLog
{
    private static final boolean FORCE_TEXT_LOGGING = true;

    public interface FooLogLevel
    {
        /**
         * "Verbose should never be compiled into an application except during development."
         */
        int Verbose = 2;
        /**
         * "Debug logs are compiled in but stripped at runtime."
         */
        int Debug   = 3;
        /**
         * "Error, warning and info logs are always kept."
         */
        int Info    = 4;
        /**
         * "Error, warning and info logs are always kept."
         */
        int Warn    = 5;
        /**
         * "Error, warning and info logs are always kept."
         */
        int Error   = 6;
        /**
         * "Report a condition that should never happen."
         */
        int Fatal   = 7;
    }

    private static final Set<FooLogPrinter> sLogPrinters;

    private static boolean sIsEnabled;

    static
    {
        sLogPrinters = new LinkedHashSet<>();

        addPrinter(FooLogAdbPrinter.getInstance());

        setEnabled(BuildConfig.DEBUG);
    }

    public static void setEnabled(boolean value)
    {
        sIsEnabled = value;
    }

    @SuppressWarnings("unused")
    public static boolean isEnabled()
    {
        return sIsEnabled;
    }

    @SuppressWarnings("unused")
    public static String TAG(Object o)
    {
        return TAG((o == null) ? null : o.getClass());
    }

    public static String TAG(Class c)
    {
        return TAG(FooReflectionUtils.getShortClassName(c));
    }

    /**
     * Per http://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String, int)
     */
    public static int LOG_TAG_LENGTH_LIMIT = 23;

    /**
     * Limits the tag length to {@link #LOG_TAG_LENGTH_LIMIT}
     *
     * @param tag tag
     * @return the tag limited to {@link #LOG_TAG_LENGTH_LIMIT}
     */
    public static String TAG(String tag)
    {
        int length = tag.length();
        if (length > LOG_TAG_LENGTH_LIMIT)
        {
            // Turn "ReallyLongClassName" to "ReallyLo…lassName";
            int half = LOG_TAG_LENGTH_LIMIT / 2;
            tag = tag.substring(0, half) + '…' + tag.substring(length - half);
        }
        return tag;
    }

    /**
     * Harmless if called multiple times with the same logPrinter
     *
     * @param logPrinter logPrinter
     */
    public static void addPrinter(FooLogPrinter logPrinter)
    {
        if (logPrinter == null)
        {
            return;
        }
        synchronized (FooLog.class)
        {
            sLogPrinters.add(logPrinter);
        }
    }

    public static void removePrinter(FooLogPrinter logPrinter)
    {
        if (logPrinter == null)
        {
            return;
        }
        synchronized (FooLog.class)
        {
            sLogPrinters.remove(logPrinter);
        }
    }

    public static boolean isAdded(FooLogPrinter logPrinter)
    {
        if (logPrinter == null)
        {
            return false;
        }
        synchronized (FooLog.class)
        {
            return sLogPrinters.contains(logPrinter);
        }
    }

    public static void clearPrinters()
    {
        synchronized (FooLog.class)
        {
            sLogPrinters.clear();
        }
    }

    public static void clear()
    {
        synchronized (FooLog.class)
        {
            for (FooLogPrinter logPrinter : sLogPrinters)
            {
                logPrinter.clear();
            }
        }
    }

    protected static void println(String tag, int level, String msg, Throwable e)
    {
        synchronized (FooLog.class)
        {
            if (FORCE_TEXT_LOGGING || sIsEnabled)// && FooLogPlatform.isLoggable(tag, level))
            {
                for (FooLogPrinter logPrinter : sLogPrinters)
                {
                    logPrinter.println(tag, level, msg, e);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static void v(String tag, String msg)
    {
        v(tag, msg, null);
    }

    @SuppressWarnings("unused")
    public static void v(String tag, Throwable e)
    {
        v(tag, "Throwable", e);
    }

    public static void v(String tag, String msg, Throwable e)
    {
        println(tag, FooLogLevel.Verbose, msg, e);
    }

    @SuppressWarnings("unused")
    public static void d(String tag, String msg)
    {
        d(tag, msg, null);
    }

    @SuppressWarnings("unused")
    public static void d(String tag, Throwable e)
    {
        d(tag, "Throwable", e);
    }

    public static void d(String tag, String msg, Throwable e)
    {
        println(tag, FooLogLevel.Debug, msg, e);
    }

    @SuppressWarnings("unused")
    public static void i(String tag, String msg)
    {
        i(tag, msg, null);
    }

    @SuppressWarnings("unused")
    public static void i(String tag, Throwable e)
    {
        i(tag, "Throwable", e);
    }

    public static void i(String tag, String msg, Throwable e)
    {
        println(tag, FooLogLevel.Info, msg, e);
    }

    @SuppressWarnings("unused")
    public static void w(String tag, String msg)
    {
        w(tag, msg, null);
    }

    @SuppressWarnings("unused")
    public static void w(String tag, Throwable e)
    {
        w(tag, "Throwable", e);
    }

    public static void w(String tag, String msg, Throwable e)
    {
        println(tag, FooLogLevel.Warn, msg, e);
    }

    @SuppressWarnings("unused")
    public static void e(String tag, String msg)
    {
        e(tag, msg, null);
    }

    @SuppressWarnings("unused")
    public static void e(String tag, Throwable e)
    {
        e(tag, "Throwable", e);
    }

    public static void e(String tag, String msg, Throwable e)
    {
        println(tag, FooLogLevel.Error, msg, e);
    }

    @SuppressWarnings("unused")
    public static void f(String tag, String msg)
    {
        f(tag, msg, null);
    }

    @SuppressWarnings("unused")
    public static void f(String tag, Throwable e)
    {
        f(tag, "Throwable", e);
    }

    public static void f(String tag, String msg, Throwable e)
    {
        println(tag, FooLogLevel.Fatal, msg, e);
    }

    private static FooTextToSpeech sTextToSpeech;

    public static void initializeSpeech(Context context)
    {
        if (sTextToSpeech == null)
        {
            sTextToSpeech = FooTextToSpeech.getInstance();

            if (!sTextToSpeech.isStartingOrStarted())
            {
                sTextToSpeech.start(context);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void s(String tag, String text)
    {
        s(tag, text, false);
    }

    public static void s(String tag, String text, boolean clear)
    {
        if (sTextToSpeech == null || !sTextToSpeech.isStartingOrStarted())
        {
            throw new IllegalStateException("initializeSpeech must be called first");
        }

        if (sIsEnabled && !FooString.isNullOrEmpty(text))
        {
            if (!FooString.isNullOrEmpty(tag))
            {
                text = FooString.separateCamelCaseWords(tag) + " " + text;
            }

            sTextToSpeech.speak(text, clear);
        }
    }

    public static void logBytes(String tag, int level, String text, String name, byte[] bytes)
    {
        if (bytes != null)
        {
            int bytesLength = bytes.length;

            byte[] reference1s = new byte[bytesLength];
            byte[] reference10s = new byte[bytesLength];
            byte[] reference100s = new byte[bytesLength];
            for (int i = 0; i < bytesLength; i++)
            {
                reference1s[i] = (byte) (i % 10);
                reference10s[i] = (byte) (i / 10);
                reference100s[i] = (byte) (i / 100);
            }
            StringBuilder padding;
            if (bytesLength > 100)
            {
                padding = new StringBuilder();
                for (int i = 0; i <= name.length() - 4; i++)
                {
                    padding.append(' ');
                }
                println(tag, level, text + ":" + padding + "100s(" + bytesLength + ")=[" +
                                    FooString.toHexString(reference100s) + "]", null);
            }
            if (bytesLength > 10)
            {
                padding = new StringBuilder();
                for (int i = 0; i <= name.length() - 3; i++)
                {
                    padding.append(' ');
                }
                println(tag, level, text + ":" + padding + "10s(" + bytesLength + ")=[" +
                                    FooString.toHexString(reference10s) + "]", null);
            }
            padding = new StringBuilder();
            for (int i = 0; i <= name.length() - 2; i++)
            {
                padding.append(' ');
            }
            println(tag, level, text + ":" + padding + "1s(" + bytesLength + ")=[" +
                                FooString.toHexString(reference1s) + "]", null);
            println(tag, level, text + ": " + name + "(" + bytesLength + ")=[" +
                                FooString.toHexString(bytes) + "]", null);
        }
    }
}
