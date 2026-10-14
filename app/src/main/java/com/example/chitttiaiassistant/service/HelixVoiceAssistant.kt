package com.example.chitttiaiassistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.chitttiaiassistant.model.AssistantState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class HelixVoiceAssistant(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initSpeechRecognizer()
        textToSpeech = TextToSpeech(context, this)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _assistantState.value = AssistantState.LISTENING
                        _liveTranscript.value = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        _assistantState.value = AssistantState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _audioLevel.value = rmsdB.coerceAtLeast(0f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _assistantState.value = AssistantState.THINKING
                    }

                    override fun onError(error: Int) {
                        _assistantState.value = AssistantState.ERROR
                        _liveTranscript.value = "Sorry, I didn't catch that. Tap the mic to try again."
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _liveTranscript.value = text
                            _assistantState.value = AssistantState.THINKING
                            onCommandRecognized(text)
                        } else {
                            _assistantState.value = AssistantState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _liveTranscript.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            val voices = textToSpeech?.voices
            val standardVoice = voices?.find { it.name.contains("en-us", ignoreCase = true) }
            if (standardVoice != null) {
                textToSpeech?.voice = standardVoice
            }
            textToSpeech?.setSpeechRate(1.0f)
            textToSpeech?.setPitch(1.0f)

            isTtsReady = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _assistantState.value = AssistantState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    scope.launch {
                        _assistantState.value = AssistantState.IDLE
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    scope.launch {
                        _assistantState.value = AssistantState.IDLE
                    }
                }
            })
        }
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
            _assistantState.value = AssistantState.LISTENING
            _liveTranscript.value = "Listening..."
        } else {
            _assistantState.value = AssistantState.LISTENING
            _liveTranscript.value = "Speech recognition unavailable on device."
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun speak(text: String) {
        if (isTtsReady && text.isNotEmpty()) {
            _assistantState.value = AssistantState.SPEAKING
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HELIX_UTTERANCE_ID")
        } else {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun setSpeechRateAndPitch(rate: Float, pitch: Float) {
        textToSpeech?.setSpeechRate(rate)
        textToSpeech?.setPitch(pitch)
    }

    fun cancel() {
        speechRecognizer?.cancel()
        textToSpeech?.stop()
        _assistantState.value = AssistantState.IDLE
        _liveTranscript.value = ""
    }

    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
