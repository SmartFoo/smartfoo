package com.smartfoo.android.core.app;

/**
 * Marker interface that the application's {@link android.app.Application} class must implement
 * to provide a {@link FooDebugConfiguration} to {@link FooDebugActivity}.
 */
public interface FooDebugApplication
{
    /**
     * Returns the debug configuration for this application.
     *
     * @return the {@link FooDebugConfiguration}; must not be null
     */
    FooDebugConfiguration getFooDebugConfiguration();
}
