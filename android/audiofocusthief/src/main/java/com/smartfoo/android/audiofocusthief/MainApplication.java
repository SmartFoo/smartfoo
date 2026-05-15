package com.smartfoo.android.audiofocusthief;

import android.Manifest;
import android.app.Application;
import android.app.NotificationManager;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusController;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotification.Companion.ChannelInfo;
import com.smartfoo.android.core.notification.FooNotificationBuilder;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    private final FooAudioFocusController mAudioFocusController;
    private final FooAudioFocusController.Callbacks mAudioFocusControllerCallbacks;
    private final FooListenerManager<FooAudioFocusController.Callbacks> mAudioFocusControllerManager;

    @Nullable
    private FooAudioFocusController.FocusHandle mAudioFocusHandle;

    private FooNotification mNotification;

    private boolean         mIsAudioFocusThief;

    public MainApplication()
    {
        mAudioFocusController = FooAudioFocusController.getInstance();
        mAudioFocusControllerCallbacks = new FooAudioFocusController.Callbacks()
        {
            @Override
            public boolean onFocusGained(FooAudioFocusController ctrl, AudioFocusRequest request)
            {
                MainApplication.this.onAudioFocusGained(request);
                return false;
            }

            @Override
            public boolean onFocusLost(FooAudioFocusController ctrl, AudioFocusRequest request, int focusChange)
            {
                return MainApplication.this.onAudioFocusLost(request, focusChange);
            }
        };
        mAudioFocusControllerManager = new FooListenerManager<>(this);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    public boolean isNotificationOn()
    {
        return mNotification != null;
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public boolean notificationOn()
    {
        if (mNotification != null)
        {
            return false;
        }

        ChannelInfo CHANNEL_INFO = new ChannelInfo(
                "FOREGROUND_SERVICE_CHANNEL",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT,
                "Non-dismissible notifications for session status");

        FooNotification.createNotificationChannel(this, CHANNEL_INFO);

        int FOREGROUND_SERVICE_TYPE = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

        FooNotificationBuilder notificationBuilder = new FooNotificationBuilder(this, CHANNEL_INFO.getId())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_android_white_24dp)
                .setContentTitle("Running In The Background")
                .setContentText("Tap to foreground the app.")
                .setContentIntentActivity(100, MainActivity.class);

        mNotification = new FooNotification(100, FOREGROUND_SERVICE_TYPE, notificationBuilder);
        mNotification.show(this);

        return true;
    }

    public boolean notificationOff()
    {
        if (mNotification == null)
        {
            return false;
        }

        mNotification.cancel(this);
        mNotification = null;

        return true;
    }

    //
    //
    //

    public void attach(FooAudioFocusController.Callbacks callbacks)
    {
        mAudioFocusControllerManager.attach(callbacks);
    }

    public void detach(FooAudioFocusController.Callbacks callbacks)
    {
        mAudioFocusControllerManager.detach(callbacks);
    }

    public boolean isAudioFocusGained()
    {
        return mAudioFocusHandle != null;
    }

    public String getAudioFocusHashtag()
    {
        return "#AUDIOFOCUS_" + (mIsAudioFocusThief ? "THIEF" : "NICE");
    }

    public boolean getIsAudioFocusThief()
    {
        return mIsAudioFocusThief;
    }

    public void setIsAudioFocusThief(boolean thief)
    {
        mIsAudioFocusThief = thief;
    }

    public boolean audioFocusOn()
    {
        if (mAudioFocusHandle != null)
        {
            return false;
        }
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mAudioFocusHandle = mAudioFocusController.acquire(
                this,
                audioAttributes,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                mAudioFocusControllerCallbacks,
                getAudioFocusHashtag());
        return mAudioFocusHandle != null;
    }

    public boolean audioFocusOff()
    {
        FooAudioFocusController.FocusHandle handle = mAudioFocusHandle;
        mAudioFocusHandle = null;
        if (handle == null)
        {
            return false;
        }
        handle.release();
        return true;
    }

    private void onAudioFocusGained(AudioFocusRequest request)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusGained(focusGain=" +
                      FooAudioUtils.audioFocusGainLossToString(request.getFocusGain()) + ')');

        for (FooAudioFocusController.Callbacks callbacks : mAudioFocusControllerManager.beginTraversing())
        {
            if (callbacks.onFocusGained(mAudioFocusController, request)) break;
        }
        mAudioFocusControllerManager.endTraversing();
    }

    private boolean onAudioFocusLost(AudioFocusRequest request, int focusChange)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusLost(focusChange=" +
                      FooAudioUtils.audioFocusGainLossToString(focusChange) + ')');

        for (FooAudioFocusController.Callbacks callbacks : mAudioFocusControllerManager.beginTraversing())
        {
            if (callbacks.onFocusLost(mAudioFocusController, request, focusChange)) break;
        }
        mAudioFocusControllerManager.endTraversing();

        if (mIsAudioFocusThief)
        {
            audioFocusOn();
            return true;
        }

        return false;
    }
}
