package com.smartfoo.android.core.media

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import com.smartfoo.android.core.FooListenerManager
import com.smartfoo.android.core.logging.FooLog

class FooAudioFocusController
private constructor() {
    companion object {
        private val TAG = FooLog.TAG(FooAudioFocusController::class.java)

        @JvmStatic
        val instance = FooAudioFocusController()

        var VERBOSE_LOG_AUDIO_FOCUS: Boolean = true

        /**
         * @param hashtag         hashtag
         * @param audioManager    audioManager
         * @param audioStreamType audioStreamType
         * @param durationHint    durationHint
         * @param listener        listener
         * @return true if successful, otherwise false
         */
        fun audioFocusStart(
            hashtag: String?,
            audioManager: AudioManager,
            audioStreamType: Int,
            durationHint: Int,
            listener: OnAudioFocusChangeListener
        ): Boolean {
            var hashtag = hashtag

            // 1. Calling this reports the following in logcat
            // "Use of stream types is deprecated for operations other than volume control"
            // "See the documentation of requestAudioFocus() for what to use instead with android.media.AudioAttributes to qualify your playback use case"
            // 2. This seems to be being called a lot while speaking utterances, but maybe that is just normal/expected.
            val result = audioManager.requestAudioFocus(listener, audioStreamType, durationHint)

            val success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (VERBOSE_LOG_AUDIO_FOCUS) {
                hashtag = if (hashtag != null) (hashtag.trim { it <= ' ' } + ' ') else ""
                if (success) {
                    FooLog.v(TAG, "${hashtag}audioFocusStart: requestAudioFocus result=${FooAudioUtils.audioFocusRequestToString(result)}")
                } else {
                    FooLog.w(TAG, "${hashtag}audioFocusStart: requestAudioFocus result=${FooAudioUtils.audioFocusRequestToString(result)}")
                }
            }
            return success
        }

        /**
         * @param hashtag      hashtag
         * @param audioManager audioManager
         * @param listener     listener
         * @return true if successful, otherwise false
         */
        fun audioFocusStop(
            hashtag: String?,
            audioManager: AudioManager,
            listener: OnAudioFocusChangeListener
        ): Boolean {
            var hashtag = hashtag
            val result = audioManager.abandonAudioFocus(listener)
            val success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (VERBOSE_LOG_AUDIO_FOCUS) {
                hashtag = if (hashtag != null) (hashtag.trim { it <= ' ' } + ' ') else ""
                if (success) {
                    FooLog.v(TAG, "${hashtag}audioFocusStop: abandonAudioFocus result=${FooAudioUtils.audioFocusRequestToString(result)}")
                } else {
                    FooLog.w(TAG, "${hashtag}audioFocusStop: abandonAudioFocus result=${FooAudioUtils.audioFocusRequestToString(result)}")
                }
            }
            return success
        }
    }

    abstract class FooAudioFocusControllerCallbacks {
        open fun onAudioFocusGained(audioFocusStreamType: Int, audioFocusDurationHint: Int) {
        }

        abstract fun onAudioFocusLost(
            audioFocusStreamType: Int,
            audioFocusDurationHint: Int,
            focusChange: Int
        ): Boolean
    }

    //
    //
    //

    private val mListenerManager = FooListenerManager<FooAudioFocusControllerCallbacks>("#AUDIOFOCUS")

    private var mAudioManager: AudioManager? = null
    private var mHashtag: String? = null
    private var lastAudioFocusStreamType = 0
    var audioFocusDurationHint: Int = 0
        private set

    private fun reset() {
        lastAudioFocusStreamType = -1
        this.audioFocusDurationHint = 0
    }

    fun setHashtag(hashtag: String?) {
        var hashtag = hashtag
        if (hashtag != null) {
            hashtag = hashtag.trim { it <= ' ' }
        }
        mHashtag = hashtag
    }

    private val logPrefix: String
        get() = if (mHashtag != null) "$mHashtag " else ""

    val isAudioFocusGained: Boolean
        get() = this.audioFocusDurationHint >= AudioManager.AUDIOFOCUS_GAIN

    fun audioFocusStart(
        context: Context, audioFocusStreamType: Int, audioFocusDurationHint: Int,
        callbacks: FooAudioFocusControllerCallbacks
    ): Boolean {
        FooLog.v(TAG, "${this.logPrefix}audioFocusStart(context, audioFocusStreamType=${FooAudioUtils.audioStreamTypeToString(audioFocusStreamType)}, audioFocusDurationHint=${FooAudioUtils.audioFocusGainLossToString(audioFocusDurationHint)}, callbacks=$callbacks)")

        if (mAudioManager == null) {
            mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        }

        mListenerManager.attach(callbacks)

        val success: Boolean = Companion.audioFocusStart(
            mHashtag,
            mAudioManager!!,
            audioFocusStreamType,
            audioFocusDurationHint,
            mOnAudioFocusChangeListener
        )
        if (success) {
            onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint)
        }

        return success
    }

    private fun onAudioFocusGained(audioFocusStreamType: Int, audioFocusDurationHint: Int) {
        lastAudioFocusStreamType = audioFocusStreamType
        this.audioFocusDurationHint = audioFocusDurationHint
        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint)
        }
        mListenerManager.endTraversing()
    }

    fun audioFocusStop(callbacks: FooAudioFocusControllerCallbacks) {
        FooLog.v(TAG, "${this.logPrefix}audioFocusStop(callbacks=$callbacks)")

        if (mAudioManager == null) {
            return
        }

        val sizeBefore = mListenerManager.size()
        FooLog.v(TAG, this.logPrefix + "audioFocusStop: BEFORE mListenerManager.size() == " + sizeBefore)
        if (sizeBefore == 0) {
            FooLog.v(TAG, this.logPrefix + "audioFocusStop: BEFORE mListenerManager.size() == 0; ignoring")
            return
        }

        mListenerManager.detach(callbacks)

        val sizeAfter = mListenerManager.size()
        FooLog.v(TAG, this.logPrefix + "audioFocusStop: AFTER mListenerManager.size() == " + sizeAfter)
        if (sizeAfter > 0) {
            FooLog.v(TAG, this.logPrefix + "audioFocusStop: AFTER mListenerManager.size() > 0; ignoring")
            return
        }

        reset()

        Companion.audioFocusStop(mHashtag, mAudioManager!!, mOnAudioFocusChangeListener)
    }

    private val mOnAudioFocusChangeListener = OnAudioFocusChangeListener { focusChange -> this@FooAudioFocusController.onAudioFocusChange(focusChange) }

    init {

        reset()
    }

    // --- Focus change routing -----------------------------------------------
    private fun onAudioFocusChange(focusChange: Int) {
        if (VERBOSE_LOG_AUDIO_FOCUS) {
            FooLog.v(TAG, "${this.logPrefix}onAudioFocusChange(focusChange=${FooAudioUtils.audioFocusGainLossToString(focusChange)})")
        }

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> {
                onAudioFocusGained(lastAudioFocusStreamType, focusChange)
            }

            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                for (callbacks in mListenerManager.beginTraversing()) {
                    if (callbacks.onAudioFocusLost(lastAudioFocusStreamType, audioFocusDurationHint, focusChange)) {
                        break
                    }
                }
                mListenerManager.endTraversing()
            }
        }

        FooLog.v(TAG, "${this.logPrefix}onAudioFocusChange: lastAudioFocusStreamType == ${FooAudioUtils.audioStreamTypeToString(lastAudioFocusStreamType)}")
        FooLog.v(TAG, "${this.logPrefix}onAudioFocusChange: audioFocusDurationHint == ${FooAudioUtils.audioFocusGainLossToString(audioFocusDurationHint)}")

        // TODO:(pv) Better cooperation of not speaking when other apps taking focus
        //  Example:
        //      Spotify is playing Media
        //      Google Now starts listening and gains audio focus
        //          FLAW! Recording doesn't always imply gaining audio focus!
        //      Spotify pauses
        //      We speak that Spotify paused, stealing audio focus from Google Now
        //      …
        /*
Google Now starts recording the gains audio focus
    FLAW! Recording doesn't always imply gaining audio focus!
Spotify pauses
We start speaking the notification…
05-19 22:06:27.016 3680-3705/com.swooby.alfred I/FooTextToSpeech: T3705 audioFocusStart()
05-19 22:06:27.094 3680-3680/com.swooby.alfred D/AudioManager: AudioManager dispatching onAudioFocusChange(-2) for android.media.AudioManager@ab99fa2com.smartfoo.android.core.texttospeech.FooTextToSpeech$1@8f88433
05-19 22:06:27.094 3680-3680/com.swooby.alfred I/FooTextToSpeech: T3680 onAudioFocusChange(focusChange=AUDIOFOCUS_LOSS_TRANSIENT(-2))
05-19 22:06:27.748 3680-3680/com.swooby.alfred D/AudioManager: AudioManager dispatching onAudioFocusChange(1) for android.media.AudioManager@ab99fa2com.smartfoo.android.core.texttospeech.FooTextToSpeech$1@8f88433
05-19 22:06:27.748 3680-3680/com.swooby.alfred I/FooTextToSpeech: T3680 onAudioFocusChange(focusChange=AUDIOFOCUS_GAIN(1))
05-19 22:06:28.733 3680-3698/com.swooby.alfred I/FooTextToSpeech: T3698 audioFocusStop()
Spotify resumes...meanwhile Google Now got the shaft and was never able to record audio
*/
        // For more info, read:
        //  https://developer.android.com/training/managing-audio/audio-focus.html
        //  http://android-developers.blogspot.com/2013/08/respecting-audio-focus.html
        /*
        switch (focusChange)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                mState.audioFocusGranted = true;

                if (mState.released)
                {
                    initializeMediaPlayer();
                }

                switch (mState.lastKnownAudioFocusState)
                {
                    case UNKNOWN:
                        if (mState.state == PlayState.PLAY && !mPlayer.isPlaying())
                        {
                            mPlayer.start();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if (mState.wasPlayingWhenTransientLoss)
                        {
                            mPlayer.start();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        restoreVolume();
                        break;
                }

                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                mState.userInitiatedState = false;
                mState.audioFocusGranted = false;
                teardown();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mState.userInitiatedState = false;
                mState.audioFocusGranted = false;
                mState.wasPlayingWhenTransientLoss = mPlayer.isPlaying();
                mPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mState.userInitiatedState = false;
                mState.audioFocusGranted = false;
                lowerVolume();
                break;
        }
        mState.lastKnownAudioFocusState = focusChange;
        */
    }
}
