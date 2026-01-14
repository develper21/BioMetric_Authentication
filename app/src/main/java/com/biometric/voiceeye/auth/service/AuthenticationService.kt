package com.biometric.voiceeye.auth.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.biometric.voiceeye.auth.eye.EyeScannerManager
import com.biometric.voiceeye.auth.eye.IrisPatternMatcher
import com.biometric.voiceeye.auth.voice.VoicePatternMatcher
import com.biometric.voiceeye.auth.voice.VoiceRecognitionManager
import com.biometric.voiceeye.auth.utils.PhoneUnlockManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthenticationService : LifecycleService() {
    
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager
    private lateinit var voicePatternMatcher: VoicePatternMatcher
    private lateinit var eyeScannerManager: EyeScannerManager
    private lateinit var irisPatternMatcher: IrisPatternMatcher
    private lateinit var phoneUnlockManager: PhoneUnlockManager
    
    private val TAG = "AuthService"
    
    companion object {
        const val ACTION_START_AUTH = "com.biometric.voiceeye.auth.START_AUTH"
        const val ACTION_AUTH_SUCCESS = "com.biometric.voiceeye.auth.AUTH_SUCCESS"
        const val ACTION_AUTH_FAILED = "com.biometric.voiceeye.auth.AUTH_FAILED"
        const val ACTION_AUTH_PROGRESS = "com.biometric.voiceeye.auth.AUTH_PROGRESS"
        
        const val EXTRA_STATUS_MESSAGE = "status_message"
        const val EXTRA_AUTH_STEP = "auth_step"
        
        const val STEP_VOICE_WAKE_WORD = "voice_wake_word"
        const val STEP_VOICE_AUTH = "voice_auth"
        const val STEP_EYE_SCAN = "eye_scan"
        const val STEP_IRIS_AUTH = "iris_auth"
        const val STEP_COMPLETE = "complete"
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeComponents()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_AUTH -> {
                startAuthenticationProcess()
            }
        }
        
        return START_STICKY
    }
    
    private fun initializeComponents() {
        voiceRecognitionManager = VoiceRecognitionManager(this)
        voicePatternMatcher = VoicePatternMatcher(this)
        eyeScannerManager = EyeScannerManager(this, this)
        irisPatternMatcher = IrisPatternMatcher(this)
        phoneUnlockManager = PhoneUnlockManager(this)
    }
    
    private fun startAuthenticationProcess() {
        lifecycleScope.launch {
            try {
                // Step 1: Voice Wake Word Detection
                broadcastProgress(STEP_VOICE_WAKE_WORD, "Listening for wake word...")
                val wakeWordResult = voiceRecognitionManager.listenForWakeWord()
                
                if (wakeWordResult == null || !voiceRecognitionManager.isWakeWordCommand(wakeWordResult)) {
                    broadcastAuthFailed("Wake word not detected")
                    return@launch
                }
                
                broadcastProgress(STEP_VOICE_WAKE_WORD, "Wake word detected!")
                delay(1000)
                
                // Step 2: Voice Authentication
                broadcastProgress(STEP_VOICE_AUTH, "Authenticating voice...")
                val voiceAuthSuccess = voicePatternMatcher.authenticateVoice(wakeWordResult)
                
                if (!voiceAuthSuccess) {
                    broadcastAuthFailed("Voice authentication failed")
                    return@launch
                }
                
                broadcastProgress(STEP_VOICE_AUTH, "Voice authenticated!")
                delay(1000)
                
                // Step 3: Eye Scanning
                broadcastProgress(STEP_EYE_SCAN, "Starting eye scan...")
                val eyeScanResult = eyeScannerManager.startEyeScan()
                
                when (eyeScanResult) {
                    is EyeScannerManager.EyeScanResult.Success -> {
                        if (!eyeScanResult.isLive) {
                            broadcastAuthFailed("Liveness detection failed")
                            return@launch
                        }
                        
                        broadcastProgress(STEP_EYE_SCAN, "Eye scan successful!")
                        delay(1000)
                        
                        // Step 4: Iris Authentication
                        broadcastProgress(STEP_IRIS_AUTH, "Authenticating iris pattern...")
                        val irisAuthSuccess = irisPatternMatcher.authenticateIris(eyeScanResult.bitmap)
                        
                        if (!irisAuthSuccess) {
                            broadcastAuthFailed("Iris authentication failed")
                            return@launch
                        }
                        
                        broadcastProgress(STEP_IRIS_AUTH, "Iris authenticated!")
                        delay(1000)
                        
                        // Step 5: Authentication Complete
                        broadcastProgress(STEP_COMPLETE, "Authentication successful!")
                        unlockPhone()
                        broadcastAuthSuccess()
                    }
                    is EyeScannerManager.EyeScanResult.Error -> {
                        broadcastAuthFailed("Eye scan failed: ${eyeScanResult.message}")
                        return@launch
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Authentication process error", e)
                broadcastAuthFailed("Authentication error: ${e.message}")
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun broadcastProgress(step: String, message: String) {
        val intent = Intent(ACTION_AUTH_PROGRESS).apply {
            putExtra(EXTRA_AUTH_STEP, step)
            putExtra(EXTRA_STATUS_MESSAGE, message)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Progress: $step - $message")
    }
    
    private fun broadcastAuthSuccess() {
        val intent = Intent(ACTION_AUTH_SUCCESS).apply {
            putExtra(EXTRA_STATUS_MESSAGE, "Authentication successful!")
        }
        sendBroadcast(intent)
        Log.d(TAG, "Authentication successful")
    }
    
    private fun broadcastAuthFailed(reason: String) {
        val intent = Intent(ACTION_AUTH_FAILED).apply {
            putExtra(EXTRA_STATUS_MESSAGE, reason)
        }
        sendBroadcast(intent)
        Log.e(TAG, "Authentication failed: $reason")
    }
    
    private fun unlockPhone() {
        try {
            val unlockResult = phoneUnlockManager.unlockDevice()
            
            when (unlockResult) {
                is PhoneUnlockManager.UnlockResult.Success -> {
                    Log.d(TAG, "Phone unlocked: ${unlockResult.message}")
                }
                is PhoneUnlockManager.UnlockResult.Error -> {
                    Log.e(TAG, "Unlock failed: ${unlockResult.message}")
                    // Fallback to simulation for demo
                    phoneUnlockManager.simulateUnlock()
                }
                PhoneUnlockManager.UnlockResult.BiometricAvailable -> {
                    Log.d(TAG, "Biometric authentication available as fallback")
                    // Could show biometric prompt here
                }
                PhoneUnlockManager.UnlockResult.SystemUnlockShown -> {
                    Log.d(TAG, "System unlock screen shown")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking phone", e)
            // Fallback simulation
            phoneUnlockManager.simulateUnlock()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        voiceRecognitionManager.destroy()
        eyeScannerManager.release()
        
        Log.d(TAG, "Authentication service destroyed")
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
