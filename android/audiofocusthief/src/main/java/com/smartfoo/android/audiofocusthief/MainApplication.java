package com.smartfoo.android.audiofocusthief;

import android.app.Application;
import android.media.AudioManager;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusListener;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusConfiguration;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    private final FooListenerManager<FooAudioFocusListenerCallbacks> mAudioFocusListenerCallbacksAttached;
    private final FooAudioFocusListenerCallbacks                     mAudioFocusListenerCallbacks;

    private String mAudioFocusHashtag;
    private int mAudioFocusStreamType   = AudioManager.STREAM_MUSIC;
    private int mAudioFocusDurationHint = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;

    private FooNotification            mNotification;
    private FooAudioFocusConfiguration mAudioFocusConfiguration;
    private FooAudioFocusListener      mAudioFocusListener;

    public MainApplication()
    {
        mAudioFocusListenerCallbacksAttached = new FooListenerManager<>();
        mAudioFocusListenerCallbacks = new FooAudioFocusListenerCallbacks()
        {
            @Override
            public void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
            {
                MainApplication.this.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
            }

            @Override
            public FooAudioFocusConfiguration onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint)
            {
                return MainApplication.this.onAudioFocusLost(audioFocusStreamType, audioFocusDurationHint);
            }
        };
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
        mAudioFocusListenerCallbacksAttached.attach(callbacks);
    }

    public void detach(FooAudioFocusListenerCallbacks callbacks)
    {
        mAudioFocusListenerCallbacksAttached.detach(callbacks);
    }

    public boolean isAudioFocusOn()
    {
        return mAudioFocusListener != null;
    }

    public boolean getIsAudioFocusThief()
    {
        return mAudioFocusConfiguration != null;
    }

    public void setIsAudioFocusThief(boolean thief)
    {
        if (thief)
        {
            mAudioFocusConfiguration = new FooAudioFocusConfiguration()
            {
                @Override
                public int getAudioFocusStreamType()
                {
                    return mAudioFocusStreamType;
                }

                @Override
                public int getAudioFocusDurationHint()
                {
                    return mAudioFocusDurationHint;
                }
            };
        }
        else
        {
            mAudioFocusConfiguration = null;
        }
    }

    public boolean audioFocusOn(final String audioFocusHashtag)
    {
        if (mAudioFocusListener != null)
        {
            return false;
        }

        mAudioFocusHashtag = audioFocusHashtag;

        mAudioFocusListener = new FooAudioFocusListener(this, mAudioFocusHashtag);
        mAudioFocusListener.audioFocusStart(mAudioFocusStreamType, mAudioFocusDurationHint, mAudioFocusListenerCallbacks);

        return true;
    }

    public boolean audioFocusOff()
    {
        if (mAudioFocusListener == null)
        {
            return false;
        }

        mAudioFocusListener.audioFocusStop();
        mAudioFocusListener = null;

        return true;
    }

    private void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
    {
        FooLog.e(TAG, mAudioFocusHashtag +
                      " onAudioFocusGained(" + audioFocusStreamType + ", " + audioFocusDurationHint + ')');

        for (FooAudioFocusListenerCallbacks callbacks : mAudioFocusListenerCallbacksAttached.beginTraversing())
        {
            callbacks.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
        }
        mAudioFocusListenerCallbacksAttached.endTraversing();
    }

    private FooAudioFocusConfiguration onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint)
    {
        FooLog.e(TAG, mAudioFocusHashtag +
                      " onAudioFocusLost(" + audioFocusStreamType + ", " + audioFocusDurationHint + ')');

        for (FooAudioFocusListenerCallbacks callbacks : mAudioFocusListenerCallbacksAttached.beginTraversing())
        {
            callbacks.onAudioFocusLost(audioFocusStreamType, audioFocusDurationHint);
        }
        mAudioFocusListenerCallbacksAttached.endTraversing();

        return mAudioFocusConfiguration;
    }
}
