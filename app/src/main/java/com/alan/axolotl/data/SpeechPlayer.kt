package com.alan.axolotl.data

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * Speaks short pieces of text out loud.
 *
 * Wrapping [TextToSpeech] behind this interface keeps the Android speech API out
 * of the ViewModels, so they stay unit-testable with a no-op fake.
 */
interface SpeechPlayer {
    /** Speaks [text], interrupting anything already being spoken. No-op until the engine is ready. */
    fun speak(text: String)

    /** Releases the underlying engine. Call once the owner is destroyed. */
    fun shutdown()
}

class AndroidSpeechPlayer @Inject constructor(
    @ApplicationContext context: Context
) : SpeechPlayer {

    private var isReady = false
    private var tts: TextToSpeech? = null

    init {
        // The language can only be set once the engine reports success, so it is
        // configured inside the init callback rather than straight after construction.
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isReady = true
            }
        }
    }

    override fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
