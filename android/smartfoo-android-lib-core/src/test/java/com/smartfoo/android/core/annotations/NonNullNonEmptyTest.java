package com.smartfoo.android.core.annotations;

import com.smartfoo.android.core.FooAssert;
import com.smartfoo.android.core.FooRun;

import org.junit.Test;

public class NonNullNonEmptyTest
{
    @Test
    public void throwIfNull()
            throws Exception
    {
        FooAssert.assertThrows(new Runnable()
        {
            @Override
            public void run()
            {
                FooRun.throwIfNull(null, null);
            }
        }, IllegalArgumentException.class);
        FooAssert.assertThrows(new Runnable()
        {
            @Override
            public void run()
            {
                FooRun.throwIfNull(null, "");
            }
        }, IllegalArgumentException.class);
    }

    @Test
    public void throwIfNullOrEmpty()
            throws Exception
    {
        FooAssert.assertThrows(new Runnable()
        {
            @Override
            public void run()
            {
                FooRun.throwIfNull(null, null);
            }
        }, IllegalArgumentException.class);
    }

    @Test
    public void toNonNull()
            throws Exception
    {
        FooRun.toNonNull(null, "null");
    }

    @Test
    public void toNonNullNonEmpty()
            throws Exception
    {
        FooAssert.assertThrows(new Runnable()
        {
            @Override
            public void run()
            {
                FooRun.toNonNullNonEmpty(null, null);
            }
        }, IllegalArgumentException.class);
    }
}