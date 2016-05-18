package com.smartfoo.android.core.texttospeech;

import android.support.annotation.NonNull;

import java.util.LinkedList;

public class FooTextToSpeechBuilder
{
    private static abstract class FooTextToSpeechPart
    {
    }

    public static class FooTextToSpeechPartSpeech
            extends FooTextToSpeechPart
    {
        private final String mText;

        public FooTextToSpeechPartSpeech(String text)
        {
            mText = text;
        }

        public String getText()
        {
            return mText;
        }
    }

    public static class FooTextToSpeechPartSilence
            extends FooTextToSpeechPart
    {
        private final int mDurationInMs;

        public FooTextToSpeechPartSilence(int durationInMs)
        {
            mDurationInMs = durationInMs;
        }

        public int getDurationInMs()
        {
            return mDurationInMs;
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

    public void invoke(
            @NonNull
            FooTextToSpeech textToSpeech,
            boolean clear,
            Runnable runAfter)
    {
        int i = 0;
        int last = mParts.size() - 1;
        for (FooTextToSpeechPart part : mParts)
        {
            invoke(textToSpeech, part, i == 0 ? clear : null, i == last ? runAfter : null);
            i++;
        }
        mParts.clear();
    }

    private static void invoke(
            @NonNull
            FooTextToSpeech textToSpeech,
            @NonNull
            FooTextToSpeechPart part,
            Boolean clear, Runnable runAfter)
    {
        if (part instanceof FooTextToSpeechPartSpeech)
        {
            String text = ((FooTextToSpeechPartSpeech) part).getText();

            if (clear != null)
            {
                textToSpeech.speak(text, clear);
                return;
            }

            textToSpeech.speak(text, runAfter);

            return;
        }

        if (part instanceof FooTextToSpeechPartSilence)
        {
            int durationInMs = ((FooTextToSpeechPartSilence) part).mDurationInMs;

            textToSpeech.silence(durationInMs, runAfter);

            return;
        }

        throw new IllegalArgumentException("unhandled part type " + part.getClass());
    }
}
