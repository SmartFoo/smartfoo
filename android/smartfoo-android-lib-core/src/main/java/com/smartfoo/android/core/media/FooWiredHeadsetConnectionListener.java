package com.smartfoo.android.core.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

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

    private final FooListenerManager<OnWiredHeadsetConnectionCallbacks> mListenerManager;
    private final WiredHeadsetBroadcastReceiver                         mWiredHeadsetBroadcastReceiver;

    public FooWiredHeadsetConnectionListener(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mListenerManager = new FooListenerManager<>(this);
        mWiredHeadsetBroadcastReceiver = new WiredHeadsetBroadcastReceiver(context);
    }

    public boolean isWiredHeadsetConnected()
    {
        return mWiredHeadsetBroadcastReceiver.isWiredHeadsetConnected();
    }

    public void attach(@NonNull OnWiredHeadsetConnectionCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.attach(callbacks);
        if (mListenerManager.size() == 1 && !mWiredHeadsetBroadcastReceiver.isStarted())
        {
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
    }

    public void detach(@NonNull OnWiredHeadsetConnectionCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.detach(callbacks);
        if (mListenerManager.size() == 0 && mWiredHeadsetBroadcastReceiver.isStarted())
        {
            mWiredHeadsetBroadcastReceiver.stop();
        }
    }

    private static class WiredHeadsetBroadcastReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = FooLog.TAG(WiredHeadsetBroadcastReceiver.class);

        private final Context mContext;
        private final Object  mSyncLock;

        private boolean                           mIsStarted;
        private OnWiredHeadsetConnectionCallbacks mCallbacks;
        private boolean                           mIsWiredHeadsetConnected;

        public WiredHeadsetBroadcastReceiver(@NonNull Context context)
        {
            mContext = context;
            mSyncLock = new Object();
        }

        public boolean isWiredHeadsetConnected()
        {
            synchronized (mSyncLock)
            {
                return mIsWiredHeadsetConnected;
            }
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
                    mContext.registerReceiver(this, intentFilter);
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

                    mContext.unregisterReceiver(this);
                }
            }
            FooLog.v(TAG, "-stop()");
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            FooLog.v(TAG, "onReceive: intent == " + FooPlatformUtils.toString(intent));
            String action = intent.getAction();
            switch (action)
            {
                case AudioManager.ACTION_HEADSET_PLUG:
                {
                    int state = intent.getIntExtra("state", 0); // 0 for unplugged, 1 for plugged
                    String name = intent.getStringExtra("name"); // Headset type, human readable string
                    int microphone = intent.getIntExtra("microphone", 0); // 1 if headset has a microphone, 0 otherwise
                    FooLog.v(TAG, "onReceive: state == " + state);
                    FooLog.v(TAG, "onReceive: name == " + FooString.quote(name));
                    FooLog.v(TAG, "onReceive: microphone == " + microphone);

                    synchronized (mSyncLock)
                    {
                        mIsWiredHeadsetConnected = (state == 1);
                        if (mCallbacks != null)
                        {
                            if (mIsWiredHeadsetConnected)
                            {
                                mCallbacks.onWiredHeadsetConnected(name, microphone == 1);
                            }
                            else
                            {
                                mCallbacks.onWiredHeadsetDisconnected(name, microphone == 1);
                            }
                        }
                    }

                    break;
                }
            }
        }
    }
}
