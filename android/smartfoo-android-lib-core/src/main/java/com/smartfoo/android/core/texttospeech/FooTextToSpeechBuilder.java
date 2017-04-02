package com.smartfoo.android.core.texttospeech;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FooTextToSpeechBuilder
{
    public static final int SILENCE_WORD_BREAK_MILLIS      = 300;
    public static final int SILENCE_SENTENCE_BREAK_MILLIS  = 500;
    public static final int SILENCE_PARAGRAPH_BREAK_MILLIS = 750;

    public static final int MAX_SPEECH_INPUT_LENGTH = TextToSpeech.getMaxSpeechInputLength();

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
            if (text != null)
            {
                if (text.length() > MAX_SPEECH_INPUT_LENGTH)
                {
                    throw new IllegalArgumentException(
                            "text.length must be <= FooTextToSpeechBuilder.MAX_SPEECH_INPUT_LENGTH(" +
                            MAX_SPEECH_INPUT_LENGTH + ')');
                }

                text = text.trim();
                if ("".equals(text))
                {
                    text = null;
                }
            }

            mText = text;
        }

        @Override
        public String toString()
        {
            return "mText=" + FooString.quote(mText);
        }
    }

    public static class FooTextToSpeechPartSilence
            extends FooTextToSpeechPart
    {
        // package
        final int mSilenceDurationMillis;

        public FooTextToSpeechPartSilence(int silenceDurationMillis)
        {
            mSilenceDurationMillis = silenceDurationMillis;
        }

        @Override
        public String toString()
        {
            return "mSilenceDurationMillis=" + mSilenceDurationMillis;
        }
    }

    private final Context                         mContext;
    private final LinkedList<FooTextToSpeechPart> mParts;

    public FooTextToSpeechBuilder()
    {
        this(null, null);
    }

    public FooTextToSpeechBuilder(Context context)
    {
        this(context, null);
    }

    public FooTextToSpeechBuilder(String text)
    {
        this(null, text);
    }

    public FooTextToSpeechBuilder(Context context, int textResId, Object... formatArgs)
    {
        this(context, null);
        appendSpeech(textResId, formatArgs);
    }

    public FooTextToSpeechBuilder(Context context, String text)
    {
        mContext = context;
        mParts = new LinkedList<>();
        appendSpeech(text);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Iterator<FooTextToSpeechPart> iterator = mParts.iterator(); iterator.hasNext(); )
        {
            FooTextToSpeechPart part = iterator.next();
            sb.append(part);
            if (iterator.hasNext())
            {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public boolean isEmpty()
    {
        return mParts.isEmpty();
    }

    public int getNumberOfParts()
    {
        return mParts.size();
    }

    public FooTextToSpeechBuilder appendSpeech(int textResId, Object... formatArgs)
    {
        return appendSpeech(mContext, textResId, formatArgs);
    }

    public FooTextToSpeechBuilder appendSpeech(Context context, int textResId, Object... formatArgs)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        return appendSpeech(context.getString(textResId, formatArgs));
    }

    public FooTextToSpeechBuilder appendSpeech(CharSequence text)
    {
        return appendSpeech(FooString.isNullOrEmpty(text) ? null : text.toString());
    }

    public FooTextToSpeechBuilder appendSpeech(String text)
    {
        return append(FooString.isNullOrEmpty(text) ? null : new FooTextToSpeechPartSpeech(text));
    }

    public FooTextToSpeechBuilder appendSilenceWordBreak()
    {
        return appendSilence(SILENCE_WORD_BREAK_MILLIS);
    }

    public FooTextToSpeechBuilder appendSilenceSentenceBreak()
    {
        return appendSilence(SILENCE_SENTENCE_BREAK_MILLIS);
    }

    public FooTextToSpeechBuilder appendSilenceParagraphBreak()
    {
        return appendSilence(SILENCE_PARAGRAPH_BREAK_MILLIS);
    }

    public FooTextToSpeechBuilder appendSilence(int durationInMs)
    {
        return append(new FooTextToSpeechPartSilence(durationInMs));
    }

    public FooTextToSpeechBuilder append(FooTextToSpeechPart part)
    {
        if (part != null &&
            (!(part instanceof FooTextToSpeechPartSpeech) ||
             (((FooTextToSpeechPartSpeech) part).mText != null)))
        {
            mParts.add(part);
        }
        return this;
    }

    public FooTextToSpeechBuilder append(FooTextToSpeechBuilder builder)
    {
        if (builder != null)
        {
            for (FooTextToSpeechPart mPart : builder.mParts)
            {
                append(mPart);
            }
        }
        return this;
    }

    //package
    List<FooTextToSpeechPart> build()
    {
        List<FooTextToSpeechPart> parts = new LinkedList<>(mParts);
        mParts.clear();
        return parts;
    }
}
