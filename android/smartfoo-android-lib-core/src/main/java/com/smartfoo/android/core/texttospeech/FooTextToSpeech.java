package com.smartfoo.android.core.texttospeech;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusListener;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder.FooTextToSpeechPart;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder.FooTextToSpeechPartSilence;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder.FooTextToSpeechPartSpeech;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * References:
 * <ul>
 * <li>https://github.com/android/platform_frameworks_base/tree/master/core/java/android/speech/tts</li>
 * <li>https://github.com/android/platform_packages_apps_settings/tree/master/src/com/android/settings/tts</li>
 * </ul>
 */
@TargetApi(21)
public class FooTextToSpeech
{
    private static final String TAG = FooLog.TAG(FooTextToSpeech.class);

    public interface FooTextToSpeechCallbacks
    {
        void onTextToSpeechInitialized(int status);
    }

    @NonNull
    public static FooTextToSpeech getInstance()
    {
        return sInstance;
    }

    public static String statusToString(int status)
    {
        switch (status)
        {
            case TextToSpeech.SUCCESS:
                return "TextToSpeech.SUCCESS(" + status + ')';
            case TextToSpeech.ERROR:
                return "TextToSpeech.ERROR(" + status + ')';
            default:
                return "UNKNOWN(" + status + ')';
        }
    }

    public static boolean VERBOSE_LOG_SPEECH             = false;
    public static boolean VERBOSE_LOG_UTTERANCE_IDS      = false;
    public static boolean VERBOSE_LOG_UTTERANCE_PROGRESS = false;
    public static boolean VERBOSE_LOG_AUDIO_FOCUS        = false;

    //
    //
    //

    private static final FooTextToSpeech sInstance;

    static
    {
        sInstance = new FooTextToSpeech();
    }

    private class UtteranceInfo
    {
        private final String   mText;
        private final Runnable mRunAfter;

        public UtteranceInfo(String text, Runnable runAfter)
        {
            mText = text;
            mRunAfter = runAfter;
        }
    }

    private final Object                                       mSyncLock;
    private final FooAudioFocusListener                        mAudioFocusListener;
    private final FooAudioFocusListenerCallbacks               mAudioFocusListenerCallbacks;
    private final FooListenerManager<FooTextToSpeechCallbacks> mListeners;
    private final List<UtteranceInfo>                          mTextToSpeechQueue;
    private final Map<String, Runnable>                        mUtteranceCallbacks;
    private final Runnable                                     mRunAfterSpeak;

    private Context      mApplicationContext;
    private TextToSpeech mTextToSpeech;
    private boolean      mIsInitialized;
    private int          mNextUtteranceId;
    private String       mVoiceName;
    private float        mVoiceSpeed;
    private float        mVoicePitch;
    private int          mAudioStreamType;
    private float        mVolumeRelativeToAudioStream;

    public FooTextToSpeech()
    {
        FooLog.v(TAG, "+FooTextToSpeech()");

        mSyncLock = new Object();
        mAudioFocusListener = FooAudioFocusListener.getInstance();
        mAudioFocusListenerCallbacks = new FooAudioFocusListenerCallbacks()
        {
            @Override
            public void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
            {
                FooTextToSpeech.this.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
            }

            @Override
            public boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
            {
                return FooTextToSpeech.this.onAudioFocusLost(audioFocusListener, audioFocusStreamType, audioFocusDurationHint, focusChange);
            }
        };
        mListeners = new FooListenerManager<>(this);
        mTextToSpeechQueue = new LinkedList<>();
        mUtteranceCallbacks = new HashMap<>();
        mRunAfterSpeak = new Runnable()
        {
            @Override
            public void run()
            {
                FooTextToSpeech.this.runAfterSpeak();
            }
        };

        mVoiceSpeed = 2.0f;
        mVoicePitch = 1.0f;
        mAudioStreamType = TextToSpeech.Engine.DEFAULT_STREAM;
        mVolumeRelativeToAudioStream = 1.0f;

        FooLog.v(TAG, "-FooTextToSpeech()");
    }

    public void attach(FooTextToSpeechCallbacks callbacks)
    {
        synchronized (mSyncLock)
        {
            mListeners.attach(callbacks);
        }
    }

