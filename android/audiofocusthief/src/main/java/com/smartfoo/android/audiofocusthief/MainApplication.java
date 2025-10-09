package com.smartfoo.android.audiofocusthief;

import android.Manifest;
import android.app.Application;
import android.app.NotificationManager;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;

import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusController;
import com.smartfoo.android.core.media.FooAudioFocusController.FooAudioFocusControllerCallbacks;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotification.Companion.ChannelInfo;
import com.smartfoo.android.core.notification.FooNotificationBuilder;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    private final FooAudioFocusController mAudioFocusController;
    private final FooAudioFocusControllerCallbacks mAudioFocusControllerCallbacks;
    private final FooListenerManager<FooAudioFocusControllerCallbacks> mAudioFocusControllerManager;

    private int mAudioFocusStreamType   = AudioManager.STREAM_MUSIC;
    private int mAudioFocusDurationHint = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;

    private FooNotification mNotification;
    private boolean         mIsAudioFocusThief;

    public MainApplication()
    {
        mAudioFocusController = FooAudioFocusController.getInstance();
        mAudioFocusControllerCallbacks = new FooAudioFocusControllerCallbacks()
        {
            @Override
            public void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
            {
                MainApplication.this.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
            }

            @Override
            public boolean onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
            {
                return MainApplication.this.onAudioFocusLost(audioFocusStreamType, audioFocusDurationHint, focusChange);
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

    public void attach(FooAudioFocusControllerCallbacks callbacks)
    {
        mAudioFocusControllerManager.attach(callbacks);
    }

    public void detach(FooAudioFocusControllerCallbacks callbacks)
    {
        mAudioFocusControllerManager.detach(callbacks);
    }

    public boolean isAudioFocusGained()
    {
        return mAudioFocusController.isAudioFocusGained();
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
        mAudioFocusController.setHashtag(getAudioFocusHashtag());
    }

    public boolean audioFocusOn()
    {
        mAudioFocusController.setHashtag(getAudioFocusHashtag());
        mAudioFocusController.audioFocusStart(this, mAudioFocusStreamType, mAudioFocusDurationHint, mAudioFocusControllerCallbacks);
        return true;
    }

    public boolean audioFocusOff()
    {
        mAudioFocusController.audioFocusStop(mAudioFocusControllerCallbacks);
        return true;
    }

    private void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusGained(audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint=" +
                      FooAudioUtils.audioFocusGainLossToString(audioFocusDurationHint) + ')');

        for (FooAudioFocusControllerCallbacks callbacks : mAudioFocusControllerManager.beginTraversing())
        {
            callbacks.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
        }
        mAudioFocusControllerManager.endTraversing();
    }

    private boolean onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusLost(…, audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint=" +
                      FooAudioUtils.audioFocusGainLossToString(audioFocusDurationHint) +
                      ", focusChange=" +
                      FooAudioUtils.audioFocusGainLossToString(focusChange) + ')');

        for (FooAudioFocusControllerCallbacks callbacks : mAudioFocusControllerManager.beginTraversing())
        {
            callbacks.onAudioFocusLost(audioFocusStreamType, audioFocusDurationHint, focusChange);
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
