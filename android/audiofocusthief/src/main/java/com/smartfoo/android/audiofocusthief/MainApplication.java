package com.smartfoo.android.audiofocusthief;

import android.app.Application;
import android.media.AudioManager;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusListener;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    private final FooAudioFocusListener                              mAudioFocusListener;
    private final FooAudioFocusListenerCallbacks                     mAudioFocusListenerCallbacks;
    private final FooListenerManager<FooAudioFocusListenerCallbacks> mAudioFocusListenerManager;

    private int mAudioFocusStreamType   = AudioManager.STREAM_MUSIC;
    private int mAudioFocusDurationHint = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;

    private FooNotification mNotification;
    private boolean         mIsAudioFocusThief;

    public MainApplication()
    {
        mAudioFocusListener = FooAudioFocusListener.getInstance();
        mAudioFocusListenerCallbacks = new FooAudioFocusListenerCallbacks()
        {
            @Override
            public void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
            {
                MainApplication.this.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
            }

            @Override
            public boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
            {
                return MainApplication.this.onAudioFocusLost(audioFocusListener, audioFocusStreamType, audioFocusDurationHint, focusChange);
            }
        };
        mAudioFocusListenerManager = new FooListenerManager<>(this);
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

    public boolean notificationOn()
    {
        if (mNotification != null)
        {
            return false;
        }

        FooNotificationBuilder notificationBuilder = new FooNotificationBuilder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_android_white_24dp)
                .setContentTitle("Running In The Background")
                .setContentText("Tap to foreground the app.")
                .setContentIntentActivity(100, MainActivity.class);

        mNotification = new FooNotification(100, notificationBuilder);
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

    public void attach(FooAudioFocusListenerCallbacks callbacks)
    {
        mAudioFocusListenerManager.attach(callbacks);
    }

    public void detach(FooAudioFocusListenerCallbacks callbacks)
    {
        mAudioFocusListenerManager.detach(callbacks);
    }

    public boolean isAudioFocusGained()
    {
        return mAudioFocusListener.isAudioFocusGained();
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
        mAudioFocusListener.setHashtag(getAudioFocusHashtag());
    }

    public boolean audioFocusOn()
    {
        mAudioFocusListener.setHashtag(getAudioFocusHashtag());
        mAudioFocusListener.audioFocusStart(this, mAudioFocusStreamType, mAudioFocusDurationHint, mAudioFocusListenerCallbacks);
        return true;
    }

    public boolean audioFocusOff()
    {
        mAudioFocusListener.audioFocusStop(mAudioFocusListenerCallbacks);
        return true;
    }

    private void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusGained(audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint=" +
                      FooAudioUtils.audioFocusToString(audioFocusDurationHint) + ')');

        for (FooAudioFocusListenerCallbacks callbacks : mAudioFocusListenerManager.beginTraversing())
        {
            callbacks.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
        }
        mAudioFocusListenerManager.endTraversing();
    }

    private boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusLost(…, audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint=" +
                      FooAudioUtils.audioFocusToString(audioFocusDurationHint) +
                      ", focusChange=" +
                      FooAudioUtils.audioFocusToString(focusChange) + ')');

        for (FooAudioFocusListenerCallbacks callbacks : mAudioFocusListenerManager.beginTraversing())
        {
            callbacks.onAudioFocusLost(audioFocusListener, audioFocusStreamType, audioFocusDurationHint, focusChange);
        }
        mAudioFocusListenerManager.endTraversing();

        if (mIsAudioFocusThief)
        {
            audioFocusOn();
            return true;
        }

        return false;
    }
}