    public void detach(FooTextToSpeechCallbacks callbacks)
    {
        synchronized (mSyncLock)
        {
            mListeners.detach(callbacks);
        }
    }

    public Set<Voice> getVoices()
    {
        synchronized (mSyncLock)
        {
            return mTextToSpeech != null ? mTextToSpeech.getVoices() : null;
        }
    }

    public String getVoiceName()
    {
        synchronized (mSyncLock)
        {
            return mVoiceName;
        }
    }

    /**
     * @param voiceName null to set default voice, or the name of a voice in {@link #getVoices()}
     * @return true if changed, otherwise false
     */
    public boolean setVoiceName(String voiceName)
    {
        FooLog.v(TAG, "setVoiceName(" + FooString.quote(voiceName) + ')');

        if (FooString.isNullOrEmpty(voiceName))
        {
            voiceName = null;
        }

        final String oldValue;
        final boolean changed;

        synchronized (mSyncLock)
        {
            oldValue = mVoiceName;

            if (mTextToSpeech == null)
            {
                mVoiceName = voiceName;
                changed = !FooString.equals(oldValue, mVoiceName);
            }
            else
            {
                Voice foundVoice = mTextToSpeech.getDefaultVoice();

                if (voiceName != null)
                {
                    Set<Voice> voices = getVoices();
                    if (voices != null)
                    {
                        for (Voice voice : voices)
                        {
                            //FooLog.e(TAG, "setVoiceName: voice=" + FooString.quote(voice.getName()));
                            if (voiceName.equalsIgnoreCase(voice.getName()))
                            {
                                foundVoice = voice;
                                break;
                            }
                        }
                    }
                }

                mVoiceName = foundVoice.getName();

                changed = !FooString.equals(oldValue, mVoiceName);

                mTextToSpeech.setVoice(foundVoice);
            }
        }

        return changed;
    }

    public float getVoiceSpeed()
    {
        return mVoiceSpeed;
    }

    /**
     * @param voiceSpeed Speech rate. {@code 1.0} is the normal speech rate,
     *                   lower values slow down the speech ({@code 0.5} is half the normal speech rate),
     *                   greater values accelerate it ({@code 2.0} is twice the normal speech rate).
     */
    public void setVoiceSpeed(float voiceSpeed)
    {
        mVoiceSpeed = voiceSpeed;
        mTextToSpeech.setSpeechRate(voiceSpeed);
    }

    public float getVoicePitch()
    {
        return mVoicePitch;
    }

    /**
     * @param voicePitch Speech pitch. {@code 1.0} is the normal pitch,
     *                   lower values lower the tone of the synthesized voice,
     *                   greater values increase it.
     */
    public void setVoicePitch(float voicePitch)
    {
        mVoicePitch = voicePitch;
        mTextToSpeech.setPitch(voicePitch);
    }

    public int getAudioStreamType()
    {
        return mAudioStreamType;
    }

    public void setAudioStreamType(int audioStreamType)
    {
        synchronized (mSyncLock)
        {
            mAudioStreamType = audioStreamType;
        }
    }

    /**
     * @return 0 (silence) to 1 (maximum)
     */
    public float getVolumeRelativeToAudioStream()
    {
        synchronized (mSyncLock)
        {
            return mVolumeRelativeToAudioStream;
        }
    }

    /**
     * @param volumeRelativeToAudioStream 0 (silence) to 1 (maximum)
     */
    public void setVolumeRelativeToAudioStream(float volumeRelativeToAudioStream)
    {
        synchronized (mSyncLock)
        {
            mVolumeRelativeToAudioStream = volumeRelativeToAudioStream;
        }
    }

    public boolean isStarted()
    {
        synchronized (mSyncLock)
        {
            return mTextToSpeech != null;
        }
    }

    public boolean isInitialized()
    {
        synchronized (mSyncLock)
        {
            return mIsInitialized;
        }
    }

    public void stop()
    {
        synchronized (mSyncLock)
        {
            clear();
            if (mTextToSpeech != null)
            {
                mTextToSpeech.stop();
                mTextToSpeech.shutdown();
                mTextToSpeech = null;
            }
            mIsInitialized = false;
        }
    }

