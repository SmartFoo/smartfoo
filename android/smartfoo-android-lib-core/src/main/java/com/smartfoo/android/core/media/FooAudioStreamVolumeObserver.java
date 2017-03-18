package com.smartfoo.android.core.media;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings.System;
import android.support.annotation.NonNull;

public class FooAudioStreamVolumeObserver
{
    public interface OnAudioStreamVolumeChangedListener
    {
        void onAudioStreamVolumeChanged(int audioStreamType, int volume);
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
            int currentVolume = mAudioManager.getStreamVolume(mAudioStreamType);

            if (currentVolume != mLastVolume)
            {
                mLastVolume = currentVolume;

                mListener.onAudioStreamVolumeChanged(mAudioStreamType, currentVolume);
            }
        }
    }

    private final Context mContext;

    private AudioStreamVolumeContentObserver mAudioStreamVolumeContentObserver;

    public FooAudioStreamVolumeObserver(@NonNull Context context)
    {
        mContext = context;
    }

    public void start(int audioStreamType, @NonNull OnAudioStreamVolumeChangedListener listener)
    {
        stop();

        Handler handler = new Handler();
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mAudioStreamVolumeContentObserver = new AudioStreamVolumeContentObserver(handler, audioManager, audioStreamType, listener);

        mContext.getContentResolver()
                .registerContentObserver(System.CONTENT_URI, true, mAudioStreamVolumeContentObserver);
    }

    public void stop()
    {
        if (mAudioStreamVolumeContentObserver == null)
        {
            return;
        }

        mContext.getContentResolver()
                .unregisterContentObserver(mAudioStreamVolumeContentObserver);
        mAudioStreamVolumeContentObserver = null;
    }
}
