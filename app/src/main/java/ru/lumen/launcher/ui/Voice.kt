package ru.lumen.launcher.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Голос персонажа на встроенных средствах Android:
 * TextToSpeech для речи и SpeechRecognizer для слуха.
 */
class VoiceController(
    context: Context,
    onResult: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val resultHandler = onResult

    var speaking by mutableStateOf(false)
        private set
    var listening by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    var ready by mutableStateOf(false)
        private set

    private var textToSpeech: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var pitch: Float = 1f

    init {
        textToSpeech = TextToSpeech(appContext) { status -> onEngineReady(status) }
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                speaking = true
            }

            override fun onDone(utteranceId: String?) {
                speaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speaking = false
            }
        })
    }

    private fun onEngineReady(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val engine = textToSpeech ?: return
            engine.language = Locale("ru", "RU")
            engine.setPitch(pitch)
        }
    }

    fun setPitch(value: Float) {
        pitch = value
        textToSpeech?.setPitch(value)
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun speak(text: String) {
        val engine = textToSpeech ?: return
        if (text.isBlank()) return
        engine.setPitch(pitch)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lumen-${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        speaking = false
    }

    fun listen() {
        val engine = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also { created ->
            created.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    listening = false
                }

                override fun onError(error: Int) {
                    listening = false
                    partial = ""
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    partial = ""
                    if (text.isNotEmpty()) resultHandler(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            recognizer = created
            created
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        stopSpeaking()
        engine.startListening(intent)
        listening = true
    }

    fun stopListening() {
        recognizer?.stopListening()
        listening = false
        partial = ""
    }

    fun release() {
        runCatching {
            recognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        recognizer = null
        textToSpeech = null
    }
}

@Composable
fun rememberVoiceController(onResult: (String) -> Unit): VoiceController {
    val context = LocalContext.current
    val handler by rememberUpdatedState(onResult)
    val controller = remember { VoiceController(context) { text -> handler(text) } }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}