package com.example.takaful.data.model

/**
 * Represents a campaign share action in Takaful.
 * Stored in the "shares" Firestore collection.
 */
data class ShareRecord(
    val id: String = "",                    // Firestore document ID
    val shareId: String = "",               // duplicate of id
    val caseId: String = "",               // the shared case
    val caseTitle: String = "",            // cached title for display
    val userId: String = "",               // who shared
    val platform: String = "",             // "WhatsApp" | "Twitter/X" | "Facebook" | "تيليغرام" | "نسخ الرابط"
    val timestamp: Long = 0L
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "shareId"    to id,
        "caseId"     to caseId,
        "caseTitle"  to caseTitle,
        "userId"     to userId,
        "platform"   to platform,
        "timestamp"  to timestamp
    )

    companion object {
        fun fromFirestore(doc: com.google.firebase.firestore.DocumentSnapshot): ShareRecord? {
            if (!doc.exists()) return null
            return ShareRecord(
                id         = doc.id,
                shareId    = doc.getString("shareId") ?: doc.id,
                caseId     = doc.getString("caseId") ?: "",
                caseTitle  = doc.getString("caseTitle") ?: "",
                userId     = doc.getString("userId") ?: "",
                platform   = doc.getString("platform") ?: "",
                timestamp  = doc.getLong("timestamp") ?: 0L
            )
        }
    }
}
