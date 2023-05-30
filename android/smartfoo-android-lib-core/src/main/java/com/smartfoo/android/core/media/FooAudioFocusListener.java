package com.smartfoo.android.core.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;

public class FooAudioFocusListener
{
    private static final String TAG = FooLog.TAG(FooAudioFocusListener.class);

    public static boolean VERBOSE_LOG_AUDIO_FOCUS = true;

    public static abstract class FooAudioFocusListenerCallbacks
    {
        public void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
        {
        }

        public abstract boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange);
    }

    public static FooAudioFocusListener getInstance()
    {
        return sInstance;
    }

    /**
     * @param hashtag         hashtag
     * @param audioManager    audioManager
     * @param audioStreamType audioStreamType
     * @param durationHint    durationHint
     * @param listener        listener
     * @return true if successful, otherwise false
     */
    public static boolean audioFocusStart(String hashtag,
                                          @NonNull AudioManager audioManager,
                                          int audioStreamType,
                                          int durationHint,
                                          @NonNull OnAudioFocusChangeListener listener)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(audioManager, "audioManager");
        FooRun.throwIllegalArgumentExceptionIfNull(listener, "listener");
        int result = audioManager.requestAudioFocus(listener, audioStreamType, durationHint);
        boolean success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (VERBOSE_LOG_AUDIO_FOCUS)
        {
            hashtag = hashtag != null ? (hashtag.trim() + ' ') : "";
            if (success)
            {
                FooLog.v(TAG, hashtag + "audioFocusStart: requestAudioFocus result=" +
                              FooAudioUtils.audioFocusRequestToString(result));
            }
            else
            {
                FooLog.w(TAG, hashtag + "audioFocusStart: requestAudioFocus result=" +
                              FooAudioUtils.audioFocusRequestToString(result));
            }
        }
        return success;
    }

    /**
     * @param hashtag      hashtag
     * @param audioManager audioManager
     * @param listener     listener
     * @return true if successful, otherwise false
     */
    public static boolean audioFocusStop(String hashtag,
                                         @NonNull AudioManager audioManager,
                                         @NonNull OnAudioFocusChangeListener listener)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(audioManager, "audioManager");
        FooRun.throwIllegalArgumentExceptionIfNull(listener, "listener");
        int result = audioManager.abandonAudioFocus(listener);
        boolean success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (VERBOSE_LOG_AUDIO_FOCUS)
        {
            hashtag = hashtag != null ? (hashtag.trim() + ' ') : "";
            if (success)
            {
                FooLog.v(TAG, hashtag + "audioFocusStop: abandonAudioFocus result=" +
                              FooAudioUtils.audioFocusRequestToString(result));
            }
            else
            {
                FooLog.w(TAG, hashtag + "audioFocusStop: abandonAudioFocus result=" +
                              FooAudioUtils.audioFocusRequestToString(result));
            }
        }
        return success;
    }

    //
    //
    //

    private static FooAudioFocusListener sInstance;

    static
    {
        sInstance = new FooAudioFocusListener();
    }

    //
    //
    //

    private final FooListenerManager<FooAudioFocusListenerCallbacks> mListenerManager;

    private AudioManager mAudioManager;
    private String       mHashtag;
    private int          mLastAudioFocusStreamType;
    private int          mLastAudioFocusDurationHint;

    private FooAudioFocusListener()
    {
        mListenerManager = new FooListenerManager<>("#AUDIOFOCUS");

        reset();
    }

    private void reset()
    {
        mLastAudioFocusStreamType = -1;
        mLastAudioFocusDurationHint = 0;
    }

    public void setHashtag(String hashtag)
    {
        if (hashtag != null)
        {
            hashtag = hashtag.trim();
        }
        mHashtag = hashtag;
    }

    private String getLogPrefix()
    {
        return mHashtag != null ? (mHashtag + ' ') : "";
    }

    public boolean isAudioFocusGained()
    {
        return mLastAudioFocusDurationHint >= AudioManager.AUDIOFOCUS_GAIN;
    }

    public int getAudioFocusDurationHint()
    {
        return mLastAudioFocusDurationHint;
    }

    public boolean audioFocusStart(@NonNull Context context, int audioFocusStreamType, int audioFocusDurationHint,
                                   @NonNull FooAudioFocusListenerCallbacks callbacks)
    {
        FooLog.v(TAG, getLogPrefix() + "audioFocusStart(context, audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint" +
                      FooAudioUtils.audioFocusToString(audioFocusDurationHint) +
                      ", callbacks=" + callbacks + ')');
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        if (mAudioManager == null)
        {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        mListenerManager.attach(callbacks);

        boolean success = audioFocusStart(mHashtag,
                mAudioManager,
                audioFocusStreamType,
                audioFocusDurationHint,
                mOnAudioFocusChangeListener);
        if (success)
        {
            onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
        }

        return success;
    }

    private void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
    {
        mLastAudioFocusStreamType = audioFocusStreamType;
        mLastAudioFocusDurationHint = audioFocusDurationHint;
        for (FooAudioFocusListenerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
        }
        mListenerManager.endTraversing();
    }

    public void audioFocusStop(@NonNull FooAudioFocusListenerCallbacks callbacks)
    {
        FooLog.v(TAG, getLogPrefix() + "audioFocusStop(callbacks=" + callbacks + ')');
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        if (mAudioManager == null)
        {
            return;
        }

        int sizeBefore = mListenerManager.size();
        FooLog.v(TAG, getLogPrefix() + "audioFocusStop: BEFORE mListenerManager.size() == " + sizeBefore);
        if (sizeBefore == 0)
        {
            FooLog.v(TAG, getLogPrefix() + "audioFocusStop: BEFORE mListenerManager.size() == 0; ignoring");
            return;
        }

        mListenerManager.detach(callbacks);

        int sizeAfter = mListenerManager.size();
        FooLog.v(TAG, getLogPrefix() + "audioFocusStop: AFTER mListenerManager.size() == " + sizeAfter);
        if (sizeAfter > 0)
        {
            FooLog.v(TAG, getLogPrefix() + "audioFocusStop: AFTER mListenerManager.size() > 0; ignoring");
            return;
        }

        reset();

        audioFocusStop(mHashtag, mAudioManager, mOnAudioFocusChangeListener);
    }

    private final OnAudioFocusChangeListener mOnAudioFocusChangeListener = new OnAudioFocusChangeListener()
    {
        @Override
        public void onAudioFocusChange(int focusChange)
        {
            FooAudioFocusListener.this.onAudioFocusChange(focusChange);
        }
    };

    private void onAudioFocusChange(int focusChange)
    {
        if (VERBOSE_LOG_AUDIO_FOCUS)
        {
            FooLog.v(TAG, getLogPrefix() + "onAudioFocusChange(focusChange=" +
                          FooAudioUtils.audioFocusToString(focusChange) +
                          ')');
        }

        switch (focusChange)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
            {
                onAudioFocusGained(mLastAudioFocusStreamType, focusChange);
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            {
                for (FooAudioFocusListenerCallbacks callbacks : mListenerManager.beginTraversing())
                {
                    if (callbacks.onAudioFocusLost(this, mLastAudioFocusStreamType, mLastAudioFocusDurationHint, focusChange))
                    {
                        break;
                    }
                }
                mListenerManager.endTraversing();

                break;
            }
        }

        FooLog.v(TAG, getLogPrefix() + "onAudioFocusChange: mLastAudioFocusStreamType == " +
                      FooAudioUtils.audioStreamTypeToString(mLastAudioFocusStreamType));
        FooLog.v(TAG, getLogPrefix() + "onAudioFocusChange: mLastAudioFocusDurationHint == " +
                      FooAudioUtils.audioFocusToString(mLastAudioFocusDurationHint));

        // TODO:(pv) Better cooperation of not speaking when other apps taking focus
        //  Example:
        //      Spotify is playing Media
        //      Google Now starts listening and gains audio focus
        //          FLAW! Recording doesn't always imply gaining audio focus!
        //      Spotify pauses
        //      We speak that Spotify paused, stealing audio focus from Google Now
        //      …
/*
Google Now starts recording the gains audio focus
    FLAW! Recording doesn't always imply gaining audio focus!
Spotify pauses
We start speaking the notification…
05-19 22:06:27.016 3680-3705/com.swooby.alfred I/FooTextToSpeech: T3705 audioFocusStart()
05-19 22:06:27.094 3680-3680/com.swooby.alfred D/AudioManager: AudioManager dispatching onAudioFocusChange(-2) for android.media.AudioManager@ab99fa2com.smartfoo.android.core.texttospeech.FooTextToSpeech$1@8f88433
05-19 22:06:27.094 3680-3680/com.swooby.alfred I/FooTextToSpeech: T3680 onAudioFocusChange(focusChange=AUDIOFOCUS_LOSS_TRANSIENT(-2))
05-19 22:06:27.748 3680-3680/com.swooby.alfred D/AudioManager: AudioManager dispatching onAudioFocusChange(1) for android.media.AudioManager@ab99fa2com.smartfoo.android.core.texttospeech.FooTextToSpeech$1@8f88433
05-19 22:06:27.748 3680-3680/com.swooby.alfred I/FooTextToSpeech: T3680 onAudioFocusChange(focusChange=AUDIOFOCUS_GAIN(1))
05-19 22:06:28.733 3680-3698/com.swooby.alfred I/FooTextToSpeech: T3698 audioFocusStop()
Spotify resumes...meanwhile Google Now got the shaft and was never able to record audio
*/
        // For more info, read:
        //  https://developer.android.com/training/managing-audio/audio-focus.html
        //  http://android-developers.blogspot.com/2013/08/respecting-audio-focus.html
        /*
        switch (focusChange)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                mState.audioFocusGranted = true;

                if (mState.released)
                {
                    initializeMediaPlayer();
                }

                switch (mState.lastKnownAudioFocusState)
                {
                    case UNKNOWN:
                        if (mState.state == PlayState.PLAY && !mPlayer.isPlaying())
                        {
                            mPlayer.start();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if (mState.wasPlayingWhenTransientLoss)
                        {
                            mPlayer.start();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        restoreVolume();
                        break;
                }

                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                mState.userInitiatedState = false;
                mState.audioFocusGranted = false;
                teardown();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mState.userInitiatedState = false;
                mState.audioFocusGranted = false;
                mState.wasPlayingWhenTransientLoss = mPlayer.isPlaying();
                mPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mState.userInitiatedState = false;
                mState.audioFocusGranted = false;
                lowerVolume();
                break;
        }
        mState.lastKnownAudioFocusState = focusChange;
        */
    }
}
