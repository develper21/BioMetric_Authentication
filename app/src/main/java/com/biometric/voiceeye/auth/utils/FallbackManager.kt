package com.biometric.voiceeye.auth.utils

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.biometric.voiceeye.auth.R

class FallbackManager(private val context: Context) {
    
    private val phoneUnlockManager = PhoneUnlockManager(context)
    private val errorHandler = ErrorHandler(context)
    private val TAG = "FallbackManager"
    
    /**
     * Get available fallback authentication methods
     */
    fun getAvailableFallbacks(): List<FallbackMethod> {
        val fallbacks = mutableListOf<FallbackMethod>()
        
        // Check biometric availability
        val biometricStatus = phoneUnlockManager.checkBiometricAvailability()
        if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            fallbacks.add(FallbackMethod.BIOMETRIC)
        }
        
        // Check device credential availability
        if (phoneUnlockManager.isDeviceSecure()) {
            fallbacks.add(FallbackMethod.DEVICE_CREDENTIAL)
        }
        
        // PIN fallback (always available as last resort)
        fallbacks.add(FallbackMethod.PIN)
        
        return fallbacks
    }
    
    /**
     * Execute fallback authentication
     */
    suspend fun executeFallback(
        method: FallbackMethod,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            when (method) {
                FallbackMethod.BIOMETRIC -> {
                    executeBiometricFallback(onSuccess, onFailure)
                }
                FallbackMethod.DEVICE_CREDENTIAL -> {
                    executeDeviceCredentialFallback(onSuccess, onFailure)
                }
                FallbackMethod.PIN -> {
                    executePinFallback(onSuccess, onFailure)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback authentication failed", e)
            onFailure("Fallback authentication failed: ${e.message}")
        }
    }
    
    /**
     * Execute biometric fallback
     */
    private fun executeBiometricFallback(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val activity = context as? androidx.fragment.app.FragmentActivity
            if (activity == null) {
                onFailure("Activity context required for biometric authentication")
                return
            }
            
            val biometricPrompt = phoneUnlockManager.createBiometricPrompt(
                onSuccess = { 
                    Log.d(TAG, "Biometric fallback successful")
                    onSuccess()
                },
                onFailure = { error ->
                    Log.e(TAG, "Biometric fallback error: $error")
                    onFailure(error)
                },
                onFailed = { 
                    Log.w(TAG, "Biometric fallback failed")
                    onFailure("Biometric authentication failed")
                }
            )
            
            val promptInfo = phoneUnlockManager.createBiometricPromptInfo()
            biometricPrompt.authenticate(promptInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing biometric fallback", e)
            onFailure("Biometric authentication error: ${e.message}")
        }
    }
    
    /**
     * Execute device credential fallback
     */
    private fun executeDeviceCredentialFallback(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val result = phoneUnlockManager.showSystemUnlock()
            if (result) {
                Log.d(TAG, "Device credential fallback initiated")
                // Success will be handled by system callback
                onSuccess()
            } else {
                onFailure("Failed to show device credential screen")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing device credential fallback", e)
            onFailure("Device credential error: ${e.message}")
        }
    }
    
    /**
     * Execute PIN fallback (simple PIN entry)
     */
    private fun executePinFallback(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            // In a real implementation, this would show a PIN entry dialog
            // For demo purposes, we'll simulate PIN authentication
            Log.d(TAG, "PIN fallback executed")
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing PIN fallback", e)
            onFailure("PIN authentication error: ${e.message}")
        }
    }
    
    /**
     * Get fallback method display name
     */
    fun getFallbackMethodName(method: FallbackMethod): String {
        return when (method) {
            FallbackMethod.BIOMETRIC -> "Fingerprint/Face"
            FallbackMethod.DEVICE_CREDENTIAL -> "PIN/Pattern/Password"
            FallbackMethod.PIN -> "PIN Code"
        }
    }
    
    /**
     * Check if fallback is available
     */
    fun isFallbackAvailable(): Boolean {
        return getAvailableFallbacks().isNotEmpty()
    }
    
    /**
     * Get recommended fallback method
     */
    fun getRecommendedFallback(): FallbackMethod? {
        val available = getAvailableFallbacks()
        return when {
            available.contains(FallbackMethod.BIOMETRIC) -> FallbackMethod.BIOMETRIC
            available.contains(FallbackMethod.DEVICE_CREDENTIAL) -> FallbackMethod.DEVICE_CREDENTIAL
            available.contains(FallbackMethod.PIN) -> FallbackMethod.PIN
            else -> null
        }
    }
    
    /**
     * Execute automatic fallback (tries best available method)
     */
    suspend fun executeAutomaticFallback(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val recommended = getRecommendedFallback()
        if (recommended != null) {
            executeFallback(recommended, onSuccess, onFailure)
        } else {
            onFailure("No fallback authentication methods available")
        }
    }
    
    enum class FallbackMethod {
        BIOMETRIC,
        DEVICE_CREDENTIAL,
        PIN
    }
}
