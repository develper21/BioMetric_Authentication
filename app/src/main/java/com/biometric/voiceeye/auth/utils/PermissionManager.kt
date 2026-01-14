package com.biometric.voiceeye.auth.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.USE_BIOMETRIC
        )
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get missing permissions
     */
    fun getMissingPermissions(): Array<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    /**
     * Check if specific permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request permissions (call from Activity)
     */
    fun requestPermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions,
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * Get permission rationale message
     */
    fun getPermissionRationale(): String {
        return "इस app को आपकी voice और eye scan करने के लिए ये permissions चाहिए:\n\n" +
                "• माइक्रोफोन - Voice command सुनने के लिए\n" +
                "• कैमरा - Eye scan करने के लिए\n" +
                "• बायोमेट्रिक - Secure authentication के लिए"
    }
}
