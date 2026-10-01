package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
    data class FinalResult(val text: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _soundRms = MutableStateFlow(0f)
    val soundRms: StateFlow<Float> = _soundRms.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(locale: Locale = Locale("es", "419")) {
        try {
            stopListening()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _speechState.value = SpeechState.Error("Reconocimiento de voz no disponible en este dispositivo.")
                return
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechState.value = SpeechState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        _speechState.value = SpeechState.Listening
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize 0..10 dB
                        val normalized = (rmsdB.coerceIn(0f, 10f) / 10f)
                        _soundRms.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _soundRms.value = 0f
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono no concedido"
                            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No entendí, ¿podés repetir?"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                            SpeechRecognizer.ERROR_SERVER -> "Error en el servidor de voz"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz"
                            else -> "Error al escuchar"
                        }
                        _speechState.value = SpeechState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _speechState.value = SpeechState.FinalResult(text)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _speechState.value = SpeechState.PartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error(e.localizedMessage ?: "Error al iniciar micrófono")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            _soundRms.value = 0f
        } catch (_: Exception) {}
    }

    /**
     * Allows simulated speech input or manual text input in environments where microphone isn't physically available.
     */
    fun simulateInput(text: String) {
        _speechState.value = SpeechState.Listening
        _speechState.value = SpeechState.FinalResult(text)
    }

    fun reset() {
        _speechState.value = SpeechState.Idle
        _soundRms.value = 0f
    }
}
