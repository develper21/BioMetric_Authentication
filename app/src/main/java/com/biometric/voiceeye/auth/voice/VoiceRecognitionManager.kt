package com.biometric.voiceeye.auth.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VoiceRecognitionManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val TAG = "VoiceRecognition"
    
    companion object {
        const val WAKE_WORD = "open phone"
        const val VOICE_COMMAND_TIMEOUT = 10000L // 10 seconds
    }
    
    init {
        initializeSpeechRecognizer()
    }
    
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            Log.e(TAG, "Speech recognition not available")
        }
    }
    
    suspend fun listenForWakeWord(): String? = withTimeoutOrNull(VOICE_COMMAND_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech")
                }
                
                override fun onRmsChanged(rmsdB: Float) {}
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "End of speech")
                }
                
                override fun onError(error: Int) {
                    val errorMessage = getErrorMessage(error)
                    Log.e(TAG, "Speech recognition error: $errorMessage")
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception(errorMessage))
                    }
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0].lowercase().trim()
                        Log.d(TAG, "Recognized: $recognizedText")
                        
                        if (continuation.isActive) {
                            continuation.resume(recognizedText)
                        }
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partialText = matches[0].lowercase().trim()
                        Log.d(TAG, "Partial: $partialText")
                        
                        // Check for wake word in partial results
                        if (partialText.contains(WAKE_WORD)) {
                            if (continuation.isActive) {
                                continuation.resume(partialText)
                            }
                        }
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            
            speechRecognizer?.setRecognitionListener(listener)
            speechRecognizer?.startListening(intent)
            
            continuation.invokeOnCancellation {
                speechRecognizer?.destroy()
            }
        }
    }
    
    suspend fun captureVoicePrint(): String? = withTimeoutOrNull(VOICE_COMMAND_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Voice print capture ready")
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Voice print capture started")
                }
                
                override fun onRmsChanged(rmsdB: Float) {}
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "Voice print capture ended")
                }
                
                override fun onError(error: Int) {
                    val errorMessage = getErrorMessage(error)
                    Log.e(TAG, "Voice print capture error: $errorMessage")
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception(errorMessage))
                    }
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val voicePrint = matches[0]
                        Log.d(TAG, "Voice print captured: $voicePrint")
                        
                        if (continuation.isActive) {
                            continuation.resume(voicePrint)
                        }
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            
            speechRecognizer?.setRecognitionListener(listener)
            speechRecognizer?.startListening(intent)
            
            continuation.invokeOnCancellation {
                speechRecognizer?.destroy()
            }
        }
    }
    
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error: $errorCode"
        }
    }
    
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    fun isWakeWordCommand(text: String): Boolean {
        return text.lowercase().contains(WAKE_WORD)
    }
}
