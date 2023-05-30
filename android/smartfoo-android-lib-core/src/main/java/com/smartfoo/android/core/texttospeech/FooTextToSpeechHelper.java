package com.smartfoo.android.core.texttospeech;

import android.app.Activity;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * From:
 * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/speech/tts/TtsEngines.java
 */
public class FooTextToSpeechHelper
{
    private static final String  TAG = FooLog.TAG(FooTextToSpeechHelper.class);
    private static final boolean DBG = false;

    public static final String SETTINGS_ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";

    public static Intent getIntentTextToSpeechSettings()
    {
        return new Intent().setAction(SETTINGS_ACTION_TTS_SETTINGS);
    }

    public static Intent getIntentRequestTextToSpeechData()
    {
        return new Intent().setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
    }

    /**
     * The Activity should implement {@link Activity#onActivityResult(int, int, Intent)} similar to:
     * <pre>
     * {@literal @}Override
     * protected void onActivityResult(int requestCode, int resultCode, Intent data)
     * {
     *     super.onActivityResult(requestCode, resultCode, data);
     *
     *     switch (requestCode)
     *     {
     *         case REQUEST_ACTION_CHECK_TTS_DATA:
     *         {
     *             switch (resultCode)
     *             {
     *                 case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
     *                 {
     *                     ArrayList&lt;String&gt; availableVoices = data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
     *                     FooLog.d(TAG, "onActivityResult: availableVoices=" + availableVoices);
     *
     *                     ArrayAdapter&lt;String&gt; spinnerVoicesAdapter = new ArrayAdapter&lt;&gt;(this,
     * android.R.layout.simple_spinner_dropdown_item, availableVoices);
     *                     mSpinnerVoices.setAdapter(spinnerVoicesAdapter);
     *                     break;
     *                 }
     *             }
     *             break;
     *         }
     *     }
     * }
     * </pre>
     *
     * @param activity    activity
     * @param requestCode requestCode
     */
    public static void requestTextToSpeechData(@NonNull Activity activity, int requestCode)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(activity, "activity");
        if (activity.isFinishing() || activity.isDestroyed())
        {
            return;
        }
        Intent intent = getIntentRequestTextToSpeechData();
        String signature = "activity.startActivityForResult(intent=" + FooPlatformUtils.toString(intent) +
                           ", requestCode=" + requestCode + ')';
        FooLog.v(TAG, "requestTextToSpeechData: +" + signature);
        activity.startActivityForResult(intent, requestCode);
        FooLog.v(TAG, "requestTextToSpeechData: -" + signature);
    }

    /**
     * Locale delimiter used by the old-style 3 char locale string format (like "eng-usa")
     */
    private static final String LOCALE_DELIMITER_OLD = "-";

    /**
     * Locale delimiter used by the new-style locale string format (Locale.toString() results,
     * like "en_US")
     */
    private static final String LOCALE_DELIMITER_NEW = "_";

    /**
     * Mapping of various language strings to the normalized Locale form
     */
    private static final Map<String, String> sNormalizeLanguage;

    /**
     * Mapping of various country strings to the normalized Locale form
     */
    private static final Map<String, String> sNormalizeCountry;

    static
    {
        HashMap<String, String> normalizeLanguage = new HashMap<>();
        for (String language : Locale.getISOLanguages())
        {
            try
            {
                normalizeLanguage.put(new Locale(language).getISO3Language(), language);
            }
            catch (MissingResourceException e)
            {
                continue;
            }
        }
        sNormalizeLanguage = Collections.unmodifiableMap(normalizeLanguage);

        HashMap<String, String> normalizeCountry = new HashMap<>();
        for (String country : Locale.getISOCountries())
        {
            try
            {
                normalizeCountry.put(new Locale("", country).getISO3Country(), country);
            }
            catch (MissingResourceException e)
            {
                continue;
            }
        }
        sNormalizeCountry = Collections.unmodifiableMap(normalizeCountry);
    }

    public static Locale parseLocaleString(String localeString)
    {
        String language = "", country = "", variant = "";
        if (!TextUtils.isEmpty(localeString))
        {
            String[] split = localeString.split("[" + LOCALE_DELIMITER_OLD + LOCALE_DELIMITER_NEW + "]");
            language = split[0].toLowerCase();
            if (split.length == 0)
            {
                FooLog.w(TAG, "Failed to convert " + localeString + " to a valid Locale object. Only separators");
                return null;
            }
            if (split.length > 3)
            {
                FooLog.w(TAG, "Failed to convert " + localeString + " to a valid Locale object. Too many separators");
                return null;
            }
            if (split.length >= 2)
            {
                country = split[1].toUpperCase();
            }
            if (split.length >= 3)
            {
                variant = split[2];
            }
        }

        String normalizedLanguage = sNormalizeLanguage.get(language);
        if (normalizedLanguage != null)
        {
            language = normalizedLanguage;
        }

        String normalizedCountry = sNormalizeCountry.get(country);
        if (normalizedCountry != null)
        {
            country = normalizedCountry;
        }

        if (DBG)
        {
            FooLog.d(TAG, "parseLocaleString(" + language + "," + country + "," + variant + ")");
        }

        Locale result = new Locale(language, country, variant);
        try
        {
            result.getISO3Language();
            result.getISO3Country();
            return result;
        }
        catch (MissingResourceException e)
        {
            FooLog.w(TAG, "Failed to convert " + localeString + " to a valid Locale object.");
            return null;
        }
    }

    private FooTextToSpeechHelper()
    {
    }
}
