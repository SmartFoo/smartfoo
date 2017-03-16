package com.smartfoo.android.core;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.logging.FooLogAdbPrinter;
import com.smartfoo.android.core.logging.FooLogConsolePrinter;
import com.smartfoo.android.core.logging.FooLogPrinter;
import com.smartfoo.android.core.logging.FooLogUnixJavaFormatter;

public class FooTest
{
    public enum TestType
    {
        Junit,
        Android
    }

    public static void initialize(TestType testType)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(testType, "testType");

        FooLog.setEnabled(true);

        FooLog.clearPrinters();

        FooLogPrinter logPrinter;
        switch (testType)
        {
            case Android:
                logPrinter = FooLogAdbPrinter.getInstance();
                break;
            case Junit:
                logPrinter = FooLogConsolePrinter.getInstance(new FooLogUnixJavaFormatter());
                break;
            default:
                throw new IllegalArgumentException("testType must be one of TestType.*");
        }
        FooLog.addPrinter(logPrinter);
    }

    private FooTest()
    {
    }
}
