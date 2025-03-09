package com.smartfoo.android.core.texttospeech

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.smartfoo.android.core.FooListenerManager
import com.smartfoo.android.core.FooRun
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.media.FooAudioFocusListener
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks
import com.smartfoo.android.core.media.FooAudioUtils
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder.FooTextToSpeechPart
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder.FooTextToSpeechPartSilence
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder.FooTextToSpeechPartSpeech
import java.util.LinkedList

/**
 * References:
 *
 *  * https://github.com/android/platform_frameworks_base/tree/master/core/java/android/speech/tts
 *  * https://github.com/android/platform_packages_apps_settings/tree/master/src/com/android/settings/tts
 *
 */
class FooTextToSpeech private constructor() {
    companion object {
        private val TAG = FooLog.TAG(FooTextToSpeech::class.java)

        var VERBOSE_LOG_SPEECH = false
        var VERBOSE_LOG_UTTERANCE_IDS = false
        var VERBOSE_LOG_UTTERANCE_PROGRESS = false
        var VERBOSE_LOG_AUDIO_FOCUS = false

        const val DEFAULT_VOICE_SPEED = 2.0f
        const val DEFAULT_VOICE_PITCH = 1.0f
        const val DEFAULT_VOICE_VOLUME = 1.0f

        @JvmStatic
        val instance: FooTextToSpeech by lazy {
            FooTextToSpeech()
        }

        @JvmStatic
        fun statusToString(status: Int): String {
            return when (status) {
                TextToSpeech.SUCCESS -> "SUCCESS($status)"
                TextToSpeech.ERROR -> "ERROR($status)"
                else -> "UNKNOWN($status)"
            }
        }
    }

    interface FooTextToSpeechCallbacks {
        fun onTextToSpeechInitialized(status: Int)
    }

    private inner class UtteranceInfo(val mText: String?, val mRunAfter: Runnable?)

    private val mSyncLock: Any
    private val mAudioFocusListener: FooAudioFocusListener
    private val mAudioFocusListenerCallbacks: FooAudioFocusListenerCallbacks
    private val mListeners: FooListenerManager<FooTextToSpeechCallbacks>
    private val mTextToSpeechQueue: MutableList<UtteranceInfo>
    private val mUtteranceCallbacks: MutableMap<String, Runnable>
    private val mRunAfterSpeak: Runnable
    private var mApplicationContext: Context? = null
    private var mTextToSpeech: TextToSpeech? = null
    private var mIsStarted = false
    private var mIsInitialized = false
    private var mNextUtteranceId = 0
    private var mVoiceName: String? = null
    private var mVoiceSpeed: Float
    private var mVoicePitch: Float
    private var mAudioStreamType: Int
    private var mVolumeRelativeToAudioStream: Float

    init {
        FooLog.v(TAG, "+FooTextToSpeech()")
        mSyncLock = Any()
        mAudioFocusListener = FooAudioFocusListener.getInstance()
        mAudioFocusListenerCallbacks = object : FooAudioFocusListenerCallbacks() {
            override fun onAudioFocusGained(
                audioFocusStreamType: Int,
                audioFocusDurationHint: Int
            ) {
                this@FooTextToSpeech.onAudioFocusGained(
                    audioFocusStreamType,
                    audioFocusDurationHint
                )
            }

            override fun onAudioFocusLost(
                audioFocusListener: FooAudioFocusListener,
                audioFocusStreamType: Int,
                audioFocusDurationHint: Int,
                focusChange: Int
            ): Boolean {
                return this@FooTextToSpeech.onAudioFocusLost(
                    audioFocusListener,
                    audioFocusStreamType,
                    audioFocusDurationHint,
                    focusChange
                )
            }
        }
        mListeners = FooListenerManager(this)
        mTextToSpeechQueue = LinkedList()
        mUtteranceCallbacks = HashMap()
        mRunAfterSpeak = Runnable { runAfterSpeak() }
        mVoiceSpeed = DEFAULT_VOICE_SPEED
        mVoicePitch = DEFAULT_VOICE_PITCH
        mAudioStreamType = TextToSpeech.Engine.DEFAULT_STREAM
        mVolumeRelativeToAudioStream = DEFAULT_VOICE_VOLUME
        FooLog.v(TAG, "-FooTextToSpeech()")
    }

