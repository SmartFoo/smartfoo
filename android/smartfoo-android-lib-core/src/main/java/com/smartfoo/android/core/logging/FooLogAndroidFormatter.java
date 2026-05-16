package com.smartfoo.android.core.logging;

/**
 * A {@link FooLogFormatter} that resolves process and thread IDs using the Android
 * {@link android.os.Process} API.
 *
 * <p>Use this formatter with {@link FooLogConsolePrinter} when running on a real Android device
 * or emulator where {@code android.os.Process} is available.</p>
 */
public class FooLogAndroidFormatter
        extends FooLogFormatter
{
    @Override
    protected int getPid()
    {
        return android.os.Process.myPid();
    }

    @Override
    protected int getTid()
    {
        return android.os.Process.myTid();
    }
}
