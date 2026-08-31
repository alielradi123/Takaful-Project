package com.example.takaful.data.model

import com.google.firebase.Timestamp

/**
 * Represents a user account in Takaful.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoURL: String = "",
    val role: String = "member",
    val paymentMethod: String = "",
    val isAvailable: Boolean = true,
    val status: String = "active",
    val createdAt: Timestamp? = null
) {
    /** Effective display name */
    val displayName: String get() = name.ifBlank { "مستخدم تكافل" }

    /** Checks — supports new keys (donor/beneficiary/volunteer) + legacy keys (member/employee) */
    val isVolunteer: Boolean    get() = role == "volunteer" || role == "employee"
    val isDonor: Boolean        get() = role == "donor"    || role == "member"
    val isBeneficiary: Boolean  get() = role == "beneficiary"
    val isAdmin: Boolean        get() = role == "admin"
    val isActive: Boolean       get() = true

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid"               to uid,
        "name"              to name,
        "email"             to email,
        "phone"             to phone,
        "role"              to role,
        "photoURL"          to photoURL,
        "paymentMethod"     to paymentMethod,
        "isAvailable"       to isAvailable,
        "status"            to status,
        "createdAt"         to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromFirestore(doc: com.google.firebase.firestore.DocumentSnapshot, uid: String): UserProfile {
            return UserProfile(
                uid           = uid,
                name          = doc.getString("name") ?: "",
                phone         = doc.getString("phone") ?: "",
                email         = doc.getString("email") ?: "",
                role          = doc.getString("role") ?: "member",
                photoURL      = doc.getString("photoURL") ?: "",
                paymentMethod = doc.getString("paymentMethod") ?: "",
                isAvailable   = doc.getBoolean("isAvailable") ?: true,
                status        = doc.getString("status") ?: "active",
                createdAt     = doc.getTimestamp("createdAt")
            )
        }
    }
}
