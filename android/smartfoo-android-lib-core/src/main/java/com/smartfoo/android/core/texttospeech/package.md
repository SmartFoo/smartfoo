# Package com.smartfoo.android.core.texttospeech

Text-to-speech engine wrapper with queuing and audio-focus integration. `FooTextToSpeech` is a singleton that manages a single `android.speech.tts.TextToSpeech` engine instance, serialises utterances through its own queue using `sequenceId`/`utteranceId` tracking, and integrates with `FooAudioFocusController` to duck or pause other audio automatically. `FooTextToSpeechBuilder` assembles composite utterances from speech, silence, and earcon parts. `FooTextToSpeechHelper` provides convenience methods for common speak patterns.
