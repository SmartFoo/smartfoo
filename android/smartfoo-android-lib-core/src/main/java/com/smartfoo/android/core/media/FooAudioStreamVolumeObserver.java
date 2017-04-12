package com.smartfoo.android.core.media;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings.System;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.collections.FooLongSparseArray;
import com.smartfoo.android.core.platform.FooHandler;

public class FooAudioStreamVolumeObserver
{
    public interface OnAudioStreamVolumeChangedListener
    {
        void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent);
    }

    private static class AudioStreamVolumeContentObserver
            extends ContentObserver
    {
        private final AudioManager                       mAudioManager;
        private final int                                mAudioStreamType;
        private final OnAudioStreamVolumeChangedListener mListener;

        private int mLastVolume;

        public AudioStreamVolumeContentObserver(
                @NonNull Handler handler,
                @NonNull AudioManager audioManager,
                int audioStreamType,
                @NonNull OnAudioStreamVolumeChangedListener listener)
        {
            super(handler);

            mAudioManager = audioManager;
            mAudioStreamType = audioStreamType;
            mListener = listener;

            mLastVolume = mAudioManager.getStreamVolume(mAudioStreamType);
        }

        @Override
        public void onChange(boolean selfChange)
        {
            int volume = mAudioManager.getStreamVolume(mAudioStreamType);

            if (volume != mLastVolume)
            {
                mLastVolume = volume;

                int volumeMax = mAudioManager.getStreamMaxVolume(mAudioStreamType);
                int volumePercent = Math.round(volume / (float) volumeMax * 100f);

                mListener.onAudioStreamVolumeChanged(mAudioStreamType, volume, volumeMax, volumePercent);
            }
        }
    }

    private final AudioManager                                                                        mAudioManager;
    private final ContentResolver                                                                     mContentResolver;
    private final FooHandler                                                                          mHandler;
    private final OnAudioStreamVolumeChangedListener                                                  mAudioStreamVolumeChangedListener;
    private final FooLongSparseArray<AudioStreamVolumeContentObserver>                                mAudioStreamTypeToAudioStreamVolumeContentObservers;
    private final FooLongSparseArray<FooListenerAutoStartManager<OnAudioStreamVolumeChangedListener>> mAudioStreamTypeToListenerManagers;

    public FooAudioStreamVolumeObserver(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mContentResolver = context.getContentResolver();
        mHandler = new FooHandler();
        mAudioStreamVolumeChangedListener = new OnAudioStreamVolumeChangedListener()
        {
            @Override
            public void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent)
            {
                FooAudioStreamVolumeObserver.this.onAudioStreamVolumeChanged(audioStreamType, volume, volumeMax, volumePercent);
            }
        };
        mAudioStreamTypeToAudioStreamVolumeContentObservers = new FooLongSparseArray<>();
        mAudioStreamTypeToListenerManagers = new FooLongSparseArray<>();
    }

    public void attach(final int audioStreamType, @NonNull final OnAudioStreamVolumeChangedListener listener)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(listener, "listener");
        FooListenerAutoStartManager<OnAudioStreamVolumeChangedListener> listenerManager = mAudioStreamTypeToListenerManagers
                .get(audioStreamType);
        if (listenerManager == null)
        {
            listenerManager = new FooListenerAutoStartManager<>();
            listenerManager.attach(new FooListenerAutoStartManagerCallbacks()
            {
                @Override
                public void onFirstAttach()
                {
                    AudioStreamVolumeContentObserver audioStreamVolumeContentObserver = mAudioStreamTypeToAudioStreamVolumeContentObservers
                            .get(audioStreamType);
                    if (audioStreamVolumeContentObserver == null)
                    {
                        audioStreamVolumeContentObserver = new AudioStreamVolumeContentObserver(mHandler, mAudioManager, audioStreamType, mAudioStreamVolumeChangedListener);
                        mContentResolver.registerContentObserver(System.CONTENT_URI, true, audioStreamVolumeContentObserver);
                        mAudioStreamTypeToAudioStreamVolumeContentObservers.put(audioStreamType, audioStreamVolumeContentObserver);
                    }
                }

                @Override
                public boolean onLastDetach()
                {
                    AudioStreamVolumeContentObserver audioStreamVolumeContentObserver = mAudioStreamTypeToAudioStreamVolumeContentObservers
                            .remove(audioStreamType);
                    if (audioStreamVolumeContentObserver != null)
                    {
                        mContentResolver.unregisterContentObserver(audioStreamVolumeContentObserver);
                    }

                    mAudioStreamTypeToListenerManagers.remove(audioStreamType);

                    return true;
                }
            });
            mAudioStreamTypeToListenerManagers.put(audioStreamType, listenerManager);
        }
        listenerManager.attach(listener);
    }

    public void detach(int audioStreamType, @NonNull OnAudioStreamVolumeChangedListener listener)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(listener, "listener");
        FooListenerAutoStartManager<OnAudioStreamVolumeChangedListener> listenerManager = mAudioStreamTypeToListenerManagers
                .get(audioStreamType);
        if (listenerManager == null)
        {
            return;
        }

        listenerManager.detach(listener);
    }

    private void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent)
    {
        FooListenerAutoStartManager<OnAudioStreamVolumeChangedListener> listenerManager = mAudioStreamTypeToListenerManagers
                .get(audioStreamType);
        if (listenerManager != null)
        {
            for (OnAudioStreamVolumeChangedListener callbacks : listenerManager.beginTraversing())
            {
                callbacks.onAudioStreamVolumeChanged(audioStreamType, volume, volumeMax, volumePercent);
            }
            listenerManager.endTraversing();
        }
    }
}
