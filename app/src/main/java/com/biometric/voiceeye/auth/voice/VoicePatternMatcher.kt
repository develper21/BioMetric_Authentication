package com.biometric.voiceeye.auth.voice

import android.content.Context
import android.util.Log
import com.biometric.voiceeye.auth.utils.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.*
import kotlin.math.abs

class VoicePatternMatcher(private val context: Context) {
    
    private val secureStorage = SecureStorage(context)
    private val TAG = "VoicePatternMatcher"
    
    companion object {
        private const val VOICE_PRINT_KEY = "user_voice_print"
        private const val VOICE_FEATURES_KEY = "user_voice_features"
        private const val SIMILARITY_THRESHOLD = 0.85f // 85% similarity required
    }
    
    /**
     * Store user's voice print for future authentication
     */
    suspend fun storeVoicePrint(voiceSample: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val voiceFeatures = extractVoiceFeatures(voiceSample)
            val voicePrintHash = hashString(voiceSample)
            
            // Store both the hash and features
            secureStorage.storeData(VOICE_PRINT_KEY, voicePrintHash)
            secureStorage.storeData(VOICE_FEATURES_KEY, voiceFeatures.joinToString(","))
            
            Log.d(TAG, "Voice print stored successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error storing voice print", e)
            false
        }
    }
    
    /**
     * Authenticate user by comparing current voice sample with stored voice print
     */
    suspend fun authenticateVoice(voiceSample: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val storedVoicePrint = secureStorage.getData(VOICE_PRINT_KEY)
            val storedFeatures = secureStorage.getData(VOICE_FEATURES_KEY)
            
            if (storedVoicePrint == null || storedFeatures == null) {
                Log.w(TAG, "No voice print found for authentication")
                return@withContext false
            }
            
            // Compare voice prints
            val currentVoicePrintHash = hashString(voiceSample)
            val hashSimilarity = calculateStringSimilarity(currentVoicePrintHash, storedVoicePrint)
            
            // Compare voice features
            val currentFeatures = extractVoiceFeatures(voiceSample)
            val storedFeaturesList = storedFeatures.split(",").map { it.toFloat() }
            val featureSimilarity = calculateFeatureSimilarity(currentFeatures, storedFeaturesList)
            
            // Combined similarity score
            val overallSimilarity = (hashSimilarity + featureSimilarity) / 2
            
            Log.d(TAG, "Voice authentication - Hash similarity: $hashSimilarity, Feature similarity: $featureSimilarity, Overall: $overallSimilarity")
            
            overallSimilarity >= SIMILARITY_THRESHOLD
        } catch (e: Exception) {
            Log.e(TAG, "Error during voice authentication", e)
            false
        }
    }
    
    /**
     * Check if voice print is already stored
     */
    fun isVoicePrintStored(): Boolean {
        return secureStorage.getData(VOICE_PRINT_KEY) != null
    }
    
    /**
     * Extract voice features from audio sample
     * In a real implementation, this would use audio processing libraries
     * For demo purposes, we'll extract basic features
     */
    private fun extractVoiceFeatures(voiceSample: String): FloatArray {
        val features = mutableListOf<Float>()
        
        // Feature 1: Length of voice sample
        features.add(voiceSample.length.toFloat())
        
        // Feature 2: Average word length
        val words = voiceSample.split(" ").filter { it.isNotEmpty() }
        val avgWordLength = if (words.isNotEmpty()) {
            words.map { it.length }.average().toFloat()
        } else {
            0f
        }
        features.add(avgWordLength)
        
        // Feature 3: Number of words
        features.add(words.size.toFloat())
        
        // Feature 4: Character frequency patterns
        val charFreq = voiceSample.groupingBy { it.lowercaseChar() }.eachCount()
        features.add(charFreq.size.toFloat()) // Unique characters
        
        // Feature 5: Vowel to consonant ratio
        val vowels = voiceSample.count { it.lowercaseChar() in "aeiou" }
        val consonants = voiceSample.count { it.isLetter() && it.lowercaseChar() !in "aeiou" }
        val vowelRatio = if (consonants > 0) vowels.toFloat() / consonants else 0f
        features.add(vowelRatio)
        
        // Feature 6: Uppercase to lowercase ratio
        val uppercase = voiceSample.count { it.isUpperCase() }
        val lowercase = voiceSample.count { it.isLowerCase() }
        val caseRatio = if (lowercase > 0) uppercase.toFloat() / lowercase else 0f
        features.add(caseRatio)
        
        return features.toFloatArray()
    }
    
    /**
     * Calculate similarity between two feature arrays
     */
    private fun calculateFeatureSimilarity(features1: FloatArray, features2: List<Float>): Float {
        if (features1.size != features2.size) {
            Log.w(TAG, "Feature size mismatch: ${features1.size} vs ${features2.size}")
            return 0f
        }
        
        var totalDifference = 0f
        var maxFeature = 0f
        
        for (i in features1.indices) {
            totalDifference += abs(features1[i] - features2[i])
            maxFeature = maxOf(maxFeature, maxOf(abs(features1[i]), abs(features2[i])))
        }
        
        // Normalize and convert to similarity (1 - normalized difference)
        return if (maxFeature > 0) {
            1f - (totalDifference / (maxFeature * features1.size))
        } else {
            1f
        }
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateStringSimilarity(str1: String, str2: String): Float {
        val maxLength = maxOf(str1.length, str2.length)
        if (maxLength == 0) return 1f
        
        val distance = levenshteinDistance(str1, str2)
        return 1f - (distance.toFloat() / maxLength)
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val matrix = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) {
            matrix[i][0] = i
        }
        
        for (j in 0..str2.length) {
            matrix[0][j] = j
        }
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return matrix[str1.length][str2.length]
    }
    
    /**
     * Hash string for secure storage
     */
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Clear stored voice print
     */
    fun clearVoicePrint() {
        secureStorage.removeData(VOICE_PRINT_KEY)
        secureStorage.removeData(VOICE_FEATURES_KEY)
        Log.d(TAG, "Voice print cleared")
    }
}
