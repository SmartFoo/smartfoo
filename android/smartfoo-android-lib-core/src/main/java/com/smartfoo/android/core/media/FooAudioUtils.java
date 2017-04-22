package com.smartfoo.android.core.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.R;
import com.smartfoo.android.core.logging.FooLog;

public class FooAudioUtils
{
    private static final String TAG = FooLog.TAG(FooAudioUtils.class);

    private FooAudioUtils()
    {
    }

    private static final int[] AUDIO_STREAM_TYPES;

    static
    {
        AUDIO_STREAM_TYPES = new int[] {
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_NOTIFICATION
        };
    }

    public static int[] getAudioStreamTypes()
    {
        return AUDIO_STREAM_TYPES;
    }

    public static String audioStreamTypeToString(int audioStreamType)
    {
        return audioStreamTypeToString(null, audioStreamType);
    }

    public static String audioStreamTypeToString(Context context, int audioStreamType)
    {
        String s;
        switch (audioStreamType)
        {
            case AudioManager.STREAM_VOICE_CALL:
                s = context != null ?
                        context.getString(R.string.audio_stream_voice_call) : "STREAM_VOICE_CALL";
                break;
            case AudioManager.STREAM_SYSTEM:
                s = context != null ?
                        context.getString(R.string.audio_stream_system) : "STREAM_SYSTEM";
                break;
            case AudioManager.STREAM_RING:
                s = context != null ?
                        context.getString(R.string.audio_stream_ring) : "STREAM_RING";
                break;
            case AudioManager.STREAM_MUSIC:
                s = context != null ?
                        context.getString(R.string.audio_stream_media) : "STREAM_MUSIC";
                break;
            case AudioManager.STREAM_ALARM:
                s = context != null ?
                        context.getString(R.string.audio_stream_alarm) : "STREAM_ALARM";
                break;
            case AudioManager.STREAM_NOTIFICATION:
                s = context != null ?
                        context.getString(R.string.audio_stream_notification) : "STREAM_NOTIFICATION";
                break;
            case 6:
                s = context != null ?
                        context.getString(R.string.audio_stream_bluetooth_sco) : "STREAM_BLUETOOTH_SCO";
                break;
            case 7:
                s = context != null ?
                        context.getString(R.string.audio_stream_system_enforced) : "STREAM_SYSTEM_ENFORCED";
                break;
            case AudioManager.STREAM_DTMF:
                s = context != null ?
                        context.getString(R.string.audio_stream_dtmf) : "STREAM_DTMF";
                break;
            case 9:
                s = context != null ?
                        context.getString(R.string.audio_stream_text_to_speech) : "STREAM_TTS";
                break;
            default:
                s = context != null ?
                        context.getString(R.string.audio_stream_unknown) : "STREAM_UNKNOWN";
                break;
        }
        return context != null ? s : s + '(' + audioStreamType + ')';
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
        int volumeMax = audioManager.getStreamMaxVolume(audioStreamType);
        return Math.round(volume / (float) volumeMax * 100f);
    }

    public static int getVolumeAbsoluteFromPercent(@NonNull AudioManager audioManager, int audioStreamType, int volumePercent)
    {
        int volumeMax = audioManager.getStreamMaxVolume(audioStreamType);
        return Math.round(volumeMax * volumePercent / 100f);
    }

    public static int getVolumeAbsolute(@NonNull AudioManager audioManager, int audioStreamType)
    {
        return audioManager.getStreamVolume(audioStreamType);
    }

    public static int getVolumePercent(@NonNull AudioManager audioManager, int audioStreamType)
    {
        int volume = getVolumeAbsolute(audioManager, audioStreamType);
        return getVolumePercentFromAbsolute(audioManager, audioStreamType, volume);
    }

    public static Ringtone getRingtone(Context context, Uri ringtoneUri)
    {
        if (FooString.isNullOrEmpty(FooString.toString(ringtoneUri)))
        {
            return null;
        }

        return RingtoneManager.getRingtone(context, ringtoneUri);
    }
}