    public FooTextToSpeech start(@NonNull Context applicationContext)
    {
        return start(applicationContext, null);
    }

    public FooTextToSpeech start(@NonNull Context applicationContext, FooTextToSpeechCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(applicationContext, "applicationContext");

        synchronized (mSyncLock)
        {
            if (mApplicationContext == null)
            {
                mApplicationContext = applicationContext;
            }

            attach(callbacks);

            if (isStarted())
            {
                if (isInitialized())
                {
                    callbacks.onTextToSpeechInitialized(TextToSpeech.SUCCESS);
                }
            }
            else
            {
                mTextToSpeech = new TextToSpeech(mApplicationContext, new TextToSpeech.OnInitListener()
                {
                    @Override
                    public void onInit(int status)
                    {
                        FooTextToSpeech.this.onTextToSpeechInitialized(status);
                    }
                });

                mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener()
                {
                    @Override
                    public void onStart(String utteranceId)
                    {
                        FooTextToSpeech.this.onStart(utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId)
                    {
                        FooTextToSpeech.this.onDone(utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId)
                    {
                        FooTextToSpeech.this.onError(utteranceId);
                    }
                });
            }

            return this;
        }
    }

    private void onTextToSpeechInitialized(int status)
    {
        try
        {
            FooLog.v(TAG, "+onTextToSpeechInitialized(status=" + statusToString(status) + ')');

            synchronized (mSyncLock)
            {
                if (!isStarted())
                {
                    return;
                }

                if (status != TextToSpeech.SUCCESS)
                {
                    FooLog.w(TAG, "onTextToSpeechInitialized: TextToSpeech failed to initialize: status == " +
                                  statusToString(status));
                }
                else
                {
                    setVoiceName(mVoiceName);
                    setVoiceSpeed(mVoiceSpeed);
                    setVoicePitch(mVoicePitch);

                    mIsInitialized = true;
                }

                for (FooTextToSpeechCallbacks callbacks : mListeners.beginTraversing())
                {
                    callbacks.onTextToSpeechInitialized(status);
                }
                mListeners.endTraversing();

                if (!mIsInitialized)
                {
                    return;
                }

                Iterator<UtteranceInfo> texts = mTextToSpeechQueue.iterator();
                UtteranceInfo utteranceInfo;
                while (texts.hasNext())
                {
                    utteranceInfo = texts.next();
                    texts.remove();

                    speak(false, utteranceInfo.mText, utteranceInfo.mRunAfter);
                }
            }
        }
        finally
        {
            FooLog.v(TAG, "-onTextToSpeechInitialized(status=" + statusToString(status) + ')');
        }
    }

    private void onStart(String utteranceId)
    {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.v(TAG, "+onStart(utteranceId=" + FooString.quote(utteranceId) + ')');
        }

        mAudioFocusListener.audioFocusStart(mApplicationContext,
                getAudioStreamType(),
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                mAudioFocusListenerCallbacks);

        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.v(TAG, "-onStart(utteranceId=" + FooString.quote(utteranceId) + ')');
        }
    }

    private void onDone(String utteranceId)
    {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.v(TAG, "+onDone(utteranceId=" + FooString.quote(utteranceId) + ')');
        }

        Runnable runAfter;
        synchronized (mSyncLock)
        {
            runAfter = mUtteranceCallbacks.remove(utteranceId);
            if (VERBOSE_LOG_UTTERANCE_PROGRESS)
            {
                FooLog.e(TAG, "onDone: mUtteranceCallbacks.size() == " + mUtteranceCallbacks.size());
            }
        }
        //FooLog.v(TAG, "onDone: runAfter=" + runAfter);
        if (runAfter != null)
        {
            runAfter.run();
        }

        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.v(TAG, "-onDone(utteranceId=" + FooString.quote(utteranceId) + ')');
        }
    }

    private void runAfterSpeak()
    {
        FooLog.v(TAG, "+runAfterSpeak()");
        synchronized (mSyncLock)
        {
            int size = mUtteranceCallbacks.size();
            if (size == 0)
            {
                FooLog.v(TAG, "runAfterSpeak: mUtteranceCallbacks.size()(" + size + ") == 0; audioFocusStop()");
                mAudioFocusListener.audioFocusStop(mAudioFocusListenerCallbacks);
            }
            else
            {
                FooLog.v(TAG, "runAfterSpeak: mUtteranceCallbacks.size()(" + size + ") > 0; ignoring");
            }
        }
        FooLog.v(TAG, "+runAfterSpeak()");
    }

    private void onError(String utteranceId)
    {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.w(TAG, "+onError(utteranceId=" + FooString.quote(utteranceId) + ')');
        }

        Runnable runAfter;
        synchronized (mSyncLock)
        {
            runAfter = mUtteranceCallbacks.remove(utteranceId);
        }
        //FooLog.w(TAG, "onError: runAfter=" + runAfter);
        if (runAfter != null)
        {
            runAfter.run();
        }

        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.w(TAG, "-onError(utteranceId=" + FooString.quote(utteranceId) + ')');
        }
    }

    public void clear()
    {
        FooLog.d(TAG, "+clear()");
        synchronized (mSyncLock)
        {
            mTextToSpeechQueue.clear();
            if (mIsInitialized)
            {
                mTextToSpeech.stop();
            }
            mUtteranceCallbacks.clear();
        }
        FooLog.d(TAG, "-clear()");
    }

    private void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
    {
        if (VERBOSE_LOG_AUDIO_FOCUS)
        {
            FooLog.e(TAG, "#AUDIOFOCUS_TTS onAudioFocusGained(audioFocusStreamType=" +
                          FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                          ", audioFocusDurationHint=" +
                          FooAudioUtils.audioFocusToString(audioFocusDurationHint) + ')');
        }
    }

    private boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
    {
        if (VERBOSE_LOG_AUDIO_FOCUS)
        {
            FooLog.e(TAG, "#AUDIOFOCUS_TTS onAudioFocusLost(…, audioFocusStreamType=" +
                          FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                          ", audioFocusDurationHint=" +
                          FooAudioUtils.audioFocusToString(audioFocusDurationHint) +
                          ", focusChange=" +
                          FooAudioUtils.audioFocusToString(focusChange) + ')');
        }

        switch (focusChange)
        {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                break;
        }

        return false;
    }

    private void onAudioFocusStop()
    {
        if (VERBOSE_LOG_AUDIO_FOCUS)
        {
            FooLog.e(TAG, "#AUDIOFOCUS_TTS onAudioFocusStop()");
        }
    }

    private class Runnables
            implements Runnable
    {
        private final Runnable[] mRunnables;

        public Runnables(Runnable... runnables)
        {
            mRunnables = runnables;
        }

        @Override
        public void run()
        {
            for (Runnable runnable : mRunnables)
            {
                if (runnable != null)
                {
                    runnable.run();
                }
            }
        }
    }

    public boolean speak(String text)
    {
        return speak(false, text);
    }

    public boolean speak(boolean clear, String text)
    {
        return speak(clear, text, null);
    }

    public boolean speak(String text, Runnable runAfter)
    {
        return speak(false, text, runAfter);
    }

    public boolean speak(boolean clear, String text, Runnable runAfter)
    {
        return speak(clear, new FooTextToSpeechBuilder(text), runAfter);
    }

    public boolean speak(@NonNull FooTextToSpeechBuilder builder)
    {
        return speak(false, builder, null);
    }

    public boolean speak(boolean clear, @NonNull FooTextToSpeechBuilder builder, Runnable runAfter)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(builder, "builder");

        if (runAfter == null)
        {
            runAfter = mRunAfterSpeak;
        }
        else
        {
            runAfter = new Runnables(mRunAfterSpeak, runAfter);
        }

        //
        // Always suffix w/ 500ms so that there is a clear break before the next speech.
        //
        builder.appendSilenceSentenceBreak();

        boolean anySuccess = false;

        List<FooTextToSpeechPart> parts = builder.build();
        int i = 0;
        int last = parts.size() - 1;
        for (FooTextToSpeechPart part : parts)
        {
            anySuccess |= speak(part, i == 0 ? clear : null, i == last ? runAfter : null);
            i++;
        }

        if (!anySuccess)
        {
            runAfter.run();
        }

        return anySuccess;
    }

    private boolean speak(@NonNull FooTextToSpeechPart part, Boolean clear, Runnable runAfter)
    {
        if (part instanceof FooTextToSpeechPartSpeech)
        {
            String text = ((FooTextToSpeechPartSpeech) part).mText;

            if (clear != null)
            {
                return speakInternal(text, clear, null);
            }
            else
            {
                return speakInternal(text, false, runAfter);
            }
        }

        if (part instanceof FooTextToSpeechPartSilence)
        {
            int durationMillis = ((FooTextToSpeechPartSilence) part).mSilenceDurationMillis;

            return silence(durationMillis, runAfter);
        }

        throw new IllegalArgumentException("unhandled part type " + part.getClass());
    }

    private boolean speakInternal(String text, boolean clear, Runnable runAfter)
    {
        try
        {
            if (VERBOSE_LOG_SPEECH)
            {
                FooLog.d(TAG, "+speakInternal(text=" + FooString.quote(text) +
                              ", clear=" + clear +
                              ", runAfter=" + runAfter + ')');
            }

            boolean success = false;

            synchronized (mSyncLock)
            {
                if (mTextToSpeech == null)
                {
                    throw new IllegalStateException("start(...) must be called first");
                }

                if (mIsInitialized)
                {
                    String utteranceId = "text_" + Integer.toString(mNextUtteranceId);

                    Bundle params = new Bundle();
                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, mAudioStreamType);
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, mVolumeRelativeToAudioStream);

                    if (VERBOSE_LOG_UTTERANCE_IDS)
                    {
                        FooLog.v(TAG, "speakInternal: utteranceId=" + FooString.quote(utteranceId) +
                                      ", text=" + FooString.quote(text));
                    }

                    if (runAfter != null)
                    {
                        mUtteranceCallbacks.put(utteranceId, runAfter);
                    }

                    int result = mTextToSpeech.speak(text,
                            clear ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
                            params,
                            utteranceId);
                    if (result == TextToSpeech.SUCCESS)
                    {
                        mNextUtteranceId++;

                        success = true;
                    }
                    else
                    {
                        mUtteranceCallbacks.remove(utteranceId);

                        if (runAfter != null)
                        {
                            runAfter.run();
                        }
                    }
                }
                else
                {
                    UtteranceInfo utteranceInfo = new UtteranceInfo(text, runAfter);
                    mTextToSpeechQueue.add(utteranceInfo);

                    success = true;
                }
            }

            return success;
        }
        finally
        {
            if (VERBOSE_LOG_SPEECH)
            {
                FooLog.d(TAG, "-speakInternal(text=" + FooString.quote(text) +
                              ", clear=" + clear +
                              ", runAfter=" + runAfter + ')');
            }
        }
    }

    public boolean silence(int durationMillis)
    {
        return silence(durationMillis, null);
    }

    public boolean silence(int durationMillis, Runnable runAfter)
    {
        boolean success = false;

        synchronized (mSyncLock)
        {
            if (mTextToSpeech == null)
            {
                throw new IllegalStateException("start(Context context) must be called first");
            }

            if (mIsInitialized)
            {
                String utteranceId = "silence_" + Integer.toString(mNextUtteranceId);

                if (VERBOSE_LOG_UTTERANCE_IDS)
                {
                    FooLog.v(TAG, "silence: utteranceId=" + FooString.quote(utteranceId));
                }

                if (runAfter != null)
                {
                    mUtteranceCallbacks.put(utteranceId, runAfter);
                }

                int result = mTextToSpeech.playSilentUtterance(durationMillis, TextToSpeech.QUEUE_ADD, utteranceId);
                if (result == TextToSpeech.SUCCESS)
                {
                    mNextUtteranceId++;

                    success = true;
                }
                else
                {
                    mUtteranceCallbacks.remove(utteranceId);

                    if (runAfter != null)
                    {
                        runAfter.run();
                    }
                }
            }
            else
            {
                // TODO:(pv) Queue silence…
            }
        }

        return success;
    }
}