    fun attach(callbacks: FooTextToSpeechCallbacks) {
        synchronized(mSyncLock) { mListeners.attach(callbacks) }
    }

    fun detach(callbacks: FooTextToSpeechCallbacks) {
        synchronized(mSyncLock) { mListeners.detach(callbacks) }
    }

    val voices: Set<Voice>?
        get() {
            synchronized(mSyncLock) { return if (mTextToSpeech != null) mTextToSpeech!!.voices else null }
        }
    val voiceName: String?
        get() {
            synchronized(mSyncLock) { return mVoiceName }
        }

    /**
     * @param voiceName null to set default voice, or the name of a voice in [.getVoices]
     * @return true if changed, otherwise false
     */
    fun setVoiceName(voiceName: String?): Boolean {
        @Suppress("NAME_SHADOWING")
        var voiceName = voiceName
        FooLog.v(TAG, "setVoiceName(${FooString.quote(voiceName)})")
        if (FooString.isNullOrEmpty(voiceName)) {
            voiceName = null
        }
        val oldValue: String?
        val changed: Boolean
        synchronized(mSyncLock) {
            oldValue = mVoiceName
            if (mTextToSpeech == null) {
                mVoiceName = voiceName
                changed = !FooString.equals(oldValue, mVoiceName)
            } else {
                var foundVoice = mTextToSpeech!!.defaultVoice
                if (voiceName != null) {
                    val voices = voices
                    if (voices != null) {
                        for (voice in voices) {
                            //FooLog.e(TAG, "setVoiceName: voice=${FooString.quote(voice.name)}");
                            if (voiceName.equals(voice.name, ignoreCase = true)) {
                                foundVoice = voice
                                break
                            }
                        }
                    }
                }
                mVoiceName = foundVoice.name
                changed = !FooString.equals(oldValue, mVoiceName)
                mTextToSpeech!!.setVoice(foundVoice)
            }
        } // synclock
        return changed
    }

