package com.smartfoo.android.core.app;

/**
 * Configuration contract for the debug log activity.
 *
 * <p>Implementations persist these settings (e.g. in {@link android.content.SharedPreferences})
 * and expose them to {@link FooDebugActivity} so that log-size limits, email limits, and
 * file-logging state survive across sessions.</p>
 */
public interface FooDebugConfiguration
{
    /**
     * Returns the maximum log display size in kilobytes.
     *
     * @param defaultValue value to return if no preference has been stored
     * @return the stored limit, or {@code defaultValue}
     */
    int getDebugLogLimitKb(int defaultValue);

    /**
     * Persists the maximum log display size in kilobytes.
     *
     * @param value the new limit in kilobytes
     */
    void setDebugLogLimitKb(int value);

    /**
     * Returns the maximum log email attachment size in kilobytes.
     *
     * @param defaultValue value to return if no preference has been stored
     * @return the stored limit, or {@code defaultValue}
     */
    int getDebugLogEmailLimitKb(int defaultValue);

    /**
     * Persists the maximum log email attachment size in kilobytes.
     *
     * @param value the new limit in kilobytes
     */
    void setDebugLogEmailLimitKb(int value);

    /**
     * Returns {@code true} if debug logging is currently enabled.
     *
     * @return the current enabled state
     */
    boolean isDebugEnabled();

    /**
     * Sets whether debug logging is enabled and returns the previous value.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return the previous enabled state
     */
    boolean setDebugEnabled(boolean value);

    /**
     * Returns {@code true} if logging to a file is currently enabled.
     *
     * @return the current file-logging state
     */
    boolean getDebugToFileEnabled();

    /**
     * Sets whether logging to a file is enabled.
     *
     * @param enabled {@code true} to enable file logging, {@code false} to disable
     */
    void setDebugToFileEnabled(boolean enabled);
}
