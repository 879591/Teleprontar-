package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    MATCHING,
    ERROR,
    UNSUPPORTED
}

class VoiceFollowManager(
    private val context: Context,
    private val onWordMatched: (wordIndex: Int, progressFraction: Float) -> Unit
) {
    private val tag = "VoiceFollowManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private var scriptWords: List<String> = emptyList()
    private var isRunning = false
    private var currentLanguage = "hi-IN"

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _latestSpokenText = MutableStateFlow("")
    val latestSpokenText: StateFlow<String> = _latestSpokenText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun setScript(content: String, language: String = "hi-IN") {
        currentLanguage = language
        scriptWords = content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    fun startListening() {
        if (!isAvailable()) {
            _voiceState.value = VoiceState.UNSUPPORTED
            _errorMessage.value = "Speech recognition is not available on this device"
            return
        }

        isRunning = true
        _errorMessage.value = null
        initAndListen()
    }

    private fun initAndListen() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            Log.e(tag, "Error starting voice recognition: ${e.message}")
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = e.message ?: "Failed to start speech recognition"
        }
    }

    fun stopListening() {
        isRunning = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping speech recognition: ${e.message}")
        }
        _voiceState.value = VoiceState.IDLE
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (isRunning) _voiceState.value = VoiceState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            if (isRunning) _voiceState.value = VoiceState.MATCHING
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            if (isRunning) _voiceState.value = VoiceState.LISTENING
        }

        override fun onError(error: Int) {
            if (!isRunning) return
            // Handle common non-fatal timeouts by restarting gracefully
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    if (isRunning) {
                        initAndListen()
                    }
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    _voiceState.value = VoiceState.ERROR
                    _errorMessage.value = "Microphone permission required for Voice Follow"
                    isRunning = false
                }
                else -> {
                    // Try to re-listen after transient network or audio glitches
                    if (isRunning) {
                        initAndListen()
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            processResults(results)
            // Continue continuous voice tracking while running
            if (isRunning) {
                initAndListen()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            processResults(partialResults)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processResults(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        if (matches.isEmpty() || scriptWords.isEmpty()) return

        val spoken = matches[0]
        _latestSpokenText.value = spoken
        val spokenTokens = spoken.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (spokenTokens.isEmpty()) return

        // Find the best match in the script
        var bestIndex = -1
        var bestScore = 0

        val cleanScriptTokens = scriptWords.map { cleanWord(it) }

        for (i in cleanScriptTokens.indices) {
            var score = 0
            for (j in spokenTokens.indices) {
                val scriptIdx = i + j
                if (scriptIdx < cleanScriptTokens.size) {
                    val scriptToken = cleanScriptTokens[scriptIdx]
                    val spokenToken = cleanWord(spokenTokens[j])
                    if (scriptToken == spokenToken || (scriptToken.length > 3 && scriptToken.contains(spokenToken))) {
                        score++
                    }
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestIndex = i + (spokenTokens.size - 1)
            }
        }

        if (bestIndex >= 0 && bestScore >= 1) {
            val clampedIndex = bestIndex.coerceIn(0, scriptWords.size - 1)
            val fraction = clampedIndex.toFloat() / scriptWords.size.toFloat()
            onWordMatched(clampedIndex, fraction)
        }
    }

    private fun cleanWord(word: String): String {
        return word.lowercase(Locale.ROOT)
            .replace(Regex("[।.,!?:;\"'()\\-\\[\\]{}]"), "")
            .trim()
    }
}
