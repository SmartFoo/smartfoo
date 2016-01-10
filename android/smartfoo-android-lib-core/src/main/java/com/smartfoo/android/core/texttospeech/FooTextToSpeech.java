package com.smartfoo.android.core.texttospeech;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import com.smartfoo.android.core.FooString;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class FooTextToSpeech
{
    private static final FooTextToSpeech sInstance = new FooTextToSpeech();

    public static FooTextToSpeech getInstance()
    {
        return sInstance;
    }

    private final List<String> mTextToSpeechQueue = new LinkedList<>();

    private TextToSpeech mTextToSpeech;
    private boolean      mIsStartingOrStarted;

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

    public void start(Context context)
    {
        if (mTextToSpeech != null)
        {
            return;
        }

        mTextToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                mIsStartingOrStarted = (status == TextToSpeech.SUCCESS);

                if (mIsStartingOrStarted)
                {
                    mTextToSpeech.setLanguage(Locale.getDefault());

                    Iterator<String> texts = mTextToSpeechQueue.iterator();
                    String text;
                    while (texts.hasNext())
                    {
                        text = texts.next();
                        texts.remove();
                        speak(text, false);
                    }
                }
            }
        });
    }

    public void shutdown()
    {
        clear();
        if (mIsStartingOrStarted)
        {
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
            mIsStartingOrStarted = false;
        }
    }

    public void clear()
    {
        mTextToSpeechQueue.clear();
        if (mIsStartingOrStarted)
        {
            mTextToSpeech.stop();
        }
    }

    public void speak(String text)
    {
        speak(text, false);
    }

    /**
     * @param text
     * @param clear true to drop all entries in the playback queue and replace them with the new entry
     */
    public void speak(String text, boolean clear)
    {
        if (mTextToSpeech == null)
        {
            throw new IllegalStateException("start(Context context) must be called first");
        }

        if (!FooString.isNullOrEmpty(text))
        {
            if (mIsStartingOrStarted)
            {
                //noinspection deprecation
                mTextToSpeech.speak(text, clear ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null);
            }
            else
            {
                mTextToSpeechQueue.add(text);
            }
        }
    }
}
