package com.biometric.voiceeye.auth.eye

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.biometric.voiceeye.auth.utils.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.sqrt

class IrisPatternMatcher(private val context: Context) {
    
    private val secureStorage = SecureStorage(context)
    private val TAG = "IrisPatternMatcher"
    
    companion object {
        private const val IRIS_PATTERN_KEY = "user_iris_pattern"
        private const val IRIS_FEATURES_KEY = "user_iris_features"
        private const val IRIS_SIMILARITY_THRESHOLD = 0.80f // 80% similarity required
    }
    
    /**
     * Store iris pattern from captured eye image
     */
    suspend fun storeIrisPattern(eyeImage: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val irisFeatures = extractIrisFeatures(eyeImage)
            val irisPatternHash = hashBitmap(eyeImage)
            
            // Store both hash and features
            secureStorage.storeData(IRIS_PATTERN_KEY, irisPatternHash)
            secureStorage.storeData(IRIS_FEATURES_KEY, irisFeatures.joinToString(","))
            
            Log.d(TAG, "Iris pattern stored successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error storing iris pattern", e)
            false
        }
    }
    
    /**
     * Authenticate user by comparing current iris scan with stored pattern
     */
    suspend fun authenticateIris(eyeImage: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val storedIrisPattern = secureStorage.getData(IRIS_PATTERN_KEY)
            val storedFeatures = secureStorage.getData(IRIS_FEATURES_KEY)
            
            if (storedIrisPattern == null || storedFeatures == null) {
                Log.w(TAG, "No iris pattern found for authentication")
                return@withContext false
            }
            
            // Compare iris patterns
            val currentIrisHash = hashBitmap(eyeImage)
            val hashSimilarity = calculateHashSimilarity(currentIrisHash, storedIrisPattern)
            
            // Compare iris features
            val currentFeatures = extractIrisFeatures(eyeImage)
            val storedFeaturesList = storedFeatures.split(",").map { it.toFloat() }
            val featureSimilarity = calculateFeatureSimilarity(currentFeatures, storedFeaturesList)
            
            // Combined similarity score
            val overallSimilarity = (hashSimilarity + featureSimilarity) / 2
            
            Log.d(TAG, "Iris authentication - Hash similarity: $hashSimilarity, Feature similarity: $featureSimilarity, Overall: $overallSimilarity")
            
            overallSimilarity >= IRIS_SIMILARITY_THRESHOLD
        } catch (e: Exception) {
            Log.e(TAG, "Error during iris authentication", e)
            false
        }
    }
    
    /**
     * Check if iris pattern is already stored
     */
    fun isIrisPatternStored(): Boolean {
        return secureStorage.getData(IRIS_PATTERN_KEY) != null
    }
    
    /**
     * Extract iris features from eye image
     * In a real implementation, this would use advanced image processing
     * For demo purposes, we'll extract basic visual features
     */
    private fun extractIrisFeatures(eyeImage: Bitmap): FloatArray {
        val features = mutableListOf<Float>()
        
        // Resize image for consistent processing
        val resizedImage = Bitmap.createScaledBitmap(eyeImage, 100, 100, false)
        
        // Feature 1: Average brightness
        val avgBrightness = calculateAverageBrightness(resizedImage)
        features.add(avgBrightness)
        
        // Feature 2: Contrast (standard deviation of pixel values)
        val contrast = calculateContrast(resizedImage)
        features.add(contrast)
        
        // Feature 3: Color distribution (RGB averages)
        val colorFeatures = calculateColorDistribution(resizedImage)
        features.addAll(colorFeatures)
        
        // Feature 4: Texture complexity (edge density)
        val textureComplexity = calculateTextureComplexity(resizedImage)
        features.add(textureComplexity)
        
        // Feature 5: Symmetry measure
        val symmetry = calculateSymmetry(resizedImage)
        features.add(symmetry)
        
        // Feature 6: Radial patterns (circular patterns typical in iris)
        val radialPattern = calculateRadialPatterns(resizedImage)
        features.add(radialPattern)
        
        return features.toFloatArray()
    }
    
    private fun calculateAverageBrightness(bitmap: Bitmap): Float {
        var totalBrightness = 0L
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalBrightness += (r + g + b) / 3
        }
        
        return totalBrightness.toFloat() / pixels.size
    }
    
    private fun calculateContrast(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val brightnessValues = mutableListOf<Int>()
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            brightnessValues.add((r + g + b) / 3)
        }
        
        val mean = brightnessValues.average()
        val variance = brightnessValues.map { (it - mean).pow(2).toDouble() }.average()
        
        return sqrt(variance).toFloat()
    }
    
    private fun calculateColorDistribution(bitmap: Bitmap): List<Float> {
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixel in pixels) {
            totalR += (pixel shr 16) and 0xFF
            totalG += (pixel shr 8) and 0xFF
            totalB += pixel and 0xFF
        }
        
        val pixelCount = pixels.size
        return listOf(
            totalR.toFloat() / pixelCount,
            totalG.toFloat() / pixelCount,
            totalB.toFloat() / pixelCount
        )
    }
    
    private fun calculateTextureComplexity(bitmap: Bitmap): Float {
        var edgeCount = 0
        val width = bitmap.width
        val height = bitmap.height
        
        for (y in 0 until height - 1) {
            for (x in 0 until width - 1) {
                val currentPixel = bitmap.getPixel(x, y)
                val rightPixel = bitmap.getPixel(x + 1, y)
                val bottomPixel = bitmap.getPixel(x, y + 1)
                
                val currentGray = toGray(currentPixel)
                val rightGray = toGray(rightPixel)
                val bottomGray = toGray(bottomPixel)
                
                if (abs(currentGray - rightGray) > 30 || abs(currentGray - bottomGray) > 30) {
                    edgeCount++
                }
            }
        }
        
        return edgeCount.toFloat() / ((width - 1) * (height - 1))
    }
    
    private fun calculateSymmetry(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        var symmetryScore = 0f
        var totalPixels = 0
        
        for (y in 0 until height) {
            for (x in 0 until width / 2) {
                val leftPixel = bitmap.getPixel(x, y)
                val rightPixel = bitmap.getPixel(width - 1 - x, y)
                
                val leftGray = toGray(leftPixel)
                val rightGray = toGray(rightPixel)
                
                if (abs(leftGray - rightGray) < 20) {
                    symmetryScore++
                }
                totalPixels++
            }
        }
        
        return if (totalPixels > 0) symmetryScore / totalPixels else 0f
    }
    
    private fun calculateRadialPatterns(bitmap: Bitmap): Float {
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        var radialVariance = 0f
        var sampleCount = 0
        
        // Sample pixels at different angles from center
        for (angle in 0 until 360 step 30) {
            for (radius in 10 until minOf(centerX, centerY) step 10) {
                val x = (centerX + radius * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toInt()
                val y = (centerY + radius * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toInt()
                
                if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val gray = toGray(pixel)
                    radialVariance += gray
                    sampleCount++
                }
            }
        }
        
        return if (sampleCount > 0) radialVariance / sampleCount else 0f
    }
    
    private fun toGray(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3
    }
    
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
        
        return if (maxFeature > 0) {
            1f - (totalDifference / (maxFeature * features1.size))
        } else {
            1f
        }
    }
    
    private fun calculateHashSimilarity(hash1: String, hash2: String): Float {
        if (hash1.length != hash2.length) return 0f
        
        var matchingChars = 0
        for (i in hash1.indices) {
            if (hash1[i] == hash2[i]) {
                matchingChars++
            }
        }
        
        return matchingChars.toFloat() / hash1.length
    }
    
    private fun hashBitmap(bitmap: Bitmap): String {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 32, 32, false)
        val pixels = IntArray(resizedBitmap.width * resizedBitmap.height)
        resizedBitmap.getPixels(pixels, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)
        
        val pixelString = pixels.joinToString(",")
        return hashString(pixelString)
    }
    
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Clear stored iris pattern
     */
    fun clearIrisPattern() {
        secureStorage.removeData(IRIS_PATTERN_KEY)
        secureStorage.removeData(IRIS_FEATURES_KEY)
        Log.d(TAG, "Iris pattern cleared")
    }
}
