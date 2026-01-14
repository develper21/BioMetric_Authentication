package com.biometric.voiceeye.auth.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.biometric.voiceeye.auth.databinding.ActivityVoiceSetupBinding
import com.biometric.voiceeye.auth.voice.VoicePatternMatcher
import com.biometric.voiceeye.auth.voice.VoiceRecognitionManager
import kotlinx.coroutines.*

class VoiceSetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVoiceSetupBinding
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager
    private lateinit var voicePatternMatcher: VoicePatternMatcher
    private var setupJob: Job? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceSetup()
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        setupClickListeners()
        checkMicrophonePermission()
    }
    
    private fun initializeComponents() {
        voiceRecognitionManager = VoiceRecognitionManager(this)
        voicePatternMatcher = VoicePatternMatcher(this)
    }
    
    private fun setupClickListeners() {
        binding.btnStartVoiceSetup.setOnClickListener {
            startVoiceSetup()
        }
        
        binding.btnRetry.setOnClickListener {
            resetSetup()
            startVoiceSetup()
        }
        
        binding.btnComplete.setOnClickListener {
            finish()
        }
    }
    
    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            updateUI("ready")
        }
    }
    
    private fun startVoiceSetup() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        setupJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                updateUI("listening")
                binding.tvInstruction.text = "कृपया अपनी voice sample दें...\n\"Hello, this is my voice\""
                
                // Capture voice sample
                val voiceSample = withContext(Dispatchers.IO) {
                    voiceRecognitionManager.captureVoicePrint()
                }
                
                if (voiceSample == null) {
                    updateUI("error")
                    binding.tvInstruction.text = "Voice capture failed. Please try again."
                    return@launch
                }
                
                updateUI("processing")
                binding.tvInstruction.text = "Processing your voice pattern..."
                
                // Store voice pattern
                val success = withContext(Dispatchers.IO) {
                    voicePatternMatcher.storeVoicePrint(voiceSample)
                }
                
                if (success) {
                    updateUI("success")
                    binding.tvInstruction.text = "Voice setup completed successfully!\n\nYour voice pattern has been securely stored."
                } else {
                    updateUI("error")
                    binding.tvInstruction.text = "Failed to store voice pattern. Please try again."
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
                binding.btnStartVoiceSetup.isEnabled = true
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.GONE
                binding.tvInstruction.text = "Tap below to start voice setup\n\nWe'll record your voice pattern for secure authentication"
            }
            "listening" -> {
                binding.btnStartVoiceSetup.isEnabled = false
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.animationView.setAnimation("voice_recording.json")
                binding.animationView.playAnimation()
            }
            "processing" -> {
                binding.btnStartVoiceSetup.isEnabled = false
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.animationView.setAnimation("processing.json")
                binding.animationView.playAnimation()
            }
            "success" -> {
                binding.btnStartVoiceSetup.isEnabled = false
                binding.btnRetry.isEnabled = false
                binding.btnComplete.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
                binding.animationView.setAnimation("success.json")
                binding.animationView.playAnimation()
            }
            "error" -> {
                binding.btnStartVoiceSetup.isEnabled = false
                binding.btnRetry.isEnabled = true
                binding.btnComplete.isEnabled = false
                binding.progressBar.visibility = android.view.View.GONE
                binding.animationView.setAnimation("error.json")
                binding.animationView.playAnimation()
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
        voiceRecognitionManager.destroy()
    }
}
