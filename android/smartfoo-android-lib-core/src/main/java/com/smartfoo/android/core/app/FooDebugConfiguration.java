package com.smartfoo.android.core.app;

public interface FooDebugConfiguration
{
    int getDebugLogLimitKb(int defaultValue);

    void setDebugLogLimitKb(int value);

    int getDebugLogEmailLimitKb(int defaultValue);

    void setDebugLogEmailLimitKb(int value);

    boolean isDebugEnabled();

    boolean setDebugEnabled(boolean value);

    boolean getDebugToFileEnabled();

    void setDebugToFileEnabled(boolean enabled);
}
