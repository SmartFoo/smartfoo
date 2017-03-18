package com.smartfoo.android.core.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

public class FooAudioUtils
{
    private static final String TAG = FooLog.TAG(FooAudioUtils.class);

    private FooAudioUtils()
    {
    }

    public static String audioStreamTypeToString(int audioStreamType)
    {
        String s;
        switch (audioStreamType)
        {
            case AudioManager.STREAM_VOICE_CALL:
                s = "STREAM_VOICE_CALL";
                break;
            case AudioManager.STREAM_SYSTEM:
                s = "STREAM_SYSTEM";
                break;
            case AudioManager.STREAM_RING:
                s = "STREAM_RING";
                break;
            case AudioManager.STREAM_MUSIC:
                s = "STREAM_MUSIC";
                break;
            case AudioManager.STREAM_ALARM:
                s = "STREAM_ALARM";
                break;
            case AudioManager.STREAM_NOTIFICATION:
                s = "STREAM_NOTIFICATION";
                break;
            case 6:
                s = "STREAM_BLUETOOTH_SCO";
                break;
            case 7:
                s = "STREAM_SYSTEM_ENFORCED";
                break;
            case AudioManager.STREAM_DTMF:
                s = "STREAM_DTMF";
                break;
            case 9:
                s = "STREAM_TTS";
                break;
            default:
                s = "UNKNOWN";
                break;
        }
        return s + '(' + audioStreamType + ')';
    }

    public static String audioFocusToString(int audioFocus)
    {
        String s;
        switch (audioFocus)
        {
            case 0:
                s = "AUDIOFOCUS_NONE";
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                s = "AUDIOFOCUS_GAIN";
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                s = "AUDIOFOCUS_GAIN_TRANSIENT";
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                s = "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK";
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
                s = "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE";
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                s = "AUDIOFOCUS_LOSS";
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                s = "AUDIOFOCUS_LOSS_TRANSIENT";
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                s = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
                break;
            default:
                s = "UNKNOWN";
                break;
        }
        return s + '(' + audioFocus + ')';
    }

    public static String audioFocusRequestToString(int audioFocus)
    {
        String s;
        switch (audioFocus)
        {
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                s = "AUDIOFOCUS_REQUEST_FAILED";
                break;
            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                s = "AUDIOFOCUS_REQUEST_GRANTED";
                break;
            case 2:
                s = "AUDIOFOCUS_REQUEST_DELAYED";
                break;
            default:
                s = "UNKNOWN";
                break;
        }
        return s + '(' + audioFocus + ')';
    }

    public static int getVolumePercentFromAbsolute(@NonNull AudioManager audioManager, int audioStreamType, int volume)
    {
        float maxVolume = audioManager.getStreamMaxVolume(audioStreamType);
        return Math.round(volume / maxVolume * 100f);
    }

    public static int getVolumeAbsoluteFromPercent(@NonNull AudioManager audioManager, int audioStreamType, int percent)
    {
        int maxVolume = audioManager.getStreamMaxVolume(audioStreamType);
        return Math.round(maxVolume * (percent / 100f));
    }

    public static int getVolumeAbsolute(@NonNull AudioManager audioManager, int audioStreamType)
    {
        return audioManager.getStreamVolume(audioStreamType);
    }

    public static int getVolumePercent(@NonNull AudioManager audioManager, int audioStreamType)
    {
        int volume = audioManager.getStreamVolume(audioStreamType);
        return getVolumePercentFromAbsolute(audioManager, audioStreamType, volume);
    }

    public static Ringtone getRingtone(Context context, Uri ringtoneUri)
    {
        if (FooString.isNullOrEmpty(ringtoneUri))
        {
            return null;
        }

        return RingtoneManager.getRingtone(context, ringtoneUri);
    }
}
