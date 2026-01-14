package com.biometric.voiceeye.auth.utils

import android.content.Context
import com.biometric.voiceeye.auth.eye.IrisPatternMatcher
import com.biometric.voiceeye.auth.voice.VoicePatternMatcher

class BiometricManager(private val context: Context) {
    
    private val voicePatternMatcher = VoicePatternMatcher(context)
    private val irisPatternMatcher = IrisPatternMatcher(context)
    
    /**
     * Check if voice authentication is set up
     */
    fun isVoiceSetup(): Boolean {
        return voicePatternMatcher.isVoicePrintStored()
    }
    
    /**
     * Check if eye/iris authentication is set up
     */
    fun isEyeSetup(): Boolean {
        return irisPatternMatcher.isIrisPatternStored()
    }
    
    /**
     * Check if complete biometric setup is done
     */
    fun isCompleteSetup(): Boolean {
        return isVoiceSetup() && isEyeSetup()
    }
    
    /**
     * Clear all biometric data
     */
    fun clearAllBiometricData() {
        voicePatternMatcher.clearVoicePrint()
        irisPatternMatcher.clearIrisPattern()
    }
    
    /**
     * Get setup status information
     */
    fun getSetupStatus(): SetupStatus {
        return SetupStatus(
            voiceSetup = isVoiceSetup(),
            eyeSetup = isEyeSetup(),
            complete = isCompleteSetup()
        )
    }
    
    data class SetupStatus(
        val voiceSetup: Boolean,
        val eyeSetup: Boolean,
        val complete: Boolean
    )
}
