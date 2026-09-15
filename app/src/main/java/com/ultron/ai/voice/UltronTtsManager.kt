package com.ultron.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class UltronTtsManager(
    context: Context,
    private val onSpeechStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                configureFemaleVoice()
                isReady = true
                setupProgressListener()
            }
        }
    }

    private fun configureFemaleVoice() {
        tts?.let { engine ->
            engine.setPitch(1.05f)
            engine.setSpeechRate(0.98f)
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val femaleVoice = voices.firstOrNull { voice ->
                    voice.name.contains("female", ignoreCase = true) ||
                    voice.name.contains("en-us-x-sfg", ignoreCase = true) ||
                    voice.name.contains("network", ignoreCase = true)
                }
                femaleVoice?.let { engine.voice = it }
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeechStateChanged(true)
            }
            override fun onDone(utteranceId: String?) {
                onSpeechStateChanged(false)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onSpeechStateChanged(false)
            }
        })
    }

    fun speak(text: String) {
        if (!isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ULTRON_UTTERANCE_ID")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
