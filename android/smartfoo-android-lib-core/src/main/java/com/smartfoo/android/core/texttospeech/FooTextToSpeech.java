package com.smartfoo.android.core.texttospeech;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

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
@TargetApi(VERSION_CODES.LOLLIPOP)
public class FooTextToSpeech
{
    private static final String TAG = FooLog.TAG(FooTextToSpeech.class);

    public static boolean VERBOSE_LOG_UTTERANCE_IDS      = true;
    public static boolean VERBOSE_LOG_UTTERANCE_PROGRESS = false;

    private static final FooTextToSpeech sInstance = new FooTextToSpeech();

    @NonNull
    public static FooTextToSpeech getInstance()
    {
        return sInstance;
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

    private final List<UtteranceInfo>   mTextToSpeechQueue  = new LinkedList<>();
    private final Map<String, Runnable> mUtteranceCallbacks = new HashMap<>();

    private TextToSpeech mTextToSpeech;
    private boolean      mIsInitialized;
    private int          mNextUtteranceId;
    private String       mVoiceName;
    private Voice        mVoice;
    private int          mAudioStreamType;
    private float        mVolumeRelativeToAudioStream;

    private FooTextToSpeech()
    {
        mAudioStreamType = TextToSpeech.Engine.DEFAULT_STREAM;
        mVolumeRelativeToAudioStream = 1.0f;
    }

    public Set<Voice> getVoices()
    {
        return mTextToSpeech != null ? mTextToSpeech.getVoices() : null;
    }

    public Voice getVoice()
    {
        return mTextToSpeech != null ? mTextToSpeech.getVoice() : null;
    }

    public void setVoice(Voice voice)
    {
        mVoice = voice;

        if (!mIsInitialized)
        {
            return;
        }

        if (mVoice == null)
        {
            mVoice = mTextToSpeech.getDefaultVoice();
        }

        mTextToSpeech.setVoice(mVoice);

        mVoiceName = mVoice.getName();
    }

    public void setVoiceName(String voiceName)
    {
        mVoiceName = voiceName;

        if (!mIsInitialized)
        {
            return;
        }

        if (mVoiceName == null)
        {
            mVoiceName = mTextToSpeech.getDefaultVoice().getName();
        }

        Voice foundVoice = null;

        Set<Voice> voices = mTextToSpeech.getVoices();
        for (Voice voice : voices)
        {
            if (voice.getName().equalsIgnoreCase(mVoiceName))
            {
                foundVoice = voice;
                break;
            }
        }

        setVoice(foundVoice);
    }

    public int getAudioStreamType()
    {
        return mAudioStreamType;
    }

    public void setAudioStreamType(int audioStreamType)
    {
        mAudioStreamType = audioStreamType;
    }

    /**
     * @return 0 (silence) to 1 (maximum)
     */
    public float getVolumeRelativeToAudioStream()
    {
        return mVolumeRelativeToAudioStream;
    }

    /**
     * @param volumeRelativeToAudioStream 0 (silence) to 1 (maximum)
     */
    public void setVolumeRelativeToAudioStream(float volumeRelativeToAudioStream)
    {
        mVolumeRelativeToAudioStream = volumeRelativeToAudioStream;
    }

    public boolean isStartingOrStarted()
    {
        return mTextToSpeech != null;
    }

    public boolean isStarted()
    {
        return mIsInitialized;
    }

    public FooTextToSpeech start(Context applicationContext)
    {
        if (mTextToSpeech != null)
        {
            return this;
        }

        mTextToSpeech = new TextToSpeech(applicationContext, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                FooTextToSpeech.this.onInit(status);
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

        return this;
    }

    private static String statusToString(int status)
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

    private void onInit(int status)
    {
        try
        {
            FooLog.v(TAG, "+onInit(status=" + statusToString(status) + ')');

            synchronized (sInstance)
            {
                mIsInitialized = (status == TextToSpeech.SUCCESS);

                if (!mIsInitialized)
                {
                    FooLog.w(TAG, "onInit: TextToSpeech failed to initialize: status=" + statusToString(status));
                    return;
                }

                setVoice(mVoice);

                Iterator<UtteranceInfo> texts = mTextToSpeechQueue.iterator();
                UtteranceInfo utteranceInfo;
                while (texts.hasNext())
                {
                    utteranceInfo = texts.next();
                    texts.remove();

                    speak(utteranceInfo.mText, false, utteranceInfo.mRunAfter);
                }
            }
        }
        finally
        {
            FooLog.v(TAG, "-onInit(status=" + statusToString(status) + ')');
        }
    }

    private void onStart(String utteranceId)
    {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.v(TAG, "+onStart(utteranceId=" + FooString.quote(utteranceId) + ')');
        }

        // ...

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

        Runnable runAfter = mUtteranceCallbacks.remove(utteranceId);
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

    private void onError(String utteranceId)
    {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS)
        {
            FooLog.w(TAG, "+onError(utteranceId=" + FooString.quote(utteranceId) + ')');
        }

        Runnable runAfter = mUtteranceCallbacks.remove(utteranceId);
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
        mTextToSpeechQueue.clear();
        if (mIsInitialized)
        {
            mTextToSpeech.stop();
        }
        mUtteranceCallbacks.clear();
        FooLog.d(TAG, "-clear()");
    }

    public void stop()
    {
        clear();
        if (mIsInitialized)
        {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
            mIsInitialized = false;
        }
    }

    public void speak(String text)
    {
        speak(text, false);
    }

    public void speak(String text, boolean clear)
    {
        speak(text, clear, null);
    }

    public void speak(String text, Runnable runAfter)
    {
        speak(text, false, runAfter);
    }

    /**
     * @param text
     * @param clear true to drop all entries in the playback queue and replace them with the new entry
     */
    public void speak(String text, boolean clear, Runnable runAfter)
    {
        try
        {
            FooLog.d(TAG, "+speak(text=" + FooString.quote(text) + ", clear=" + clear + ", runAfter=" + runAfter + ')');

            if (!isStartingOrStarted())
            {
                throw new IllegalStateException("start(...) must be called first");
            }

            if (FooString.isNullOrEmpty(text))
            {
                return;
            }

            int maxSpeechInputLength = TextToSpeech.getMaxSpeechInputLength();
            if (text.length() > maxSpeechInputLength)
            {
                throw new IllegalArgumentException("text.length must be <= " + maxSpeechInputLength);
            }

            synchronized (sInstance)
            {
                if (mIsInitialized)
                {
                    String utteranceId = Integer.toString(mNextUtteranceId);

                    Bundle params = new Bundle();
                    params.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(mAudioStreamType));
                    params.putString(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(mVolumeRelativeToAudioStream));

                    if (VERBOSE_LOG_UTTERANCE_IDS)
                    {
                        FooLog.v(TAG, "speak: utteranceId=" + FooString.quote(utteranceId) +
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
                }
            }
        }
        finally
        {
            FooLog.d(TAG, "-speak(text=" + FooString.quote(text) + ", clear=" + clear + ", runAfter=" + runAfter + ')');
        }
    }

    public void silence(long durationInMs)
    {
        if (!isStartingOrStarted())
        {
            throw new IllegalStateException("start(Context context) must be called first");
        }

        if (mIsInitialized)
        {
            mTextToSpeech.playSilentUtterance(durationInMs, TextToSpeech.QUEUE_ADD, null);
        }
        else
        {
            // TODO:(pv) Queue silence...
        }
    }
}
