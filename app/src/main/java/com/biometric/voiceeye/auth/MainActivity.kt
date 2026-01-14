package com.biometric.voiceeye.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.biometric.voiceeye.auth.databinding.ActivityMainBinding
import com.biometric.voiceeye.auth.service.AuthenticationService
import com.biometric.voiceeye.auth.ui.setup.VoiceSetupActivity
import com.biometric.voiceeye.auth.ui.setup.EyeSetupActivity
import com.biometric.voiceeye.auth.utils.BiometricManager
import com.biometric.voiceeye.auth.utils.PermissionManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricManager: BiometricManager
    private lateinit var permissionManager: PermissionManager
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        checkAndRequestPermissions()
        setupClickListeners()
    }
    
    private fun initializeComponents() {
        biometricManager = BiometricManager(this)
        permissionManager = PermissionManager(this)
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.USE_BIOMETRIC
        )
        
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            onPermissionsGranted()
        }
    }
    
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            Toast.makeText(this, "सभी permissions ज़रूरी हैं", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun onPermissionsGranted() {
        checkBiometricSetup()
    }
    
    private fun checkBiometricSetup() {
        when {
            !biometricManager.isVoiceSetup() -> {
                startActivity(Intent(this, VoiceSetupActivity::class.java))
            }
            !biometricManager.isEyeSetup() -> {
                startActivity(Intent(this, EyeSetupActivity::class.java))
            }
            else -> {
                setupAuthenticationUI()
            }
        }
    }
    
    private fun setupAuthenticationUI() {
        binding.tvStatus.text = "Voice + Eye Authentication Ready"
        binding.btnStartAuth.isEnabled = true
    }
    
    private fun setupClickListeners() {
        binding.btnStartAuth.setOnClickListener {
            startAuthentication()
        }
        
        binding.btnSetupVoice.setOnClickListener {
            startActivity(Intent(this, VoiceSetupActivity::class.java))
        }
        
        binding.btnSetupEye.setOnClickListener {
            startActivity(Intent(this, EyeSetupActivity::class.java))
        }
    }
    
    private fun startAuthentication() {
        val intent = Intent(this, AuthenticationService::class.java).apply {
            action = AuthenticationService.ACTION_START_AUTH
        }
        startService(intent)
        
        binding.tvStatus.text = "Authentication in progress..."
        binding.btnStartAuth.isEnabled = false
    }
    
    override fun onResume() {
        super.onResume()
        checkBiometricSetup()
    }
}
