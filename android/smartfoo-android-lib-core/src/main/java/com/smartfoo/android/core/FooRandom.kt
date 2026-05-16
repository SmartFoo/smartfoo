package com.smartfoo.android.core

import androidx.annotation.FloatRange
import java.util.Random

/**
 * Random number generation helpers with configurable minimum bounds.
 *
 * All results are normalized to the range [0.0, 1.0] or a caller-specified sub-range.
 */
object FooRandom {
    /**
     * @return double from 0.0 to 1.0
     */
    @JvmStatic
    @FloatRange(from = 0.0, to = 1.0)
    fun randomNormal(random: Random): Double {
        return randomNormal(random, 0.0)
    }

    /**
     * @param random  Random
     * @param minimum 1.0 returns 1.0, 0.5 returns 0.5 to 1.0, 0.0 returns 0.0 to 1.0
     * @return double from minimum to 1.0
     */
    @JvmStatic
    @FloatRange(from = 0.0, to = 1.0)
    fun randomNormal(random: Random, @FloatRange(from = 0.0, to = 1.0) minimum: Double): Double {
        return minimum + (1.0 - minimum) * random.nextDouble()
    }
}
