package com.smartfoo.android.core.media

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import com.smartfoo.android.core.FooReflection
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.R
import com.smartfoo.android.core.logging.FooLog
import kotlin.math.roundToInt

/**
 * Static utility methods for audio stream and audio focus operations.
 *
 * Provides string representations of [android.media.AudioManager] stream types, audio focus
 * gain/loss constants, and audio focus request results. Also includes helpers to convert
 * between absolute and percentage volume values and to retrieve [android.media.Ringtone]
 * instances from a URI.
 */
@Suppress("unused")
object FooAudioUtils {
    private val TAG = FooLog.TAG(FooAudioUtils::class)

    @JvmStatic
    val audioStreamTypes by lazy {
        FooReflection.mapConstants(AudioManager::class, "STREAM_")
            .toMutableMap()
            .apply {
                /** [android.media.AudioManager.STREAM_BLUETOOTH_SCO] is hidden */
                put(6, "STREAM_BLUETOOTH_SCO")
                /** [android.media.AudioManager.STREAM_SYSTEM_ENFORCED] is hidden */
                put(7, "STREAM_SYSTEM_ENFORCED")
                /** [android.media.AudioManager.STREAM_TTS] is hidden */
                put(9, "STREAM_TTS")
            }
    }

    /**
     * Returns a symbolic name for the given audio stream type using reflection-based constant
     * mapping (no localisation).
     *
     * @param audioStreamType an [android.media.AudioManager].STREAM_* constant
     * @return a string such as `"STREAM_MUSIC(3)"`
     */
    @JvmStatic
    fun audioStreamTypeToString(audioStreamType: Int) =
        audioStreamTypeToString(null, audioStreamType)

    /**
     * Returns a localised display name for the given audio stream type when [context] is
     * non-null, or falls back to the reflection-based constant name.
     *
     * @param context         optional context used to look up string resources; pass null for
     *                        the non-localised fallback
     * @param audioStreamType an [android.media.AudioManager].STREAM_* constant
     * @return a human-readable stream-type name
     */
    @JvmStatic
    fun audioStreamTypeToString(
        context: Context?,
        audioStreamType: Int,
    ): String {
        val s = if (context != null) {
            when (audioStreamType) {
                AudioManager.STREAM_VOICE_CALL -> context.getString(R.string.audio_stream_voice_call)
                AudioManager.STREAM_SYSTEM -> context.getString(R.string.audio_stream_system)
                AudioManager.STREAM_RING -> context.getString(R.string.audio_stream_ring)
                AudioManager.STREAM_MUSIC -> context.getString(R.string.audio_stream_media)
                AudioManager.STREAM_ALARM -> context.getString(R.string.audio_stream_alarm)
                AudioManager.STREAM_NOTIFICATION -> context.getString(R.string.audio_stream_notification)
                /** [android.media.AudioManager.STREAM_BLUETOOTH_SCO] is hidden */
                6 -> context.getString(R.string.audio_stream_bluetooth_sco)
                /** [android.media.AudioManager.STREAM_SYSTEM_ENFORCED] is hidden */
                7 -> context.getString(R.string.audio_stream_system_enforced)
                AudioManager.STREAM_DTMF -> context.getString(R.string.audio_stream_dtmf)
                /** [android.media.AudioManager.STREAM_TTS] is hidden */
                9 -> context.getString(R.string.audio_stream_text_to_speech)
                else -> context.getString(R.string.audio_stream_unknown)
            }
        } else null
        return s ?: FooReflection.toString(audioStreamTypes, audioStreamType)
    }

    @JvmStatic
    val audioFocusMap by lazy {
        FooReflection.mapConstants(AudioManager::class, "AUDIOFOCUS_NONE", "AUDIOFOCUS_GAIN", "AUDIOFOCUS_LOSS")
    }

