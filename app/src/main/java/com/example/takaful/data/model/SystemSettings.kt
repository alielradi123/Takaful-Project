package com.example.takaful.data.model

data class SystemSettings(
    val appMaintenanceMode: Boolean = false,
    val forceAppUpdate: Boolean = false,
    val minAppVersion: String = "1.0.0",
    val playStoreLink: String = "",
    
    val webMaintenanceMode: Boolean = false,
    val contactEmail: String = "",
    val contactPhone: String = "",
    
    val facebookUrl: String = "",
    val twitterUrl: String = "",
    val instagramUrl: String = "",

    val bankName: String = "بنك الخرطوم",
    val bankAccount: String = "",
    val bankHolder: String = "",

    val aboutUsText: String = "",
    val privacyPolicyUrl: String = "",
    val termsUrl: String = ""
)
