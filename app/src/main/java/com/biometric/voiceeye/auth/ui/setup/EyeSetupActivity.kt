package com.biometric.voiceeye.auth.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.biometric.voiceeye.auth.databinding.ActivityEyeSetupBinding
import com.biometric.voiceeye.auth.eye.EyeScannerManager
import com.biometric.voiceeye.auth.eye.IrisPatternMatcher
import kotlinx.coroutines.*

class EyeSetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEyeSetupBinding
    private lateinit var eyeScannerManager: EyeScannerManager
    private lateinit var irisPatternMatcher: IrisPatternMatcher
    private var setupJob: Job? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startEyeSetup()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEyeSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        setupClickListeners()
        checkCameraPermission()
    }
    
    private fun initializeComponents() {
        eyeScannerManager = EyeScannerManager(this, this)
        irisPatternMatcher = IrisPatternMatcher(this)
    }
    
    private fun setupClickListeners() {
        binding.btnStartEyeSetup.setOnClickListener {
            startEyeSetup()
        }
        
        binding.btnRetry.setOnClickListener {
            resetSetup()
            startEyeSetup()
        }
        
        binding.btnComplete.setOnClickListener {
            finish()
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            updateUI("ready")
        }
    }
    
    private fun startEyeSetup() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        
        setupJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                updateUI("scanning")
                binding.tvInstruction.text = "अपनी eyes को camera के सामने रखें...\nऔर सीधे देखें"
                
                // Start eye scanning
                val scanResult = withContext(Dispatchers.IO) {
                    eyeScannerManager.startEyeScan()
                }
                
                when (scanResult) {
                    is EyeScannerManager.EyeScanResult.Success -> {
                        if (!scanResult.isLive) {
                            updateUI("error")
                            binding.tvInstruction.text = "Liveness detection failed. Please try again."
                            return@launch
                        }
                        
                        updateUI("capturing")
                        binding.tvInstruction.text = "Capturing iris pattern..."
                        
                        // Capture high-quality iris image
                        val irisImage = withContext(Dispatchers.IO) {
                            eyeScannerManager.captureIrisImage()
                        }
                        
                        if (irisImage == null) {
                            updateUI("error")
                            binding.tvInstruction.text = "Failed to capture iris image. Please try again."
                            return@launch
                        }
                        
                        updateUI("processing")
                        binding.tvInstruction.text = "Processing your iris pattern..."
                        
                        // Store iris pattern
                        val success = withContext(Dispatchers.IO) {
                            irisPatternMatcher.storeIrisPattern(irisImage)
                        }
                        
                        if (success) {
                            updateUI("success")
                            binding.tvInstruction.text = "Eye setup completed successfully!\n\nYour iris pattern has been securely stored."
                            
                            // Show captured image for verification
                            binding.ivPreview.setImageBitmap(irisImage)
                            binding.ivPreview.visibility = android.view.View.VISIBLE
                        } else {
                            updateUI("error")
                            binding.tvInstruction.text = "Failed to store iris pattern. Please try again."
                        }
                    }
                    is EyeScannerManager.EyeScanResult.Error -> {
                        updateUI("error")
                        binding.tvInstruction.text = "Eye scan failed: ${scanResult.message}"
                    }
                }
                
            } catch (e: Exception) {
                updateUI("error")
                binding.tvInstruction.text = "Error: ${e.message}"
            }
        }
    }
    
    private fun updateUI(state: String) {
        when (state) {
            "ready" -> {
                binding.btnStartEyeSetup.isEnabled = true
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.GONE
                binding.tvInstruction.text = "Tap below to start eye setup\n\nWe'll scan your iris pattern for secure authentication"
                binding.ivPreview.visibility = android.view.View.GONE
            }
            "scanning" -> {
                binding.btnStartEyeSetup.isEnabled = false
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.animationView.setAnimation("eye_scanning.json")
                binding.animationView.playAnimation()
                binding.ivPreview.visibility = android.view.View.GONE
            }
            "capturing" -> {
                binding.btnStartEyeSetup.isEnabled = false
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.animationView.setAnimation("camera_capture.json")
                binding.animationView.playAnimation()
            }
            "processing" -> {
                binding.btnStartEyeSetup.isEnabled = false
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.animationView.setAnimation("processing.json")
                binding.animationView.playAnimation()
            }
            "success" -> {
                binding.btnStartEyeSetup.isEnabled = false
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
                binding.animationView.setAnimation("success.json")
                binding.animationView.playAnimation()
            }
            "error" -> {
                binding.btnStartEyeSetup.isEnabled = false
                binding.btnRetry.isEnabled = true
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.GONE
                binding.animationView.setAnimation("error.json")
                binding.animationView.playAnimation()
                binding.ivPreview.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun resetSetup() {
        setupJob?.cancel()
        updateUI("ready")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        setupJob?.cancel()
        eyeScannerManager.release()
    }
}
