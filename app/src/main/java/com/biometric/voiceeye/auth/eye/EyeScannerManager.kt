package com.biometric.voiceeye.auth.eye

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EyeScannerManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private val TAG = "EyeScanner"
    
    companion object {
        const val SCAN_TIMEOUT = 15000L // 15 seconds
        const val REQUIRED_FACE_SIZE = 0.3f // Face should occupy at least 30% of frame
        const val BLINK_DETECTION_THRESHOLD = 0.7f
    }
    
    init {
        initializeCamera()
    }
    
    private fun initializeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            setupCamera()
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun setupCamera() {
        try {
            // Unbind any previous use cases
            cameraProvider?.unbindAll()
            
            // ImageCapture for taking photos
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            // ImageAnalysis for real-time face detection
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            // Select front camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            
            // Bind use cases to camera
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageCapture,
                imageAnalyzer
            )
            
            Log.d(TAG, "Camera setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Camera setup failed", e)
        }
    }
    
    /**
     * Start eye scanning process and return face detection results
     */
    suspend fun startEyeScan(): EyeScanResult = withTimeoutOrNull(SCAN_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            val faceDetector = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .setMinFaceSize(REQUIRED_FACE_SIZE)
                    .build()
            )
            
            imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageForFaceDetection(
                    imageProxy,
                    faceDetector,
                    continuation
                )
            }
            
            continuation.invokeOnCancellation {
                imageAnalyzer?.clearAnalyzer()
                faceDetector.close()
            }
        }
    } ?: EyeScanResult.Error("Eye scan timeout")
    
    /**
     * Capture high-quality image for iris pattern analysis
     */
    suspend fun captureIrisImage(): Bitmap? = withTimeoutOrNull(5000) {
        suspendCancellableCoroutine { continuation ->
            imageCapture?.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = imageProxyToBitmap(image)
                        image.close()
                        
                        if (continuation.isActive) {
                            continuation.resume(bitmap)
                        }
                    }
                    
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Image capture error", exception)
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
                }
            )
        }
    }
    
    private fun processImageForFaceDetection(
        imageProxy: ImageProxy,
        faceDetector: com.google.mlkit.vision.face.FaceDetector,
        continuation: kotlin.coroutines.Continuation<EyeScanResult>
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0] // Take the first detected face
                        
                        // Check face quality
                        val faceQuality = assessFaceQuality(face)
                        
                        if (faceQuality.isGoodQuality) {
                            // Check for liveness (blink detection)
                            val isLive = checkLiveness(face)
                            
                            if (isLive) {
                                val result = EyeScanResult.Success(
                                    face = face,
                                    confidence = faceQuality.confidence,
                                    isLive = true,
                                    bitmap = imageProxyToBitmap(imageProxy)
                                )
                                
                                if (continuation.isActive) {
                                    continuation.resume(result)
                                }
                                imageAnalyzer?.clearAnalyzer()
                            } else {
                                Log.d(TAG, "Liveness detection failed")
                            }
                        } else {
                            Log.d(TAG, "Face quality insufficient: ${faceQuality.message}")
                        }
                    } else {
                        Log.d(TAG, "No face detected")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    if (continuation.isActive) {
                        continuation.resume(EyeScanResult.Error("Face detection failed: ${e.message}"))
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    private fun assessFaceQuality(face: Face): FaceQuality {
        val boundingBox = face.boundingBox
        val imageArea = 640 * 480 // Approximate image area
        val faceArea = boundingBox.width() * boundingBox.height()
        val faceRatio = faceArea.toFloat() / imageArea
        
        val confidence = face.headEulerAngleY // Check if face is centered
        
        return when {
            faceRatio < REQUIRED_FACE_SIZE -> FaceQuality(false, 0f, "Face too small")
            abs(confidence) > 30f -> FaceQuality(false, abs(confidence), "Face not centered")
            else -> FaceQuality(true, 1f - abs(confidence) / 30f, "Good quality")
        }
    }
    
    private fun checkLiveness(face: Face): Boolean {
        // Check for blinking (left and right eye open probability)
        val leftEyeOpen = face.leftEyeOpenProbability ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
        
        // For liveness detection, we want to detect a blink
        // In a real implementation, you'd track this over multiple frames
        return (leftEyeOpen > BLINK_DETECTION_THRESHOLD && rightEyeOpen > BLINK_DETECTION_THRESHOLD)
    }
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        // Rotate bitmap if needed
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Release camera resources
     */
    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        Log.d(TAG, "Camera resources released")
    }
    
    sealed class EyeScanResult {
        data class Success(
            val face: Face,
            val confidence: Float,
            val isLive: Boolean,
            val bitmap: Bitmap
        ) : EyeScanResult()
        
        data class Error(val message: String) : EyeScanResult()
    }
    
    data class FaceQuality(
        val isGoodQuality: Boolean,
        val confidence: Float,
        val message: String
    )
}
