package com.smartfoo.android.core.media;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler.Callback;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver.SystemSettingsContentObserver.OnSystemSettingsChangedCallbacks;
import com.smartfoo.android.core.platform.FooHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FooAudioStreamVolumeObserver
{
    private static final String TAG = FooLog.TAG(FooAudioStreamVolumeObserver.class);

    public interface OnAudioStreamVolumeChangedCallbacks
    {
        void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent);
    }

    static class SystemSettingsContentObserver
    {
        interface OnSystemSettingsChangedCallbacks
        {
            void onSystemSettingsChanged(boolean selfChange);
        }

        private final ContentResolver mContentResolver;
        private final FooHandler      mHandler;

        private ContentObserver mContentObserver;

        private SystemSettingsContentObserver(@NonNull Context context)
        {
            mContentResolver = context.getContentResolver();
            mHandler = new FooHandler();
        }

        private void start(@NonNull final OnSystemSettingsChangedCallbacks callbacks)
        {
            if (mContentObserver != null)
            {
                return;
            }

            mContentObserver = new ContentObserver(mHandler)
            {
                @Override
                public void onChange(boolean selfChange, Uri uri)
                {
                    callbacks.onSystemSettingsChanged(selfChange);
                }
            };

            mContentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, mContentObserver);
        }

        private void stop()
        {
            if (mContentObserver == null)
            {
                return;
            }

            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    private final Context                                                               mContext;
    private final AudioManager                                                          mAudioManager;
    private final Map<Integer, FooListenerManager<OnAudioStreamVolumeChangedCallbacks>> mAudioStreamTypeToListenerManagers;
    private final Map<Integer, Integer>                                                 mAudioStreamTypeToLastVolume;
    private final FooHandler                                                            mHandler;

    /**
     * <ul>
     * <li>msg.arg1: audioStreamType</li>
     * <li>msg.arg2: volume</li>
     * <li>msg.obj: ?</li>
     * </ul>
     */
    private static final int MESSAGE_VOLUME_CHANGED = 100;

    private SystemSettingsContentObserver mSystemSettingsContentObserver;
    private int                           mDelayedMilliseconds;

    public FooAudioStreamVolumeObserver(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioStreamTypeToListenerManagers = new LinkedHashMap<>();
        mAudioStreamTypeToLastVolume = new LinkedHashMap<>();
        mHandler = new FooHandler(new Callback()
        {
            @Override
            public boolean handleMessage(Message msg)
            {
                return FooAudioStreamVolumeObserver.this.handleMessage(msg);
            }
        });
    }

    public void setDelayedMilliseconds(int delayedMilliseconds)
    {
        mDelayedMilliseconds = delayedMilliseconds;
    }

    public void attach(int audioStreamType, @NonNull OnAudioStreamVolumeChangedCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        FooListenerManager<OnAudioStreamVolumeChangedCallbacks> listenerManager = mAudioStreamTypeToListenerManagers
                .get(audioStreamType);
        if (listenerManager == null)
        {
            listenerManager = new FooListenerManager<>(this);
            mAudioStreamTypeToListenerManagers.put(audioStreamType, listenerManager);
        }

        int volume = mAudioManager.getStreamVolume(audioStreamType);
        mAudioStreamTypeToLastVolume.put(audioStreamType, volume);

        listenerManager.attach(callbacks);

        if (mAudioStreamTypeToListenerManagers.size() == 1)
        {
            if (mSystemSettingsContentObserver == null)
            {
                mSystemSettingsContentObserver = new SystemSettingsContentObserver(mContext);
                mSystemSettingsContentObserver.start(new OnSystemSettingsChangedCallbacks()
                {
                    @Override
                    public void onSystemSettingsChanged(boolean selfChange)
                    {
                        //FooLog.v(TAG, "onSystemSettingsChanged(selfChange=" + selfChange + ')');
                        for (Entry<Integer, FooListenerManager<OnAudioStreamVolumeChangedCallbacks>> entry :
                                mAudioStreamTypeToListenerManagers.entrySet())
                        {
                            int audioStreamType = entry.getKey();
                            //FooLog.v(TAG, "onSystemSettingsChanged: audioStreamType == " + audioStreamType);
                            int volume = mAudioManager.getStreamVolume(audioStreamType);
                            //FooLog.v(TAG, "onSystemSettingsChanged: volume == " + volume);
                            Integer lastVolume = mAudioStreamTypeToLastVolume.get(audioStreamType);
                            //FooLog.v(TAG, "onSystemSettingsChanged: lastVolume == " + lastVolume);
                            if (volume == lastVolume)
                            {
                                continue;
                            }

                            //FooLog.d(TAG, "onSystemSettingsChanged: MESSAGE_VOLUME_CHANGED audioStreamType volume");
                            if (mDelayedMilliseconds > 0)
                            {
                                mHandler.removeMessages(MESSAGE_VOLUME_CHANGED);
                            }
                            mHandler.obtainAndSendMessageDelayed(MESSAGE_VOLUME_CHANGED, audioStreamType, volume, mDelayedMilliseconds);
                        }
                    }
                });
            }
        }
    }

    public void detach(int audioStreamType, @NonNull OnAudioStreamVolumeChangedCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        FooListenerManager<OnAudioStreamVolumeChangedCallbacks> listenerManager = mAudioStreamTypeToListenerManagers
                .get(audioStreamType);
        if (listenerManager == null)
        {
            return;
        }

        listenerManager.detach(callbacks);

        if (listenerManager.size() == 0)
        {
            mAudioStreamTypeToListenerManagers.remove(audioStreamType);
        }

        if (mAudioStreamTypeToListenerManagers.size() == 0)
        {
            if (mSystemSettingsContentObserver != null)
            {
                mSystemSettingsContentObserver.stop();
                mSystemSettingsContentObserver = null;
            }
        }
    }

    private boolean handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_VOLUME_CHANGED:
                onAudioStreamVolumeChanged(msg);
                break;
        }
        return false;
    }

    private void onAudioStreamVolumeChanged(Message msg)
    {
        int audioStreamType = msg.arg1;
        //FooLog.v(TAG, "onAudioStreamVolumeChanged: audioStreamType == " + audioStreamType);
        int volume = msg.arg2;
        //FooLog.v(TAG, "onAudioStreamVolumeChanged: volume == " + volume);

        mAudioStreamTypeToLastVolume.put(audioStreamType, volume);

        FooListenerManager<OnAudioStreamVolumeChangedCallbacks> listenerManager =
                mAudioStreamTypeToListenerManagers.get(audioStreamType);
        if (listenerManager == null)
        {
            return;
        }

        int volumeMax = mAudioManager.getStreamMaxVolume(audioStreamType);
        int volumePercent = Math.round(volume / (float) volumeMax * 100f);

        for (OnAudioStreamVolumeChangedCallbacks callbacks : listenerManager.beginTraversing())
        {
            callbacks.onAudioStreamVolumeChanged(audioStreamType, volume, volumeMax, volumePercent);
        }
        listenerManager.endTraversing();
    }
}
