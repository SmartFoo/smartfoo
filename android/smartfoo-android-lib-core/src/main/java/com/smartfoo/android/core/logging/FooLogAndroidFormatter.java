package com.smartfoo.android.core.logging;

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