    @Suppress("MemberVisibilityCanBePrivate")
    var voiceSpeed: Float
        get() = mVoiceSpeed
        /**
         * @param voiceSpeed Speech rate. `1.0` is the normal speech rate,
         * lower values slow down the speech (`0.5` is half the normal speech rate),
         * greater values accelerate it (`2.0` is twice the normal speech rate).
         */
        set(voiceSpeed) {
            mVoiceSpeed = voiceSpeed
            mTextToSpeech?.setSpeechRate(voiceSpeed)
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var voicePitch: Float
        get() = mVoicePitch
        /**
         * @param voicePitch Speech pitch. `1.0` is the normal pitch,
         * lower values lower the tone of the synthesized voice,
         * greater values increase it.
         */
        set(voicePitch) {
            mVoicePitch = voicePitch
            mTextToSpeech?.setPitch(voicePitch)
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var audioStreamType: Int
        get() = mAudioStreamType
        set(audioStreamType) {
            synchronized(mSyncLock) { mAudioStreamType = audioStreamType }
        }

    var volumeRelativeToAudioStream: Float
        /**
         * @return 0 (silence) to 1 (maximum)
         */
        get() {
            synchronized(mSyncLock) { return mVolumeRelativeToAudioStream }
        }
        /**
         * @param volumeRelativeToAudioStream 0 (silence) to 1 (maximum)
         */
        set(volumeRelativeToAudioStream) {
            synchronized(mSyncLock) { mVolumeRelativeToAudioStream = volumeRelativeToAudioStream }
        }

    val isStarted: Boolean
        get() {
            synchronized(mSyncLock) { return mIsStarted }
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val isInitialized: Boolean
        get() {
            synchronized(mSyncLock) { return mIsInitialized }
        }

    fun stop() {
        synchronized(mSyncLock) {
            clear()
            mIsStarted = false
            if (mTextToSpeech != null) {
                mTextToSpeech!!.stop()
                mTextToSpeech!!.shutdown()
                mTextToSpeech = null
            }
            mIsInitialized = false
        } // synclock
    }

    @JvmOverloads
    fun start(
        applicationContext: Context,
        callbacks: FooTextToSpeechCallbacks? = null
    ): FooTextToSpeech {
        FooRun.throwIllegalArgumentExceptionIfNull(applicationContext, "applicationContext")
        synchronized(mSyncLock) {
            if (mApplicationContext == null) {
                mApplicationContext = applicationContext
            }
            if (callbacks != null) {
                attach(callbacks)
            }
            if (isStarted) {
                if (isInitialized) {
                    callbacks!!.onTextToSpeechInitialized(TextToSpeech.SUCCESS)
                }
            } else {
                mIsStarted = true
                mTextToSpeech = TextToSpeech(mApplicationContext) { status -> onTextToSpeechInitialized(status) }
                mTextToSpeech!!.setOnUtteranceProgressListener(object :
                    UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        this@FooTextToSpeech.onStart(utteranceId)
                    }

                    override fun onDone(utteranceId: String) {
                        this@FooTextToSpeech.onDone(utteranceId)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String) {
                        onError(utteranceId, TextToSpeech.ERROR)
                    }

                    override fun onError(utteranceId: String, errorCode: Int) {
                        this@FooTextToSpeech.onError(utteranceId, errorCode)
                    }
                })
            }
            return this
        } // synclock
    }

    private fun onTextToSpeechInitialized(status: Int) {
        try {
            FooLog.v(TAG, "+onTextToSpeechInitialized(status=${statusToString(status)})")
            synchronized(mSyncLock) {
                if (!isStarted) {
                    return
                }
                if (status != TextToSpeech.SUCCESS) {
                    FooLog.w(TAG, "onTextToSpeechInitialized: TextToSpeech failed to initialize: status == ${statusToString(status)}")
                } else {
                    setVoiceName(mVoiceName)
                    voiceSpeed = mVoiceSpeed
                    voicePitch = mVoicePitch
                    mIsInitialized = true
                }
                for (callbacks in mListeners.beginTraversing()) {
                    callbacks!!.onTextToSpeechInitialized(status)
                }
                mListeners.endTraversing()
                if (!mIsInitialized) {
                    return
                }
                val texts = mTextToSpeechQueue.iterator()
                var utteranceInfo: UtteranceInfo
                while (texts.hasNext()) {
                    utteranceInfo = texts.next()
                    texts.remove()
                    speak(false, utteranceInfo.mText, utteranceInfo.mRunAfter)
                }
            } // synclock
        } finally {
            FooLog.v(TAG, "-onTextToSpeechInitialized(status=${statusToString(status)})")
        }
    }

    private fun onStart(utteranceId: String) {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS) {
            FooLog.v(TAG, "+onStart(utteranceId=${FooString.quote(utteranceId)})")
        }
        mAudioFocusListener.audioFocusStart(
            mApplicationContext!!,
            audioStreamType,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            mAudioFocusListenerCallbacks
        )
        if (VERBOSE_LOG_UTTERANCE_PROGRESS) {
            FooLog.v(TAG, "-onStart(utteranceId=${FooString.quote(utteranceId)})")
        }
    }

