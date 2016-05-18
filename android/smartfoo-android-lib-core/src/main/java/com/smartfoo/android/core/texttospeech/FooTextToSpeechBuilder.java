package com.smartfoo.android.core.texttospeech;

import android.speech.tts.TextToSpeech;

import com.smartfoo.android.core.FooString;

import java.util.LinkedList;

public class FooTextToSpeechBuilder
{
    // package
    static abstract class FooTextToSpeechPart
    {
    }

    public static class FooTextToSpeechPartSpeech
            extends FooTextToSpeechPart
    {
        // package
        final String mText;

        public FooTextToSpeechPartSpeech(String text)
        {
            int maxSpeechInputLength = TextToSpeech.getMaxSpeechInputLength();

            if (FooString.isNullOrEmpty(text) || text.length() > maxSpeechInputLength)
            {
                throw new IllegalArgumentException("text.length must be > 0 and <= " + maxSpeechInputLength);
            }

            mText = text;
        }
    }

    public static class FooTextToSpeechPartSilence
            extends FooTextToSpeechPart
    {
        // package
        final int mDurationInMs;

        public FooTextToSpeechPartSilence(int durationInMs)
        {
            mDurationInMs = durationInMs;
        }
    }

    private final LinkedList<FooTextToSpeechPart> mParts;

    public FooTextToSpeechBuilder()
    {
        mParts = new LinkedList<>();
    }

    public FooTextToSpeechBuilder(String text)
    {
        this();
        appendSpeech(text);
    }

    public FooTextToSpeechBuilder appendSpeech(String text)
    {
        mParts.add(new FooTextToSpeechPartSpeech(text));
        return this;
    }

    public FooTextToSpeechBuilder appendSilence(int durationInMs)
    {
        mParts.add(new FooTextToSpeechPartSilence(durationInMs));
        return this;
    }

    public LinkedList<FooTextToSpeechPart> build()
    {
        LinkedList<FooTextToSpeechPart> parts = new LinkedList<>(mParts);
        mParts.clear();
        return parts;
    }
}
