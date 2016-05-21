package com.smartfoo.android.core.platform;

public class FooIncrementingIntegerValue
{
    private int mNextMessageCode = 0;

    public int getNextMessageCode()
    {
        return mNextMessageCode++;
    }
}
