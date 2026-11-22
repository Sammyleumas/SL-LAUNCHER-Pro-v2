package com.sllauncher.pro.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speaks with the phone's own text-to-speech engine. Created lazily so the
 * engine is not loaded until SL first needs to talk.
 */
@Singleton
class Speaker @Inject constructor(@param:ApplicationContext private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    fun speak(text: String) {
        if (text.isBlank()) return
        if (tts == null) create()
        if (ready) say(text) else pending = text
    }

    private fun create() {
        // The init callback runs later on the main thread, after `tts` is assigned.
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.getDefault()
                pending?.let { say(it) }
            } else {
                Log.w("Speaker", "Text-to-speech unavailable (status $status)")
            }
            pending = null
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _speaking.value = true }
                override fun onDone(utteranceId: String?) { _speaking.value = false }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { _speaking.value = false }
                override fun onStop(utteranceId: String?, interrupted: Boolean) { _speaking.value = false }
            })
        }
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sl-${System.nanoTime()}")
    }

    /** Interrupts whatever SL is saying. */
    fun stop() {
        tts?.stop()
        _speaking.value = false
    }

    val isAvailable: Boolean get() = ready
}
