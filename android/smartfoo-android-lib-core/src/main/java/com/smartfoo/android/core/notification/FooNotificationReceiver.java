package com.smartfoo.android.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

public class FooNotificationReceiver
        extends BroadcastReceiver
{
    private static final String TAG = FooLog.TAG(FooNotificationReceiver.class);

    public interface FooNotificationReceiverCallbacks
    {
        void onFooNotificationReceived(@NonNull Intent intent);
    }

    private static final FooListenerManager<FooNotificationReceiverCallbacks> mListenerManager;

    static
    {
        mListenerManager = new FooListenerManager<>(FooNotificationReceiver.class);
    }

    public static void attach(@NonNull FooNotificationReceiverCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
    }

    public static void detach(@NonNull FooNotificationReceiverCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            FooLog.i(TAG, "+onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')');
            for (FooNotificationReceiverCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onFooNotificationReceived(intent);
            }
            mListenerManager.endTraversing();
        }
        finally
        {
            FooLog.i(TAG, "-onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')');
        }
    }
}
