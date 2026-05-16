package com.smartfoo.android.core.platform;

/**
 * Thread-unsafe monotonically incrementing integer counter.
 *
 * <p>Useful for generating unique request codes, message IDs, or any context
 * where a simple auto-incrementing sequence is needed within a single thread or
 * when external synchronisation is provided by the caller.</p>
 */
public class FooIncrementingIntegerValue
{
    private int mNextMessageCode = 0;

    /**
     * Returns the current counter value and then increments it.
     *
     * @return the value before incrementing
     */
    public int getNextMessageCode()
    {
        return mNextMessageCode++;
    }
}
