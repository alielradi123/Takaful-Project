package com.example.takaful.data.repository

import android.util.Log
import com.example.takaful.data.model.SystemSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private val _sysSettings = MutableStateFlow(SystemSettings())
    val sysSettings: StateFlow<SystemSettings> = _sysSettings.asStateFlow()

    fun startListening() {
        if (listenerRegistration != null) return

        listenerRegistration = firestore.collection("settings").document("general")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SettingsRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val data = snapshot.data ?: emptyMap<String, Any>()
                        val settings = SystemSettings(
                            appMaintenanceMode = data["app_maintenance_mode"] as? Boolean ?: false,
                            forceAppUpdate = data["force_app_update"] as? Boolean ?: false,
                            minAppVersion = data["min_app_version"] as? String ?: "1.0.0",
                            playStoreLink = data["play_store_link"] as? String ?: "",
                            webMaintenanceMode = data["web_maintenance_mode"] as? Boolean ?: false,
                            contactEmail = data["contact_email"] as? String ?: "",
                            contactPhone = data["contact_phone"] as? String ?: "",
                            facebookUrl = data["facebook_url"] as? String ?: "",
                            twitterUrl = data["twitter_url"] as? String ?: "",
                            instagramUrl = data["instagram_url"] as? String ?: "",
                            bankName = data["bank_name"] as? String ?: "بنك الخرطوم",
                            bankAccount = data["bank_account"] as? String ?: "",
                            bankHolder = data["bank_holder"] as? String ?: "",
                            aboutUsText = data["about_us_text"] as? String ?: "",
                            privacyPolicyUrl = data["privacy_policy_url"] as? String ?: "",
                            termsUrl = data["terms_url"] as? String ?: ""
                        )
                        _sysSettings.value = settings
                    } catch (ex: Exception) {
                        Log.e("SettingsRepository", "Error parsing settings", ex)
                    }
                }
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
