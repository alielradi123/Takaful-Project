package com.example.takaful.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
) {
    val timeFormatted: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "الآن"
                diff < 3_600_000 -> "منذ ${diff / 60_000} د"
                diff < 86_400_000 -> {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
                else -> {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
}

data class ChatThread(
    val id: String = "",
    val participantName: String = "الدعم والمحادثات",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0
) {
    val timeFormatted: String
        get() = ChatMessage(timestamp = lastMessageTime).timeFormatted
}

class ChatsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chatThreads = MutableStateFlow<List<ChatThread>>(emptyList())
    val chatThreads = _chatThreads.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private var threadsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    /**
     * Starts listening for chat threads for the current user.
     */
    fun startListening() {
        startListeningWithCallback(onFirstLoad = null)
    }

    /**
     * Starts listening for chat threads and calls [onFirstLoad] after first snapshot is received.
     */
    fun startListeningWithCallback(userName: String = "مستخدم", onFirstLoad: (() -> Unit)?) {
        val uid = auth.currentUser?.uid ?: run { onFirstLoad?.invoke(); return }
        if (threadsListener != null) { onFirstLoad?.invoke(); return }
        var firstLoad = true

        threadsListener = db.collection("chats")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    if (firstLoad) { firstLoad = false; onFirstLoad?.invoke() }
                    return@addSnapshotListener
                }

                if (snapshot.isEmpty) {
                    // Create a default support chat thread
                    createSupportChat(uid, userName)
                } else {
                    // Sort all docs by lastMessageTime descending - keep the latest
                    val sorted = snapshot.documents.sortedByDescending { doc ->
                        doc.getTimestamp("lastMessageTime")?.toDate()?.time
                            ?: doc.getLong("lastMessageTime")
                            ?: 0L
                    }

                    // Keep only the latest chat, delete duplicates silently
                    if (sorted.size > 1) {
                        sorted.drop(1).forEach { oldDoc ->
                            // Delete messages sub-collection first then the doc
                            db.collection("chats").document(oldDoc.id)
                                .collection("messages").get()
                                .addOnSuccessListener { msgs ->
                                    val batch = db.batch()
                                    msgs.documents.forEach { batch.delete(it.reference) }
                                    batch.delete(db.collection("chats").document(oldDoc.id))
                                    batch.commit()
                                }
                        }
                    }

                    val latest = sorted.first()
                    val time = latest.getTimestamp("lastMessageTime")?.toDate()?.time
                        ?: latest.getLong("lastMessageTime")
                        ?: 0L

                    _chatThreads.value = listOf(
                        ChatThread(
                            id = latest.id,
                            participantName = "الدعم والمحادثات",
                            lastMessage = latest.getString("lastMessage") ?: "",
                            lastMessageTime = time,
                            unreadCount = latest.getLong("unreadCount_$uid")?.toInt() ?: 0
                        )
                    )
                }

                if (firstLoad) { firstLoad = false; onFirstLoad?.invoke() }
            }
    }

    /**
     * Listens for messages in a specific chat.
     */
    fun listenToMessages(chatId: String) {
        messagesListener?.remove()

        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                _messages.value = snapshot.documents.mapNotNull { doc ->
                    val time = doc.getTimestamp("createdAt")?.toDate()?.time 
                        ?: doc.getLong("timestamp") 
                        ?: 0L
                    
                    // Web platform uses "sender" = 'user' or 'admin' 
                    // and "senderId" is not always reliable. So let's check both.
                    val senderId = doc.getString("senderId") 
                        ?: if (doc.getString("sender") == "admin") "support" else auth.currentUser?.uid ?: ""
                    val senderName = doc.getString("senderName") ?: ""
                    val senderPhotoUrl = doc.getString("senderPhotoUrl") ?: doc.getString("photoUrl") ?: ""
                        
                    ChatMessage(
                        id = doc.id,
                        senderId = senderId,
                        senderName = senderName,
                        senderPhotoUrl = senderPhotoUrl,
                        text = doc.getString("text") ?: "",
                        timestamp = time,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                }
            }
    }

    /**
     * Sends a message in a chat thread.
     */
    fun sendMessage(
        chatId: String,
        text: String,
        userName: String = "مستخدم",
        userPhotoUrl: String = "",
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return

        val message = mapOf(
            "sender" to "user",
            "senderId" to uid,
            "senderName" to userName,
            "senderPhotoUrl" to userPhotoUrl,
            "text" to text,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "timestamp" to System.currentTimeMillis(), // fallback
            "isRead" to false
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // Update thread's last message
                db.collection("chats").document(chatId).update(
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun editMessage(
        chatId: String,
        messageId: String,
        newText: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .update(
                mapOf(
                    "text" to newText,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteMessage(
        chatId: String,
        messageId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun createSupportChat(uid: String, userName: String) {
        val chatData = mapOf(
            "userId" to uid,
            "userName" to userName,
            "lastMessage" to "مرحباً بك في تكافل! كيف يمكننا مساعدتك؟",
            "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "unreadCount_$uid" to 1
        )

        db.collection("chats")
            .add(chatData)
            .addOnSuccessListener { docRef ->
                // Add welcome message
                docRef.collection("messages")
                    .add(
                        mapOf(
                            "sender" to "admin",
                            "senderId" to "support",
                            "senderName" to "الدعم والمحادثات",
                            "text" to "مرحباً بك في تكافل! كيف يمكننا مساعدتك؟",
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "isRead" to false
                        )
                    )
            }
    }

    fun stopListening() {
        threadsListener?.remove()
        threadsListener = null
        messagesListener?.remove()
        messagesListener = null
    }
}
