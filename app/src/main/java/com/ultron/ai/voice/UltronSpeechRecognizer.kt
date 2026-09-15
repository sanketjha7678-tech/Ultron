package com.ultron.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class UltronSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onErrorState: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@UltronSpeechRecognizer)
            }
        }
    }

    fun startListening() {
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) { onRmsChanged(rmsdB) }
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection required"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            else -> "Speech engine error"
        }
        onErrorState(message)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onResult(matches[0])
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        speechRecognizer?.destroy()
    }
}
