package com.example.takaful.data.repository

import com.example.takaful.data.model.ShareRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharesRepository {

    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("shares")

    private val _shares = MutableStateFlow<List<ShareRecord>>(emptyList())
    val shares = _shares.asStateFlow()

    private var listener: ListenerRegistration? = null

    // ── Listen to all shares for current user ──
    fun startListening() {
        if (listener != null) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        listener = col
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                _shares.value = snap?.documents?.mapNotNull { ShareRecord.fromFirestore(it) } ?: emptyList()
            }
    }

    fun stopListening() {
        listener?.remove(); listener = null
    }

    // ── Record a new share ──
    fun recordShare(
        caseId: String,
        caseTitle: String,
        platform: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val docRef = col.document()
        val record = ShareRecord(
            id        = docRef.id,
            shareId   = docRef.id,
            caseId    = caseId,
            caseTitle = caseTitle,
            userId    = uid,
            platform  = platform,
            timestamp = System.currentTimeMillis()
        )
        docRef.set(record.toFirestoreMap())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // ── Count shares for a specific case ──
    fun getShareCountForCase(caseId: String): Int =
        _shares.value.count { it.caseId == caseId }
}
