package com.example.takaful.data.repository

import android.content.Context
import android.net.Uri
import com.example.takaful.data.model.CaseItem
import com.example.takaful.utils.SupabaseStorageHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CasesRepository {

    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("cases")

    private val _cases = MutableStateFlow<List<CaseItem>>(emptyList())
    val cases = _cases.asStateFlow()

    // Volunteer: cases assigned to current user
    private val _assignedCases = MutableStateFlow<List<CaseItem>>(emptyList())
    val assignedCases = _assignedCases.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var casesListener: ListenerRegistration? = null
    private var volunteerListener: ListenerRegistration? = null

    // ── Active cases listener (status == "active" || "approved") ──
    fun startListening() {
        if (casesListener != null) return
        casesListener = col
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { _isLoading.value = false; return@addSnapshotListener }
                if (snapshot != null) {
                    val list = snapshot.documents
                        .mapNotNull { CaseItem.fromFirestore(it) }
                        .filter { it.isActive }
                    _cases.value = list
                    _isLoading.value = false
                }
            }
    }

    // ── Volunteer: listen to cases assigned to this volunteer ──
    fun startVolunteerListener(volunteerUid: String) {
        volunteerListener?.remove()
        volunteerListener = col
            .whereEqualTo("assignedVolunteerId", volunteerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                _assignedCases.value = snapshot?.documents
                    ?.mapNotNull { CaseItem.fromFirestore(it) }
                    ?: emptyList()
            }
    }

    fun stopListening() {
        casesListener?.remove(); casesListener = null
        volunteerListener?.remove(); volunteerListener = null
    }

    // ── Add new case ──
    fun addCase(
        title: String,
        location: String,
        category: String,
        targetAmount: Double,
        description: String,
        urgencyLevel: String = "عادي",
        imageUri: Uri? = null,
        context: Context? = null,
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val docRef = col.document()

        val save: (String) -> Unit = { imageUrl ->
            val case = CaseItem(
                id            = docRef.id,
                caseId        = docRef.id,
                beneficiaryId = uid,
                createdBy     = uid,
                title         = title,
                description   = description,
                story         = description,
                location      = location,
                category      = category,
                targetAmount  = targetAmount,
                amountRequired = targetAmount,
                imageUrl      = imageUrl,
                documentUrl   = imageUrl,
                imageUrls     = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
                urgencyLevel  = urgencyLevel,
                status        = "pending",
                createdAt     = System.currentTimeMillis()
            )
            docRef.set(case.toFirestoreMap())
                .addOnSuccessListener { onSuccess(docRef.id) }
                .addOnFailureListener { e -> onFailure(e) }
        }

        if (imageUri != null && context != null) {
            SupabaseStorageHelper.uploadImage(
                context = context,
                uri = imageUri,
                path = "case_documents/${docRef.id}_${System.currentTimeMillis()}.jpg",
                onSuccess = { url -> save(url) },
                onFailure = { save("") }
            )
        } else {
            save("")
        }
    }

    // ── Approve a pending case ──
    fun approveCase(
        caseId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(caseId).update(
            mapOf("status" to "active", "updatedAt" to System.currentTimeMillis())
        ).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) }
    }

    // ── Reject a case ──
    fun rejectCase(
        caseId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(caseId).update(
            mapOf("status" to "rejected", "updatedAt" to System.currentTimeMillis())
        ).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) }
    }

    // ── Assign a volunteer to a case ──
    fun assignVolunteer(
        caseId: String,
        volunteerId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(caseId).update(
            mapOf(
                "assignedVolunteerId" to volunteerId,
                "updatedAt"           to System.currentTimeMillis()
            )
        ).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) }
    }

    // ── Volunteer submits completion report ──
    fun submitVolunteerReport(
        caseId: String,
        report: String,
        imageUri: Uri? = null,
        context: Context? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val saveReport: (String) -> Unit = { imageUrl ->
            val updateMap = mutableMapOf<String, Any>(
                "volunteerReport" to report,
                "status"          to "completed",
                "updatedAt"       to System.currentTimeMillis()
            )
            if (imageUrl.isNotEmpty()) {
                updateMap["volunteerReportImage"] = imageUrl
            }
            col.document(caseId).update(updateMap)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        }

        if (imageUri != null && context != null) {
            SupabaseStorageHelper.uploadImage(
                context = context,
                uri = imageUri,
                path = "volunteer_reports/${caseId}_${System.currentTimeMillis()}.jpg",
                onSuccess = { url -> saveReport(url) },
                onFailure = { saveReport("") }
            )
        } else {
            saveReport("")
        }
    }

    // ── Update raised amount and progress ──
    fun updateCaseProgress(
        caseId: String,
        addedAmount: Double,
        onComplete: () -> Unit = {}
    ) {
        val ref = col.document(caseId)
        ref.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val target = doc.getDouble("targetAmount") ?: doc.getDouble("amountRequired") ?: 0.0
                val currentRaised = doc.getDouble("raisedAmount") ?: doc.getDouble("amountRaised") ?: 0.0
                val newRaised = currentRaised + addedAmount
                val newProgress = if (target > 0) (newRaised / target).coerceAtMost(1.0) else 0.0
                ref.update(
                    mapOf(
                        "raisedAmount" to newRaised,
                        "amountRaised" to newRaised,   // backward compat
                        "progress"     to newProgress,
                        "updatedAt"    to System.currentTimeMillis()
                    )
                ).addOnSuccessListener { onComplete() }
            }
        }
    }

    // ── Atomic increment of raisedAmount (used when donation is distributed) ──
    fun addToRaisedAmount(
        caseId: String,
        amount: Double,
        onComplete: () -> Unit = {}
    ) {
        val ref = col.document(caseId)
        // First increment atomically
        ref.update(
            mapOf(
                "raisedAmount" to com.google.firebase.firestore.FieldValue.increment(amount),
                "amountRaised" to com.google.firebase.firestore.FieldValue.increment(amount),
                "updatedAt"    to System.currentTimeMillis()
            )
        ).addOnSuccessListener {
            // Then recalculate progress from fresh values
            ref.get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val target = doc.getDouble("targetAmount") ?: doc.getDouble("amountRequired") ?: 0.0
                    val raised = doc.getDouble("raisedAmount") ?: 0.0
                    val prog   = if (target > 0) (raised / target).coerceAtMost(1.0) else 0.0
                    ref.update("progress", prog).addOnSuccessListener { onComplete() }
                }
            }
        }
    }

    // ── Lookup ──
    fun getCaseById(id: String): CaseItem? = _cases.value.find { it.id == id }
        ?: _assignedCases.value.find { it.id == id }

    /** Legacy Int support */
    fun getCaseById(id: Int): CaseItem? = _cases.value.find {
        it.id == "case_$id" || it.id == id.toString()
    }
}