    private fun onDone(utteranceId: String) {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS) {
            FooLog.v(TAG, "+onDone(utteranceId=${FooString.quote(utteranceId)})")
        }
        var runAfter: Runnable?
        synchronized(mSyncLock) {
            runAfter = mUtteranceCallbacks.remove(utteranceId)
            if (VERBOSE_LOG_UTTERANCE_PROGRESS) {
                FooLog.e(/* tag = */ TAG, /* msg = */ "onDone: mUtteranceCallbacks.size() == ${mUtteranceCallbacks.size}")
            }
        }
        //FooLog.v(TAG, "onDone: runAfter=$runAfter");
        if (runAfter != null) {
            runAfter!!.run()
        }
        if (VERBOSE_LOG_UTTERANCE_PROGRESS) {
            FooLog.v(TAG, "-onDone(utteranceId=${FooString.quote(utteranceId)})")
        }
    }

    private fun runAfterSpeak() {
        FooLog.v(TAG, "+runAfterSpeak()")
        synchronized(mSyncLock) {
            val size = mUtteranceCallbacks.size
            if (size == 0) {
                FooLog.v(TAG, "runAfterSpeak: mUtteranceCallbacks.size() == 0; audioFocusStop()")
                mAudioFocusListener.audioFocusStop(mAudioFocusListenerCallbacks)
            } else {
                FooLog.v(TAG, "runAfterSpeak: mUtteranceCallbacks.size()($size) > 0; ignoring")
            }
        }
        FooLog.v(TAG, "+runAfterSpeak()")
    }

    private fun onError(utteranceId: String, errorCode: Int) {
        if (VERBOSE_LOG_UTTERANCE_PROGRESS) {
            FooLog.w(TAG, "+onError(utteranceId=${FooString.quote(utteranceId)}, errorCode=$errorCode)")
        }
        var runAfter: Runnable?
        synchronized(mSyncLock) { runAfter = mUtteranceCallbacks.remove(utteranceId) }
        //FooLog.w(TAG, "onError: runAfter=$runAfter");
        if (runAfter != null) {
            runAfter!!.run()
        }
        if (VERBOSE_LOG_UTTERANCE_PROGRESS) {
            FooLog.w(TAG, "-onError(utteranceId=${FooString.quote(utteranceId)}), errorCode=$errorCode)")
        }
    }

    fun clear() {
        FooLog.d(TAG, "+clear()")
        synchronized(mSyncLock) {
            mTextToSpeechQueue.clear()
            if (mIsInitialized) {
                mTextToSpeech!!.stop()
            }
            mUtteranceCallbacks.clear()
        }
        FooLog.d(TAG, "-clear()")
    }

    private fun onAudioFocusGained(audioFocusStreamType: Int, audioFocusDurationHint: Int) {
        if (VERBOSE_LOG_AUDIO_FOCUS) {
            FooLog.e(TAG, "#AUDIOFOCUS_TTS onAudioFocusGained(audioFocusStreamType=${FooAudioUtils.audioStreamTypeToString(audioFocusStreamType)}, audioFocusDurationHint=${FooAudioUtils.audioFocusToString(audioFocusDurationHint)})")
        }
    }

    private fun onAudioFocusLost(
        audioFocusListener: FooAudioFocusListener,
        audioFocusStreamType: Int,
        audioFocusDurationHint: Int,
        focusChange: Int
    ): Boolean {
        if (VERBOSE_LOG_AUDIO_FOCUS) {
            FooLog.e(TAG, "#AUDIOFOCUS_TTS onAudioFocusLost(…, audioFocusStreamType=${FooAudioUtils.audioStreamTypeToString(audioFocusStreamType)}, audioFocusDurationHint=${FooAudioUtils.audioFocusToString(audioFocusDurationHint)}, focusChange=${FooAudioUtils.audioFocusToString(focusChange)})")
        }
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                //...
            }
        }
        return false
    }

    private fun onAudioFocusStop() {
        if (VERBOSE_LOG_AUDIO_FOCUS) {
            FooLog.e(TAG, "#AUDIOFOCUS_TTS onAudioFocusStop()")
        }
    }

    private inner class Runnables(private vararg val runnables: Runnable) : Runnable {
        override fun run() {
            for (runnable in runnables) {
                runnable.run()
            }
        }
    }

    fun speak(text: String): Boolean {
        return speak(false, text)
    }

    @Suppress("unused")
    fun speak(text: String?, runAfter: Runnable?): Boolean {
        return speak(false, text, runAfter)
    }

    @JvmOverloads
    fun speak(clear: Boolean, text: String?, runAfter: Runnable? = null): Boolean {
        return speak(clear, FooTextToSpeechBuilder(text), runAfter)
    }

    @Suppress("unused")
    fun speak(builder: FooTextToSpeechBuilder): Boolean {
        return speak(false, builder, null)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun speak(clear: Boolean, builder: FooTextToSpeechBuilder?, runAfter: Runnable?): Boolean {
        @Suppress("NAME_SHADOWING")
        val runAfter = if (runAfter == null) {
            mRunAfterSpeak
        } else {
            Runnables(mRunAfterSpeak, runAfter)
        }

        //
        // Always suffix w/ 500ms so that there is a clear break before the next speech.
        //
        var anySuccess = false
        if (builder != null) {
            builder.appendSilenceSentenceBreak()
            val parts = builder.build()
            val last = parts.size - 1
            for ((i, part) in parts.withIndex()) {
                anySuccess = anySuccess or speak(
                    part,
                    if (i == 0) clear else null,
                    if (i == last) runAfter else null
                )
            }
        } else {
            anySuccess = true
        }
        if (!anySuccess) {
            runAfter.run()
        }
        return anySuccess
    }

    private fun speak(part: FooTextToSpeechPart, clear: Boolean?, runAfter: Runnable?): Boolean {
        if (part is FooTextToSpeechPartSpeech) {
            val text = part.mText
            return if (clear != null) {
                speakInternal(text, clear, null)
            } else {
                speakInternal(text, false, runAfter)
            }
        }
        if (part is FooTextToSpeechPartSilence) {
            val durationMillis = part.mSilenceDurationMillis
            return silence(durationMillis, runAfter)
        }
        throw IllegalArgumentException("Unhandled part type ${part.javaClass}")
    }

    private fun speakInternal(text: String?, clear: Boolean, runAfter: Runnable?): Boolean {
        return try {
            if (VERBOSE_LOG_SPEECH) {
                FooLog.d(TAG, "+speakInternal(text=${FooString.quote(text)}, clear=$clear, runAfter=$runAfter)")
            }
            var success = false
            synchronized(mSyncLock) {
                checkNotNull(mTextToSpeech) { "start(context) must be called first" }
                if (mIsInitialized) {
                    val utteranceId = "text_$mNextUtteranceId"
                    val params = Bundle()
                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, mAudioStreamType)
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, mVolumeRelativeToAudioStream)
                    if (VERBOSE_LOG_UTTERANCE_IDS) {
                        FooLog.v(TAG, "speakInternal: utteranceId=${FooString.quote(utteranceId)}, text=${FooString.quote(text)}")
                    }
                    if (runAfter != null) {
                        mUtteranceCallbacks[utteranceId] = runAfter
                    }
                    val result = mTextToSpeech!!.speak(
                        text,
                        if (clear) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                        params,
                        utteranceId
                    )
                    if (result == TextToSpeech.SUCCESS) {
                        mNextUtteranceId++
                        success = true
                    } else {
                        mUtteranceCallbacks.remove(utteranceId)
                        runAfter?.run()
                    }
                } else {
                    val utteranceInfo = UtteranceInfo(text, runAfter)
                    mTextToSpeechQueue.add(utteranceInfo)
                    success = true
                }
            }
            success
        } finally {
            if (VERBOSE_LOG_SPEECH) {
                FooLog.d(TAG, "-speakInternal(text=${FooString.quote(text)}, clear=$clear, runAfter=$runAfter)")
            }
        }
    }

    @JvmOverloads
    fun silence(durationMillis: Int, runAfter: Runnable? = null): Boolean {
        var success = false
        synchronized(mSyncLock) {
            checkNotNull(mTextToSpeech) { "start(context) must be called first" }
            if (mIsInitialized) {
                val utteranceId = "silence_$mNextUtteranceId"
                if (VERBOSE_LOG_UTTERANCE_IDS) {
                    FooLog.v(TAG, "silence: utteranceId=${FooString.quote(utteranceId)}")
                }
                if (runAfter != null) {
                    mUtteranceCallbacks[utteranceId] = runAfter
                }
                val result = mTextToSpeech!!.playSilentUtterance(
                    durationMillis.toLong(),
                    TextToSpeech.QUEUE_ADD,
                    utteranceId
                )
                if (result == TextToSpeech.SUCCESS) {
                    mNextUtteranceId++
                    success = true
                } else {
                    mUtteranceCallbacks.remove(utteranceId)
                    runAfter?.run()
                }
            } else {
                // TODO:(pv) Queue silence…
            }
        }
        return success
    }
}
