package com.smartfoo.android.core.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver.OnAudioStreamVolumeChangedCallbacks;

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

    private final OnAudioStreamVolumeChangedCallbacks mVolumeObserverCallbacks = new OnAudioStreamVolumeChangedCallbacks()
    {
        @Override
        public void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent)
        {
            FooVolumeRestoringMediaPlayer.this.onAudioStreamVolumeChanged(audioStreamType, volume, volumeMax, volumePercent);
        }
    };

    public FooVolumeRestoringMediaPlayer(@NonNull Context context)
    {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mVolumeObserver = new FooAudioStreamVolumeObserver(mContext);
    }

    /**
     * Prepares and starts playback of the given media URI at the specified volume.
     * The original stream volume is captured before playback begins and restored by
     * {@link #stop()} unless the user changes the volume manually during playback.
     *
     * @param mediaUri            the URI of the media to play; must not be null or empty
     * @param streamType          the audio stream to use (e.g. {@link android.media.AudioManager#STREAM_MUSIC})
     * @param streamVolumePercent the desired playback volume as a fraction of the stream maximum
     *                            (0.0 = silent, 1.0 = maximum); pass a value {@code < 0} to keep
     *                            the current volume unchanged
     * @param looping             {@code true} to loop until {@link #stop()} is called
     * @return {@code true} if playback was started successfully; {@code false} if media is already
     *         playing, the URI is empty, or an error occurs while preparing the player
     */
    public boolean play(
            @NonNull final Uri mediaUri,
            final int streamType,
            double streamVolumePercent,
            final boolean looping)
    {
        FooLog.v(TAG, "play(...)");

        if (mMediaPlayer != null)
        {
            return false;
        }

        if (FooString.isNullOrEmpty(FooString.toString(mediaUri)))
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
            mVolumeObserver.attach(mStreamType, mVolumeObserverCallbacks);
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

    /**
     * Tracks whether the stream volume has been changed externally (i.e. not by this player).
     * If the volume at any point differs from the value this player requested,
     * {@code mWasStreamVolumeUnchanged} is set to {@code false}, and the original volume will
     * <em>not</em> be restored when {@link #stop()} is called.
     *
     * @param audioStreamType the stream whose volume changed
     * @param volume          the new absolute volume level
     * @param volumeMax       the maximum possible volume for this stream
     * @param volumePercent   the new volume as a percentage of the maximum (0–100)
     */
    private void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent)
    {
        FooLog.v(TAG, "onAudioStreamVolumeChanged(audioStreamType=" + audioStreamType +
                      ", volume=" + volume +
                      ", volumeMax=" + volumeMax +
                      ", volumePercent=" + volumePercent + ')');
        mWasStreamVolumeUnchanged &= volume == mStreamVolumeRequested;
        FooLog.i(TAG, "onAudioStreamVolumeChanged: mWasStreamVolumeUnchanged=" + mWasStreamVolumeUnchanged);
    }

    /**
     * Stops and releases the media player. If the stream volume was not changed externally
     * during playback, the volume is restored to the value it had before {@link #play} was called.
     * Does nothing if no media is currently playing.
     */
    public void stop()
    {
        FooLog.v(TAG, "stop()");

        if (mMediaPlayer == null)
        {
            return;
        }

        mVolumeObserver.detach(mStreamType, mVolumeObserverCallbacks);

        mMediaPlayer.stop();
        mMediaPlayer.release();

        if (mWasStreamVolumeUnchanged)
        {
            mAudioManager.setStreamVolume(mStreamType, mStreamVolumeOriginal, 0);
        }

        mMediaPlayer = null;
    }
}
