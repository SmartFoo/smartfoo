package com.smartfoo.android.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class FooRandomTest
{
    private Random mRandom;

    @Before
    public void setup()
    {
        mRandom = new Random();
    }

    @Test
    public void randomNormal()
            throws Exception
    {
        double result = FooRandom.randomNormal(mRandom);
        Assert.assertFalse("result < 0.0", result < 0.0);
        Assert.assertFalse("result > 1.0", result > 1.0);
    }

    @Test
    public void randomNormalWeighted()
            throws Exception
    {
        int count = 10;
        for (int i = 0; i <= count; i++)
        {
            double weight = 1.0 - (i / (double) count);
            double result = FooRandom.randomNormal(mRandom, weight);
            String message = "result < " + weight;
            Assert.assertFalse(message, result < weight);
        }
    }
}