package com.smartfoo.android.core.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver.OnAudioStreamVolumeChangedListener;

import java.io.IOException;

public class FooVolumeRestoringMediaPlayer
{
    private static final String TAG = FooLog.TAG(FooVolumeRestoringMediaPlayer.class);

    private final Context                      mContext;
    private final AudioManager                 mAudioManager;
    private       FooAudioStreamVolumeObserver mVolumeObserver;

    private int         mStreamType;
    private int         mStreamVolumeOriginal;
    private int         mStreamVolumeRequested;
    private MediaPlayer mMediaPlayer;
    private boolean     mWasStreamVolumeUnchanged;

    private final OnAudioStreamVolumeChangedListener mVolumeObserverListener = new

            OnAudioStreamVolumeChangedListener()
            {
                @Override
                public void onAudioStreamVolumeChanged(int audioStreamType, int volume)
                {
                    FooVolumeRestoringMediaPlayer.this.onAudioStreamVolumeChanged(audioStreamType, volume);
                }
            };

    public FooVolumeRestoringMediaPlayer(
            @NonNull
            Context context)
    {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mVolumeObserver = new FooAudioStreamVolumeObserver(mContext);
    }

    /**
     * @param mediaUri
     * @param streamType
     * @param streamVolumePercent 0.0 to 1.0 to set respective min to max volume, or &lt; 0 to use the current volume
     * @param looping
     * @return
     */
    public boolean play(
            @NonNull
            Uri mediaUri,
            int streamType,
            double streamVolumePercent,
            boolean looping)
    {
        FooLog.v(TAG, "play(...)");

        if (mMediaPlayer != null)
        {
            return false;
        }

        if (FooString.isNullOrEmpty(mediaUri))
        {
            return false;
        }

        mStreamType = streamType;

        int streamMaxVolume = mAudioManager.getStreamMaxVolume(streamType);

        mStreamVolumeOriginal = mAudioManager.getStreamVolume(streamType);

        if (streamVolumePercent < 0)
        {
            mStreamVolumeRequested = mStreamVolumeOriginal;
        }
        else
        {
            streamVolumePercent = Math.max(0, Math.min(streamVolumePercent, 1));

            //
            // Avoid rounding errors
            //
            if (streamVolumePercent == 1)
            {
                mStreamVolumeRequested = streamMaxVolume;
            }
            else if (streamVolumePercent == 0)
            {
                mStreamVolumeRequested = 0;
            }
            else
            {
                mStreamVolumeRequested = (int) Math.round(streamMaxVolume * streamVolumePercent);
            }
        }

        MediaPlayer mediaPlayer = new MediaPlayer();

        try
        {
            mediaPlayer.setDataSource(mContext, mediaUri);

            // NOTE: On older devices, OnCompletionListener won't be called if setLooping(true).
            //  On newer devices, OnCompletionListener will be called, but isPlaying() will be false.
            mediaPlayer.setLooping(looping);

            mediaPlayer.setAudioStreamType(streamType);

            mediaPlayer.setOnCompletionListener(new OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    boolean isPlaying = mp.isPlaying();
                    FooLog.v(TAG, "playMedia.onCompletion: mediaPlayer.isPlaying()=" + isPlaying);
                    FooLog.v(TAG, "playMedia.onCompletion: mediaPlayer.isLooping()=" + mp.isLooping());
                    //int currentPosition = mp.getCurrentPosition();
                    //FooLog.v(TAG, "playMedia.onCompletion: mediaPlayer.getCurrentPosition()=" + currentPosition);
                    //int duration = mp.getDuration();
                    //FooLog.v(TAG, "playMedia.onCompletion: mediaPlayer.getDuration()=" + duration);

                    if (isPlaying)
                    {
                        return;
                    }

                    stop();
                }
            });

            mediaPlayer.prepare();
        }
        catch (IOException | IllegalStateException | IllegalArgumentException | SecurityException ex)
        {
            mediaPlayer.release();
            return false;
        }

        try
        {
            mWasStreamVolumeUnchanged = true;
            mVolumeObserver.start(mStreamType, mVolumeObserverListener);
            mAudioManager.setStreamVolume(streamType, mStreamVolumeRequested, 0);
            mediaPlayer.start();
        }
        catch (IllegalStateException ex)
        {
            mAudioManager.setStreamVolume(streamType, mStreamVolumeOriginal, 0);
            mediaPlayer.release();
            return false;
        }

        mMediaPlayer = mediaPlayer;

        return true;
    }

    private void onAudioStreamVolumeChanged(int audioStreamType, int volume)
    {
        FooLog.v(TAG, "onAudioStreamVolumeChanged(audioStreamType=" + audioStreamType + ", volume=" + volume + ')');
        mWasStreamVolumeUnchanged &= volume == mStreamVolumeRequested;
        FooLog.i(TAG, "onAudioStreamVolumeChanged: mWasStreamVolumeUnchanged=" + mWasStreamVolumeUnchanged);
    }

    public void stop()
    {
        FooLog.v(TAG, "stop()");

        if (mMediaPlayer == null)
        {
            return;
        }

        mVolumeObserver.stop();

        mMediaPlayer.stop();
        mMediaPlayer.release();

        if (mWasStreamVolumeUnchanged)
        {
            mAudioManager.setStreamVolume(mStreamType, mStreamVolumeOriginal, 0);
        }

        mMediaPlayer = null;
    }
}