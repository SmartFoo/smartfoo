package com.smartfoo.android.core.texttospeech;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

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
public class FooTextToSpeech
{
    private static final String TAG = FooLog.TAG(FooTextToSpeech.class);

    public static boolean VERBOSE_LOG_UTTERANCE_IDS      = true;
    public static boolean VERBOSE_LOG_UTTERANCE_PROGRESS = false;

    private static final FooTextToSpeech sInstance = new FooTextToSpeech();

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
    private boolean      mIsStartingOrStarted;
    private int          mNextUtteranceId;
    private String       mVoiceName;
    private int          mAudioStreamType;
    private float        mVolumeRelativeToAudioStream;

    private FooTextToSpeech()
    {
        mAudioStreamType = TextToSpeech.Engine.DEFAULT_STREAM;
        mVolumeRelativeToAudioStream = 1.0f;
    }

    public TextToSpeech getTextToSpeech()
    {
        return mTextToSpeech;
    }
    @TargetApi(VERSION_CODES.LOLLIPOP)
    public void setVoiceName(String voiceName)
    {
        mVoiceName = voiceName;

        if (mIsStartingOrStarted)
        {
            if (mVoiceName == null)
            {
                mVoiceName = mTextToSpeech.getDefaultVoice().getName();
            }

            Set<Voice> voices = mTextToSpeech.getVoices();
            for (Voice voice : voices)
            {
                if (voice.getName().equalsIgnoreCase(mVoiceName))
                {
                    mTextToSpeech.setVoice(voice);
                    break;
                }
            }
        }
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
        return mIsStartingOrStarted;
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
        FooLog.v(TAG, "+onInit(status=" + statusToString(status) + ')');

        synchronized (sInstance)
        {
            mIsStartingOrStarted = (status == TextToSpeech.SUCCESS);

            if (mIsStartingOrStarted)
            {

                setVoiceName(mVoiceName);


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

        FooLog.v(TAG, "-onInit(status=" + statusToString(status) + ')');
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
        if (mIsStartingOrStarted)
        {
            mTextToSpeech.stop();
        }
        mUtteranceCallbacks.clear();
        FooLog.d(TAG, "-clear()");
    }

    public void stop()
    {
        clear();
        if (mIsStartingOrStarted)
        {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
            mIsStartingOrStarted = false;
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
        FooLog.d(TAG, "+speak(text=" + FooString.quote(
                text) + ", clear=" + clear + ", runAfter=" + runAfter + ')');

        if (!isStartingOrStarted())
        {
            throw new IllegalStateException("start(Context context) must be called first");
        }

        if (!FooString.isNullOrEmpty(text))
        {
            synchronized (sInstance)
            {
                if (mIsStartingOrStarted)
                {
                    String utteranceId = Integer.toString(mNextUtteranceId);

                    HashMap<String, String> params = new HashMap<>();
                    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(mAudioStreamType));
                    params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(mVolumeRelativeToAudioStream));

                    if (VERBOSE_LOG_UTTERANCE_IDS)
                    {
                        FooLog.v(TAG, "speak: utteranceId=" + FooString.quote(utteranceId) +
                                      ", text=" + FooString.quote(text));
                    }

                    if (runAfter != null)
                    {
                        mUtteranceCallbacks.put(utteranceId, runAfter);
                    }

                    //noinspection deprecation
                    int result = mTextToSpeech.speak(text,
                            clear ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
                            params);
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

        FooLog.d(TAG, "-speak(text=" + FooString.quote(
                text) + ", clear=" + clear + ", runAfter=" + runAfter + ')');
    }

    public void silence(long durationInMs)
    {
        if (!isStartingOrStarted())
        {
            throw new IllegalStateException("start(Context context) must be called first");
        }

        if (mIsStartingOrStarted)
        {
            mTextToSpeech.playSilence(durationInMs, TextToSpeech.QUEUE_ADD, null);
        }
        else
        {
            // TODO:(pv) Queue silence...
        }
    }
}
