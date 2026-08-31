package com.example.takaful.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("takaful_prefs", Context.MODE_PRIVATE)

    var isFirstTimeLaunch: Boolean
        get() = prefs.getBoolean("isFirstTimeLaunch", true)
        set(value) = prefs.edit().putBoolean("isFirstTimeLaunch", value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("isBiometricEnabled", false)
        set(value) = prefs.edit().putBoolean("isBiometricEnabled", value).apply()

    var profileImagePath: String?
        get() = prefs.getString("profileImagePath", null)
        set(value) = prefs.edit().putString("profileImagePath", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("isDarkMode", false)
        set(value) = prefs.edit().putBoolean("isDarkMode", value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean("isNotificationsEnabled", true)
        set(value) = prefs.edit().putBoolean("isNotificationsEnabled", value).apply()

    fun saveCredentials(email: String, pass: String) {
        val encryptedEmail = android.util.Base64.encodeToString(email.toByteArray(), android.util.Base64.DEFAULT)
        val encryptedPass = android.util.Base64.encodeToString(pass.toByteArray(), android.util.Base64.DEFAULT)
        prefs.edit()
            .putString("savedEmail", encryptedEmail)
            .putString("savedPassword", encryptedPass)
            .apply()
    }

    fun getSavedCredentials(): Pair<String, String>? {
        val emailEnc = prefs.getString("savedEmail", null)
        val passEnc = prefs.getString("savedPassword", null)
        if (emailEnc != null && passEnc != null) {
            val email = String(android.util.Base64.decode(emailEnc, android.util.Base64.DEFAULT))
            val pass = String(android.util.Base64.decode(passEnc, android.util.Base64.DEFAULT))
            return Pair(email, pass)
        }
        return null
    }

    fun clearCredentials() {
        prefs.edit().remove("savedEmail").remove("savedPassword").apply()
    }
}
