package com.smartfoo.android.core;

import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;

import java.util.Random;

public class FooRandom
{
    /**
     * @return double from 0.0 to 1.0
     */
    @FloatRange(from = 0.0, to = 1.0)
    public static double randomNormal(@NonNull Random random)
    {
        return randomNormal(random, 0.0);
    }

    /**
     * @param random  Random
     * @param minimum 1.0 returns 1.0, 0.5 returns 0.5 to 1.0, 0.0 returns 0.0 to 1.0
     * @return double from minimum to 1.0
     */
    @FloatRange(from = 0.0, to = 1.0)
    public static double randomNormal(@NonNull Random random, @FloatRange(from = 0.0, to = 1.0) double minimum)
    {
        return minimum + (1.0 - minimum) * random.nextDouble();
    }
}
