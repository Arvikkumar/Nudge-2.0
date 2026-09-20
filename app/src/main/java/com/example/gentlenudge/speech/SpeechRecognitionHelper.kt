package com.example.gentlenudge.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

enum class VoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING
}

class SpeechRecognitionHelper(
    private val context: Context,
    private val onTextRecognized: (String, Boolean) -> Unit, // text, isFinal
    private val onListeningStateChanged: (Boolean) -> Unit = {},
    private val onStateChanged: (VoiceInputState) -> Unit = {},
    private val onRmsChangedState: (Float) -> Unit = {},
    private val onErrorOccurred: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentState = VoiceInputState.IDLE
    private var isStarting = false

    fun isListeningActive(): Boolean = isListening
    fun getState(): VoiceInputState = currentState

    private fun setState(state: VoiceInputState) {
        mainHandler.post {
            currentState = state
            isListening = (state == VoiceInputState.LISTENING || state == VoiceInputState.PROCESSING)
            try {
                onListeningStateChanged(state == VoiceInputState.LISTENING)
                onStateChanged(state)
            } catch (e: Exception) {
                Log.e("SpeechHelper", "Error in state change callbacks: ${e.message}")
            }
        }
    }

    fun startListening(): Boolean {
        if (isStarting) return false
        isStarting = true

        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                setState(VoiceInputState.IDLE)
                onErrorOccurred("Speech recognition is not available on this device.")
                isStarting = false
                return false
            }

            // Safely stop any previous instance first
            safelyCleanupRecognizer()

            val recognizer = try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                Log.e("SpeechHelper", "Failed to create SpeechRecognizer: ${e.message}")
                null
            }

            if (recognizer == null) {
                setState(VoiceInputState.IDLE)
                onErrorOccurred("Could not initialize voice recorder. You can type directly.")
                isStarting = false
                return false
            }

            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY)
                }
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    setState(VoiceInputState.LISTENING)
                }

                override fun onBeginningOfSpeech() {
                    setState(VoiceInputState.LISTENING)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    try {
                        onRmsChangedState(rmsdB)
                    } catch (_: Exception) {}
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    setState(VoiceInputState.PROCESSING)
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch any words. Tap mic to speak again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic when ready to speak."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Please check microphone."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue for voice recognition."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognition busy. Tap to retry."
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition not ready. Tap to retry."
                        else -> "Could not capture voice. Tap mic to retry."
                    }
                    Log.w("SpeechHelper", "SpeechRecognizer error: $error ($message)")
                    safelyCleanupRecognizer()
                    setState(VoiceInputState.IDLE)
                    mainHandler.post {
                        try {
                            onErrorOccurred(message)
                        } catch (e: Exception) {
                            Log.e("SpeechHelper", "Error in onErrorOccurred callback: ${e.message}")
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognized = matches?.firstOrNull()
                    safelyCleanupRecognizer()
                    setState(VoiceInputState.IDLE)
                    if (!recognized.isNullOrBlank()) {
                        mainHandler.post {
                            try {
                                onTextRecognized(recognized, true)
                            } catch (e: Exception) {
                                Log.e("SpeechHelper", "Error in onTextRecognized: ${e.message}")
                            }
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognized = matches?.firstOrNull()
                    if (!recognized.isNullOrBlank()) {
                        mainHandler.post {
                            try {
                                onTextRecognized(recognized, false)
                            } catch (e: Exception) {
                                Log.e("SpeechHelper", "Error in onPartialResults: ${e.message}")
                            }
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer = recognizer
            try {
                recognizer.startListening(recognizerIntent)
                setState(VoiceInputState.LISTENING)
                isStarting = false
                return true
            } catch (e: Exception) {
                Log.e("SpeechHelper", "Failed calling startListening on recognizer: ${e.message}", e)
                safelyCleanupRecognizer()
                setState(VoiceInputState.IDLE)
                onErrorOccurred("Could not start microphone recording.")
                isStarting = false
                return false
            }
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Failed to start speech recognizer: ${e.message}", e)
            safelyCleanupRecognizer()
            setState(VoiceInputState.IDLE)
            onErrorOccurred("Could not start speech recognition.")
            isStarting = false
            return false
        }
    }

    private fun safelyCleanupRecognizer() {
        val recognizer = speechRecognizer
        speechRecognizer = null
        if (recognizer != null) {
            mainHandler.post {
                try {
                    recognizer.stopListening()
                } catch (_: Exception) {}
                try {
                    recognizer.cancel()
                } catch (_: Exception) {}
                try {
                    recognizer.destroy()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopListening() {
        safelyCleanupRecognizer()
        setState(VoiceInputState.IDLE)
    }

    fun destroy() {
        stopListening()
    }
}

