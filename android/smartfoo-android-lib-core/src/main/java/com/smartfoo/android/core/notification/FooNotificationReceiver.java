package com.smartfoo.android.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

public class FooNotificationReceiver
        extends BroadcastReceiver
{
    private static final String TAG = FooLog.TAG(FooNotificationReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            FooLog.i(TAG, "+onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')');

            // ...
        }
        finally
        {
            FooLog.i(TAG, "-onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')');
        }
    }
}
