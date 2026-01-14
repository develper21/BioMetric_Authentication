package com.biometric.voiceeye.auth.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ErrorHandler(private val context: Context) {
    
    private val TAG = "ErrorHandler"
    
    /**
     * Global exception handler for coroutines
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable, "Coroutine error occurred")
    }
    
    /**
     * Handle authentication errors with user-friendly messages
     */
    fun handleAuthenticationError(error: Throwable, step: String): String {
        val userMessage = when {
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                "Authentication timed out. Please try again."
            }
            error.message?.contains("permission", ignoreCase = true) == true -> {
                "Required permissions not granted. Please check app settings."
            }
            error.message?.contains("camera", ignoreCase = true) == true -> {
                "Camera error. Please check if camera is available."
            }
            error.message?.contains("microphone", ignoreCase = true) == true -> {
                "Microphone error. Please check if microphone is available."
            }
            error.message?.contains("network", ignoreCase = true) == true -> {
                "Network error. Please check your internet connection."
            }
            error.message?.contains("biometric", ignoreCase = true) == true -> {
                "Biometric hardware error. Please try again."
            }
            step.contains("voice", ignoreCase = true) -> {
                "Voice authentication failed. Please speak clearly and try again."
            }
            step.contains("eye", ignoreCase = true) -> {
                "Eye scan failed. Please ensure proper lighting and try again."
            }
            step.contains("iris", ignoreCase = true) -> {
                "Iris authentication failed. Please position your eyes properly."
            }
            else -> {
                "Authentication failed. Please try again."
            }
        }
        
        Log.e(TAG, "Authentication error in $step: ${error.message}", error)
        return userMessage
    }
    
    /**
     * Handle setup errors
     */
    fun handleSetupError(error: Throwable, setupType: String): String {
        val userMessage = when {
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                "Setup timed out. Please try again."
            }
            error.message?.contains("permission", ignoreCase = true) == true -> {
                "Required permissions not granted. Please enable permissions in settings."
            }
            setupType.contains("voice", ignoreCase = true) -> {
                "Voice setup failed. Please ensure microphone is working and try again."
            }
            setupType.contains("eye", ignoreCase = true) -> {
                "Eye setup failed. Please ensure camera is working and try again."
            }
            else -> {
                "Setup failed. Please try again."
            }
        }
        
        Log.e(TAG, "Setup error for $setupType: ${error.message}", error)
        return userMessage
    }
    
    /**
     * Handle general errors
     */
    fun handleError(error: Throwable, context: String = "Unknown error") {
        val message = when {
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                "Operation timed out. Please try again."
            }
            error.message?.contains("permission", ignoreCase = true) == true -> {
                "Permission denied. Please check app permissions."
            }
            error.message?.contains("network", ignoreCase = true) == true -> {
                "Network error. Please check your connection."
            }
            else -> {
                "An error occurred. Please try again."
            }
        }
        
        Log.e(TAG, "$context: ${error.message}", error)
        showToast(message)
    }
    
    /**
     * Show error toast to user
     */
    private fun showToast(message: String) {
        CoroutineScope(coroutineExceptionHandler).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Log error with context
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Check if error is recoverable
     */
    fun isRecoverableError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: return false
        return !message.contains("security", ignoreCase = true) &&
               !message.contains("corrupt", ignoreCase = true) &&
               !message.contains("invalid", ignoreCase = true) &&
               !message.contains("unauthorized", ignoreCase = true)
    }
    
    /**
     * Get retry suggestion based on error
     */
    fun getRetrySuggestion(error: Throwable): String {
        return when {
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                "Please try again with a stable connection."
            }
            error.message?.contains("camera", ignoreCase = true) == true -> {
                "Please ensure camera is not in use by another app."
            }
            error.message?.contains("microphone", ignoreCase = true) == true -> {
                "Please ensure microphone is not in use by another app."
            }
            error.message?.contains("permission", ignoreCase = true) == true -> {
                "Please grant the required permissions and try again."
            }
            else -> {
                "Please wait a moment and try again."
            }
        }
    }
}