    /**
     * Returns a symbolic name for the given audio focus gain/loss value.
     *
     * @param audioFocusGainLoss an [android.media.AudioManager].AUDIOFOCUS_* constant
     * @return a string such as `"AUDIOFOCUS_GAIN(1)"`
     */
    @JvmStatic
    fun audioFocusGainLossToString(audioFocusGainLoss: Int) =
        FooReflection.toString(audioFocusMap, audioFocusGainLoss)

    @JvmStatic
    val audioFocusRequestMap by lazy {
        FooReflection.mapConstants(AudioManager::class, "AUDIOFOCUS_REQUEST_")
    }

    /**
     * Returns a symbolic name for the given audio focus request result.
     *
     * @param audioFocusRequest an [android.media.AudioManager].AUDIOFOCUS_REQUEST_* constant
     * @return a string such as `"AUDIOFOCUS_REQUEST_GRANTED(1)"`
     */
    @JvmStatic
    fun audioFocusRequestToString(audioFocusRequest: Int) =
        FooReflection.toString(audioFocusRequestMap, audioFocusRequest)

    /**
     * Converts an absolute stream volume level to a [0.0, 1.0] fraction.
     *
     * @param audioManager    the audio manager used to query the stream maximum
     * @param audioStreamType the stream type (e.g. [android.media.AudioManager.STREAM_MUSIC])
     * @param volume          the absolute volume level to convert
     * @return volume as a fraction of the stream maximum
     */
    @JvmStatic
    fun getVolumePercentFromAbsolute(
        audioManager: AudioManager,
        audioStreamType: Int,
        volume: Int,
    ): Float {
        val volumeMax = audioManager.getStreamMaxVolume(audioStreamType)
        return volume / volumeMax.toFloat()
    }

    /**
     * Converts a [0.0, 1.0] volume fraction to an absolute stream volume level.
     *
     * @param audioManager    the audio manager used to query the stream maximum
     * @param audioStreamType the stream type (e.g. [android.media.AudioManager.STREAM_MUSIC])
     * @param volumePercent   the target volume as a fraction of the maximum
     * @return the nearest integer absolute volume level
     */
    @JvmStatic
    fun getVolumeAbsoluteFromPercent(
        audioManager: AudioManager,
        audioStreamType: Int,
        volumePercent: Float,
    ): Int {
        val volumeMax = audioManager.getStreamMaxVolume(audioStreamType)
        return (volumeMax * volumePercent).roundToInt()
    }

    /**
     * Returns the current absolute volume level for the given stream.
     *
     * @param audioManager    the audio manager
     * @param audioStreamType the stream type (e.g. [android.media.AudioManager.STREAM_MUSIC])
     * @return the current absolute volume level
     */
    @JvmStatic
    fun getVolumeAbsolute(
        audioManager: AudioManager,
        audioStreamType: Int,
    ) = audioManager.getStreamVolume(audioStreamType)

    /**
     * Returns the current volume for the given stream as a [0.0, 1.0] fraction.
     *
     * @param audioManager    the audio manager
     * @param audioStreamType the stream type (e.g. [android.media.AudioManager.STREAM_MUSIC])
     * @return current volume as a fraction of the stream maximum
     */
    @JvmStatic
    fun getVolumePercent(
        audioManager: AudioManager,
        audioStreamType: Int,
    ): Float {
        val volume = getVolumeAbsolute(audioManager, audioStreamType)
        return getVolumePercentFromAbsolute(audioManager, audioStreamType, volume)
    }

    /**
     * Returns a [android.media.Ringtone] for the given URI, or null if the URI is null/empty.
     *
     * @param context     context used to resolve the ringtone; may be null (returns null)
     * @param ringtoneUri URI of the ringtone to load; returns null if null/empty
     * @return the [android.media.Ringtone] or null
     */
    @JvmStatic
    fun getRingtone(
        context: Context?,
        ringtoneUri: Uri?,
    ): Ringtone? {
        if (FooString.isNullOrEmpty(FooString.toString(ringtoneUri))) {
            return null
        }
        return RingtoneManager.getRingtone(context, ringtoneUri)
    }
}
