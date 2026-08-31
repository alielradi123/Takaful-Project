package com.example.takaful.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class NotificationRecord(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val type: String = "general", // "donation_update", "system", etc.
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,
            "message" to message,
            "isRead" to isRead,
            "type" to type,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromFirestore(doc: DocumentSnapshot): NotificationRecord? {
            if (!doc.exists()) return null
            return NotificationRecord(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                title = doc.getString("title") ?: "",
                message = doc.getString("message") ?: "",
                isRead = doc.getBoolean("isRead") ?: false,
                type = doc.getString("type") ?: "general",
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }
    }
}
