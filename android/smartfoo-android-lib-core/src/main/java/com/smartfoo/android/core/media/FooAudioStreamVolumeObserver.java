package com.smartfoo.android.core.media;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
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

    private SystemSettingsContentObserver mSystemSettingsContentObserver;

    public FooAudioStreamVolumeObserver(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioStreamTypeToListenerManagers = new LinkedHashMap<>();
        mAudioStreamTypeToLastVolume = new LinkedHashMap<>();
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
                        for (Entry<Integer, FooListenerManager<OnAudioStreamVolumeChangedCallbacks>> entry :
                                mAudioStreamTypeToListenerManagers.entrySet())
                        {
                            int audioStreamType = entry.getKey();

                            int volume = mAudioManager.getStreamVolume(audioStreamType);

                            Integer lastVolume = mAudioStreamTypeToLastVolume.get(audioStreamType);

                            if (volume == lastVolume)
                            {
                                continue;
                            }

                            mAudioStreamTypeToLastVolume.put(audioStreamType, volume);

                            int volumeMax = mAudioManager.getStreamMaxVolume(audioStreamType);
                            int volumePercent = Math.round(volume / (float) volumeMax * 100f);

                            FooListenerManager<OnAudioStreamVolumeChangedCallbacks> listenerManager = entry.getValue();
                            for (OnAudioStreamVolumeChangedCallbacks callbacks : listenerManager.beginTraversing())
                            {
                                callbacks.onAudioStreamVolumeChanged(audioStreamType, volume, volumeMax, volumePercent);
                            }
                            listenerManager.endTraversing();
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
}
