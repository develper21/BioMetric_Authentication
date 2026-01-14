package com.biometric.voiceeye.auth.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.biometric.voiceeye.auth.R

class PhoneUnlockManager(private val context: Context) {
    
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val TAG = "PhoneUnlock"
    
    /**
     * Check if device is locked
     */
    fun isDeviceLocked(): Boolean {
        return keyguardManager.isKeyguardLocked
    }
    
    /**
     * Check if device is secure (has PIN/pattern/password)
     */
    fun isDeviceSecure(): Boolean {
        return keyguardManager.isKeyguardSecure
    }
    
    /**
     * Wake up the device screen
     */
    fun wakeUpDevice() {
        try {
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "VoiceEye:WakeUp"
            )
            wakeLock.acquire(1000) // Wake up for 1 second
            wakeLock.release()
            
            Log.d(TAG, "Device woken up")
        } catch (e: Exception) {
            Log.e(TAG, "Error waking up device", e)
        }
    }
    
    /**
     * Show system unlock screen
     */
    fun showSystemUnlock(): Boolean {
        return try {
            if (keyguardManager.isKeyguardSecure) {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "Voice + Eye Authentication",
                    "Verify your identity to unlock"
                )
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                
                Log.d(TAG, "System unlock screen shown")
                true
            } else {
                Log.w(TAG, "Device is not secure")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing system unlock", e)
            false
        }
    }
    
    /**
     * Check biometric hardware availability
     */
    fun checkBiometricAvailability(): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }
    
    /**
     * Get biometric availability message
     */
    fun getBiometricAvailabilityMessage(): String {
        return when (checkBiometricAvailability()) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometric authentication available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric credentials enrolled"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Biometric not supported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Biometric status unknown"
            else -> "Unknown biometric error"
        }
    }
    
    /**
     * Create biometric prompt for fallback authentication
     */
    fun createBiometricPrompt(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onFailed: () -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded")
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e(TAG, "Biometric authentication error: $errString")
                onFailure(errString.toString())
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed")
                onFailed()
            }
        }
        
        return BiometricPrompt(
            context as androidx.fragment.app.FragmentActivity,
            executor,
            callback
        )
    }
    
    /**
     * Create biometric prompt info
     */
    fun createBiometricPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Voice + Eye Authentication")
            .setSubtitle("Fallback authentication method")
            .setDescription("Use your fingerprint or device credential to unlock")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .setConfirmationRequired(false)
            .build()
    }
    
    /**
     * Unlock device using available methods
     */
    fun unlockDevice(): UnlockResult {
        return try {
            // First wake up the device
            wakeUpDevice()
            
            when {
                !isDeviceLocked() -> {
                    Log.d(TAG, "Device already unlocked")
                    UnlockResult.Success("Device already unlocked")
                }
                !isDeviceSecure() -> {
                    Log.d(TAG, "Device not secure, unlocking directly")
                    UnlockResult.Success("Device unlocked (no security)")
                }
                checkBiometricAvailability() == BiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.d(TAG, "Biometric available, showing biometric prompt")
                    UnlockResult.BiometricAvailable
                }
                else -> {
                    Log.d(TAG, "Showing system unlock screen")
                    if (showSystemUnlock()) {
                        UnlockResult.SystemUnlockShown
                    } else {
                        UnlockResult.Error("Failed to show unlock screen")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking device", e)
            UnlockResult.Error("Unlock error: ${e.message}")
        }
    }
    
    /**
     * Simulate unlock for demo purposes
     */
    fun simulateUnlock(): UnlockResult {
        Log.d(TAG, "Simulating phone unlock...")
        wakeUpDevice()
        
        // In a real implementation, this would integrate with system APIs
        // For demo, we'll just return success
        return UnlockResult.Success("Phone unlocked successfully!")
    }
    
    sealed class UnlockResult {
        data class Success(val message: String) : UnlockResult()
        data class Error(val message: String) : UnlockResult()
        object BiometricAvailable : UnlockResult()
        object SystemUnlockShown : UnlockResult()
    }
}
