package com.example.takaful.data.repository

import com.example.takaful.data.model.NotificationRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("notifications")
    
    private val _notifications = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val notifications = _notifications.asStateFlow()
    
    private var listener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    fun startListening() {
        if (authListener != null) return
        val auth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            listener?.remove()
            listener = null
            
            val user = firebaseAuth.currentUser
            if (user != null) {
                listener = col.whereEqualTo("userId", user.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snap, err ->
                        if (err != null) return@addSnapshotListener
                        val list = snap?.documents?.mapNotNull { NotificationRecord.fromFirestore(it) } ?: emptyList()
                        _notifications.value = list
                    }
            } else {
                _notifications.value = emptyList()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    fun stopListening() {
        listener?.remove()
        listener = null
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        authListener = null
    }

    fun markAsRead(notificationId: String) {
        col.document(notificationId).update("isRead", true)
    }
    
    fun markAllAsRead() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        db.runBatch { batch ->
            _notifications.value.filter { !it.isRead }.forEach {
                val docRef = col.document(it.id)
                batch.update(docRef, "isRead", true)
            }
        }
    }

    fun addNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val newDoc = col.document()
        val notif = hashMapOf(
            "id" to newDoc.id,
            "userId" to userId,
            "title" to title,
            "message" to message,
            "type" to type,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        newDoc.set(notif)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
