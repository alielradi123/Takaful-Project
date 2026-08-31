package com.example.takaful.data.repository

import com.example.takaful.data.model.DonationRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DonationsRepository {

    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("donations")

    private val _donations = MutableStateFlow<List<DonationRecord>>(emptyList())
    val donations = _donations.asStateFlow()

    /** Recurring donations filtered from the main list */
    val recurringDonations get() = _donations.value.filter {
        it.isRecurring && it.recurringStatus in listOf("active", "paused")
    }

    private var donorListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    // ── Real-time listener (current user's donations - handles both donorId and userId) ──
    fun startListening() {
        if (authListener != null) return
        val auth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            donorListener?.remove(); donorListener = null
            userListener?.remove(); userListener = null

            val user = firebaseAuth.currentUser
            if (user != null) {
                val donorMap = mutableMapOf<String, DonationRecord>()
                val userMap = mutableMapOf<String, DonationRecord>()

                val updateCombinedList = {
                    val combined = (donorMap.values + userMap.values).distinctBy { it.id }
                    _donations.value = combined.sortedByDescending { it.effectiveTimestamp }
                }

                donorListener = col.whereEqualTo("donorId", user.uid)
                    .addSnapshotListener { snap, err ->
                        if (err != null) return@addSnapshotListener
                        donorMap.clear()
                        snap?.documents?.mapNotNull { DonationRecord.fromFirestore(it) }?.forEach {
                            donorMap[it.id] = it
                        }
                        updateCombinedList()
                    }

                userListener = col.whereEqualTo("userId", user.uid)
                    .addSnapshotListener { snap, err ->
                        if (err != null) return@addSnapshotListener
                        userMap.clear()
                        snap?.documents?.mapNotNull { DonationRecord.fromFirestore(it) }?.forEach {
                            userMap[it.id] = it
                        }
                        updateCombinedList()
                    }
            } else {
                _donations.value = emptyList()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    fun stopListening() {
        donorListener?.remove(); donorListener = null
        userListener?.remove(); userListener = null
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        authListener = null
    }

    // ── Update Donation Status with appropriate timestamps ──
    fun updateDonationStatus(
        donationId: String,
        status: String,
        caseId: String = "",
        amount: Double = 0.0,
        casesRepository: CasesRepository? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val data = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (status == "تم الاستلام") {
            data["receivedAt"] = FieldValue.serverTimestamp()
        } else if (status == "تم التوزيع") {
            data["distributedAt"] = FieldValue.serverTimestamp()
        }

        col.document(donationId).update(data)
            .addOnSuccessListener {
                if (status == "تم التوزيع" && caseId.isNotBlank() && amount > 0 && casesRepository != null) {
                    casesRepository.addToRaisedAmount(caseId, amount) {
                        onSuccess()
                    }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ── Add new donation (new schema with BBAN/QR support) ──
    fun addDonation(
        caseId: String,
        caseTitle: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        isRecurring: Boolean = false,
        recurringInterval: String = "none",
        receiptUrl: String = "",
        donorMessage: String = "",
        notes: String = "",
        bbanAccountNumber: String = "",
        qrTransactionRef: String = "",
        bankName: String = "",
        paymentRef: String = "",
        paymentGatewayName: String = "",
        ocrText: String = "",
        aiSuspicionFlag: Boolean = false,
        casesRepository: CasesRepository,
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ): String {

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onFailure(Exception("يجب تسجيل الدخول لإتمام التبرع"))
            return ""
        }
        val docRef = col.document()
        val ts     = System.currentTimeMillis()
        val date   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Calculate next recurring date
        val nextDate = if (isRecurring) calculateNextRecurringDate(recurringInterval) else 0L

        val record = DonationRecord(

            id                = docRef.id,
            donationId        = docRef.id,
            caseId            = caseId,
            caseTitle         = caseTitle,
            donorId           = user.uid,
            userId            = user.uid,
            donorName         = user.displayName ?: "",
            amount            = amount,
            amountOrItem      = if (category == "عيني") notes.ifBlank { "$amount" }
                                else "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount)} ج.س",
            paymentMethod     = paymentMethod,
            category          = category,
            receiptUrl        = receiptUrl,
            paymentRef        = paymentRef,
            paymentGatewayName = paymentGatewayName,
            paymentStatus     = "pending",
            receiptVerifiedBy = "",
            paymentVerifiedAt = null,
            paymentRejectedAt = null,
            donorMessage      = donorMessage,
            ocrText           = ocrText,
            aiSuspicionFlag   = aiSuspicionFlag,
            bbanAccountNumber = bbanAccountNumber,

            qrTransactionRef  = qrTransactionRef,
            bankName          = bankName,
            isRecurring       = isRecurring,
            recurringInterval = if (isRecurring) recurringInterval else "none",
            recurringNextDate = nextDate,
            recurringStatus   = if (isRecurring) "active" else "none",
            status            = if (category == "عيني") "قيد الجمع" else "مكتمل",
            timestamp         = ts,
            createdAt         = ts,
            date              = date
        )

        val firestoreData = record.toFirestoreMap().toMutableMap()
        firestoreData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
        firestoreData["timestamp"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
        firestoreData["transactionId"] = paymentRef.ifBlank { qrTransactionRef }

        docRef.set(firestoreData)
            .addOnSuccessListener {
                if (caseId.isNotBlank() && category != "عيني") {
                    casesRepository.updateCaseProgress(caseId, amount)
                }
                onSuccess(docRef.id)
            }
            .addOnFailureListener { onFailure(it) }

        return docRef.id
    }

    // ── Legacy Int-based addDonation (backward compat with DonationFormScreen) ──
    fun addDonation(
        caseId: Int?,
        caseTitle: String,
        category: String,
        amountOrItem: String,
        paymentMethod: String,
        casesRepository: CasesRepository,
        onSuccess: (Int) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ): Int {
        val parsedAmount = amountOrItem
            .replace(" ر.ي", "").replace(" ر.س", "").replace(" ج.س", "").replace(",", "")
            .toDoubleOrNull() ?: 0.0
        val caseIdStr = caseId?.toString() ?: ""
        var legacyId  = 0

        addDonation(
            caseId            = caseIdStr,
            caseTitle         = caseTitle,
            amount            = parsedAmount,
            category          = category,
            paymentMethod     = paymentMethod,
            casesRepository   = casesRepository,
            onSuccess         = { docId ->
                legacyId = docId.hashCode().let { if (it < 0) -it else it }
                onSuccess(legacyId)
            },
            onFailure         = onFailure
        )
        return legacyId
    }

    // ── Update any fields on a donation ──
    fun updateDonation(
        donationId: String,
        fields: Map<String, Any?>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val data = fields.toMutableMap()
        data["updatedAt"] = FieldValue.serverTimestamp()
        col.document(donationId).update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // ── Confirm Received (تأكيد الاستلام) ──
    fun confirmReceived(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(donationId)
            .update(mapOf(
                "status"            to "تم الاستلام",
                "paymentStatus"    to "verified",
                "receivedAt"       to FieldValue.serverTimestamp(),
                "paymentVerifiedAt" to FieldValue.serverTimestamp(),
                "updatedAt"         to FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun setPaymentStatusVerified(
        donationId: String,
        receiptVerifiedBy: String,
        paymentRef: String = "",
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val data = mutableMapOf<String, Any?>(
            "paymentStatus" to "verified",
            "receiptVerifiedBy" to receiptVerifiedBy,
            "paymentVerifiedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (paymentRef.isNotBlank()) data["paymentRef"] = paymentRef
        col.document(donationId)
            .update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun setPaymentStatusRejected(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(donationId)
            .update(mapOf(
                "paymentStatus" to "rejected",
                "paymentRejectedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


    // ── Confirm Distributed (تأكيد التوزيع) — also updates case raisedAmount ──
    fun confirmDistributed(
        donationId: String,
        caseId: String,
        amount: Double,
        casesRepository: CasesRepository,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(donationId)
            .update(mapOf(
                "status"        to "تم التوزيع",
                "distributedAt" to FieldValue.serverTimestamp(),
                "updatedAt"     to FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener {
                // Update case raisedAmount when distributing
                if (caseId.isNotBlank() && amount > 0) {
                    casesRepository.addToRaisedAmount(caseId, amount)
                }
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    // ── Delete a donation ──
    fun deleteDonation(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(donationId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // ── Recurring Donation Management ──

    /** Pause a recurring donation */
    fun pauseRecurringDonation(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(donationId)
            .update(mapOf(
                "recurringStatus" to "paused",
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Resume a paused recurring donation */
    fun resumeRecurringDonation(
        donationId: String,
        recurringInterval: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val nextDate = calculateNextRecurringDate(recurringInterval)
        col.document(donationId)
            .update(mapOf(
                "recurringStatus" to "active",
                "recurringNextDate" to nextDate,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Cancel a recurring donation permanently */
    fun cancelRecurringDonation(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        col.document(donationId)
            .update(mapOf(
                "recurringStatus" to "cancelled",
                "isRecurring" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Calculate the next recurring date based on interval */
    private fun calculateNextRecurringDate(interval: String): Long {
        val cal = Calendar.getInstance()
        when (interval) {
            "daily"   -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "weekly"  -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    // ── Lookup ──
    fun getDonationById(id: String): DonationRecord? = _donations.value.find { it.id == id }
    fun getDonationById(id: Int): DonationRecord?    = _donations.value.find {
        it.id == id.toString() || it.id.hashCode() == id
    }

    // ── Statistics ──
    fun calculateTotalDonations(): String {
        val total = _donations.value
            .filter { it.category != "عيني" }
            .sumOf { it.effectiveAmount }
        return "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(total)} ج.س"
    }

    fun calculateCasesHelped(): Int = _donations.value
        .mapNotNull { it.effectiveCaseId.takeIf { s -> s.isNotBlank() } }
        .distinct().size

    fun getMonthlyTotals(): Map<String, Double> {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return _donations.value
            .filter { it.category != "عيني" }
            .groupBy { fmt.format(Date(it.effectiveTimestamp)) }
            .mapValues { (_, list) -> list.sumOf { it.effectiveAmount } }
    }
}
