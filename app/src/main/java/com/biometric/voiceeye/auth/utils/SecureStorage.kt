package com.biometric.voiceeye.auth.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

class SecureStorage(context: Context) {
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "biometric_secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Store data securely
     */
    fun storeData(key: String, value: String) {
        sharedPreferences.edit()
            .putString(key, value)
            .apply()
    }
    
    /**
     * Retrieve stored data
     */
    fun getData(key: String): String? {
        return sharedPreferences.getString(key, null)
    }
    
    /**
     * Remove stored data
     */
    fun removeData(key: String) {
        sharedPreferences.edit()
            .remove(key)
            .apply()
    }
    
    /**
     * Clear all stored data
     */
    fun clearAll() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }
    
    /**
     * Check if key exists
     */
    fun containsKey(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
}
