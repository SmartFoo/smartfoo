package com.smartfoo.android.core.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

/**
 * Inspiration:
 * http://blog.urvatechlabs.com/detect-programatically-if-headphone-or-bluetooth-headsets-attached-with-android-phone/
 */
public class FooWiredHeadsetConnectionListener
{
    public interface OnWiredHeadsetConnectionCallbacks
    {
        void onWiredHeadsetConnected(String name, boolean hasMicrophone);

        void onWiredHeadsetDisconnected(String name, boolean hasMicrophone);
    }

    private final WiredHeadsetBroadcastReceiver                         mWiredHeadsetBroadcastReceiver;
    private final FooListenerManager<OnWiredHeadsetConnectionCallbacks> mListenerManager;

    public FooWiredHeadsetConnectionListener(@NonNull Context applicationContext)
    {
        mWiredHeadsetBroadcastReceiver = new WiredHeadsetBroadcastReceiver(applicationContext);
        mListenerManager = new FooListenerManager<>();
    }

    public boolean isWiredHeadsetConnected()
    {
        return mWiredHeadsetBroadcastReceiver.isWiredHeadsetConnected();
    }

    public boolean isStarted()
    {
        return mWiredHeadsetBroadcastReceiver.isStarted();
    }

    public void attach(@NonNull OnWiredHeadsetConnectionCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);

        if (isStarted())
        {
            return;
        }

        mWiredHeadsetBroadcastReceiver.start(new OnWiredHeadsetConnectionCallbacks()
        {
            @Override
            public void onWiredHeadsetConnected(String name, boolean hasMicrophone)
            {
                for (OnWiredHeadsetConnectionCallbacks callbacks : mListenerManager.beginTraversing())
                {
                    callbacks.onWiredHeadsetConnected(name, hasMicrophone);
                }
                mListenerManager.endTraversing();
            }

            @Override
            public void onWiredHeadsetDisconnected(String name, boolean hasMicrophone)
            {
                for (OnWiredHeadsetConnectionCallbacks callbacks : mListenerManager.beginTraversing())
                {
                    callbacks.onWiredHeadsetDisconnected(name, hasMicrophone);
                }
                mListenerManager.endTraversing();
            }
        });
    }

    public void detach(@NonNull OnWiredHeadsetConnectionCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);

        if (mListenerManager.isEmpty())
        {
            mWiredHeadsetBroadcastReceiver.stop();
        }
    }

    private static class WiredHeadsetBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(WiredHeadsetBroadcastReceiver.class);

        private final Context mApplicationContext;

        private final Object mSyncLock = new Object();

        private boolean mIsStarted;

        private OnWiredHeadsetConnectionCallbacks mCallbacks;

        private boolean mIsWiredHeadsetConnected;

        public WiredHeadsetBroadcastReceiver(Context applicationContext)
        {
            mApplicationContext = applicationContext;
        }

        public boolean isWiredHeadsetConnected()
        {
            return mIsWiredHeadsetConnected;
        }

        public boolean isStarted()
        {
            synchronized (mSyncLock)
            {
                return mIsStarted;
            }
        }

        public void start(@NonNull OnWiredHeadsetConnectionCallbacks callbacks)
        {
            FooLog.v(TAG, "+start(...)");
            synchronized (mSyncLock)
            {
                if (!mIsStarted)
                {
                    mIsStarted = true;

                    mCallbacks = callbacks;

                    IntentFilter intentFilter = new IntentFilter();
                    if (Build.VERSION.SDK_INT >= 21)
                    {
                        intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
                    }
                    else
                    {
                        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
                    }
                    mApplicationContext.registerReceiver(this, intentFilter);
                }
            }
            FooLog.v(TAG, "-start(...)");
        }

        public void stop()
        {
            FooLog.v(TAG, "+stop()");
            synchronized (mSyncLock)
            {
                if (mIsStarted)
                {
                    mIsStarted = false;

                    mApplicationContext.unregisterReceiver(this);
                }
            }
            FooLog.v(TAG, "-stop()");
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            FooLog.v(TAG, "onReceive: action=" + FooString.quote(action));

            switch (action)
            {
                case AudioManager.ACTION_HEADSET_PLUG:
                {
                    int state = intent.getIntExtra("state", 0); // 0 for unplugged, 1 for plugged
                    String name = intent.getStringExtra("name"); // Headset type, human readable string
                    int microphone = intent.getIntExtra("microphone", 0); // 1 if headset has a microphone, 0 otherwise
                    FooLog.v(TAG, "onReceive: state=" + state);
                    FooLog.v(TAG, "onReceive: name=" + FooString.quote(name));
                    FooLog.v(TAG, "onReceive: microphone=" + microphone);

                    synchronized (mSyncLock)
                    {
                        mIsWiredHeadsetConnected = (state == 1);
                        if (mIsWiredHeadsetConnected)
                        {
                            mCallbacks.onWiredHeadsetConnected(name, microphone == 1);
                        }
                        else
                        {
                            mCallbacks.onWiredHeadsetDisconnected(name, microphone == 1);
                        }
                    }

                    break;
                }
            }
        }
    }
}
