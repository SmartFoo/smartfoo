package com.smartfoo.android.core.platform;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooRun;

public class FooService
{
    private FooService()
    {
    }

    public static boolean startService(@NonNull Context context, @NonNull Intent intent)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(intent, "intent");
        
        ComponentName componentName = context.startService(intent);

        //noinspection UnnecessaryLocalVariable
        boolean started = (componentName != null);

        return started;
    }

    @NonNull
    public static String startToString(int start)
    {
        return switch (start) {
            case Service.START_STICKY_COMPATIBILITY -> "START_STICKY_COMPATIBILITY";
            case Service.START_STICKY -> "START_STICKY";
            case Service.START_NOT_STICKY -> "START_NOT_STICKY";
            case Service.START_REDELIVER_INTENT -> "START_REDELIVER_INTENT";
            default -> "UNKNOWN";
        } + '(' + start + ')';
    }
}
