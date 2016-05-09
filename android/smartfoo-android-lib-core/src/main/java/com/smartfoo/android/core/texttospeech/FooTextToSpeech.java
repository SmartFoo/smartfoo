package com.smartfoo.android.core.texttospeech;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FooTextToSpeech
{
    private static final String TAG = FooLog.TAG(FooTextToSpeech.class);

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

    private FooTextToSpeech()
    {
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
                return "TextToSpeech.SUCCESS(" + status + ")";
            case TextToSpeech.ERROR:
                return "TextToSpeech.ERROR(" + status + ")";
            default:
                return "UNKNOWN(" + status + ")";
        }
    }

    private void onInit(int status)
    {
        FooLog.i(TAG, "+onInit(status=" + statusToString(status) + ")");

        synchronized (sInstance)
        {
            mIsStartingOrStarted = (status == TextToSpeech.SUCCESS);

            if (mIsStartingOrStarted)
            {
                Locale locale = Locale.getDefault();
                boolean hasVariant = (null != locale.getVariant() && locale.getVariant().length() > 0);
                boolean hasCountry = (null != locale.getCountry() && locale.getCountry().length() > 0);

                int isLanguageAvailable = mTextToSpeech.isLanguageAvailable(locale);

                boolean isLocaleSupported =
                        (!hasVariant && !hasCountry && isLanguageAvailable == TextToSpeech.LANG_AVAILABLE) ||
                        (!hasVariant && hasCountry && isLanguageAvailable == TextToSpeech.LANG_COUNTRY_AVAILABLE) ||
                        (isLanguageAvailable == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE);

                if (isLocaleSupported)
                {
                    mTextToSpeech.setLanguage(locale);
                }

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

        FooLog.i(TAG, "-onInit(status=" + statusToString(status) + ")");
    }

    private void onStart(String utteranceId)
    {
        FooLog.i(TAG, "+onStart(utteranceId=" + FooString.quote(utteranceId) + ")");
        /*
        Runnable runAfter = mUtteranceCallbacks.get(utteranceId);
        if (runAfter != null) {
            runAfter.run();
        }
        */
        FooLog.i(TAG, "-onStart(utteranceId=" + FooString.quote(utteranceId) + ")");
    }

    private void onDone(String utteranceId)
    {
        FooLog.i(TAG, "+onDone(utteranceId=" + FooString.quote(utteranceId) + ")");
        Runnable runAfter = mUtteranceCallbacks.remove(utteranceId);
        FooLog.i(TAG, "onDone: runAfter=" + runAfter);
        if (runAfter != null)
        {
            runAfter.run();
        }
        FooLog.i(TAG, "-onDone(utteranceId=" + FooString.quote(utteranceId) + ")");
    }

    private void onError(String utteranceId)
    {
        FooLog.i(TAG, "+onError(utteranceId=" + FooString.quote(utteranceId) + ")");
        Runnable runAfter = mUtteranceCallbacks.remove(utteranceId);
        FooLog.i(TAG, "onError: runAfter=" + runAfter);
        if (runAfter != null)
        {
            runAfter.run();
        }
        FooLog.i(TAG, "-onError(utteranceId=" + FooString.quote(utteranceId) + ")");
    }

    public void clear()
    {
        FooLog.i(TAG, "+clear()");
        mTextToSpeechQueue.clear();
        if (mIsStartingOrStarted)
        {
            mTextToSpeech.stop();
        }
        mUtteranceCallbacks.clear();
        FooLog.i(TAG, "-clear()");
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

    /**
     * @param text
     * @param clear true to drop all entries in the playback queue and replace them with the new entry
     */
    public void speak(String text, boolean clear, Runnable runAfter)
    {
        FooLog.i(TAG, "+speak(text=" + FooString.quote(
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

                    //noinspection deprecation
                    int result = mTextToSpeech.speak(text,
                            clear ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
                            params);
                    if (result == TextToSpeech.SUCCESS)
                    {
                        mNextUtteranceId++;
                        if (runAfter != null)
                        {
                            mUtteranceCallbacks.put(utteranceId, runAfter);
                        }
                    }
                    else
                    {
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

        FooLog.i(TAG, "-speak(text=" + FooString.quote(
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
