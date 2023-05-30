package com.smartfoo.android.core.platform;

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
}
