package com.example.takaful.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Represents a donation in Takaful.
 * Supports both old (amountOrItem: String) and new (amount: Double) schemas.
 * Updated to support BBAN transfers, QR payments, and recurring donations.
 */
data class DonationRecord(
    // ── Core Identity ──
    val id: String = "",                        // Firestore document ID
    val donationId: String = "",                // duplicate of id

    // ── Linkage ──
    val caseId: String = "",                    // linked case (String doc ID — new schema)
    val caseIdLegacy: Int? = null,              // old Int caseId for backward compat
    val caseTitle: String = "",
    val donorId: String = "",                   // donor UID (new name)
    val userId: String = "",                    // backward compat alias
    val donorName: String = "",

    // ── Amount ──
    val amount: Double = 0.0,                   // numeric amount (new)
    val amountOrItem: String = "",              // legacy string ("500 ج.س" or "10 بطانيات")

    // ── Payment & Category ──
    val paymentMethod: String = "",
    val category: String = "مالي",             // "مالي" | "عيني" | "طبي"

    // Receipt & verification
    val receiptUrl: String = "",                // URL to uploaded bank transfer receipt
    val paymentRef: String = "",               // payment gateway reference / transaction reference
    val paymentGatewayName: String = "",       // e.g. mada, visa, bank, qr
    val paymentStatus: String = "pending",     // pending | verified | rejected
    val receiptVerifiedBy: String = "",       // UID of admin/volunteer who verified manually
    val paymentVerifiedAt: Long? = null,
    val paymentRejectedAt: Long? = null,

    val ocrText: String = "",                   // text extracted by AI
    val aiSuspicionFlag: Boolean = false,       // true if AI flags transaction mismatch

    val donorMessage: String = "",              // optional message from donor


    // ── BBAN / QR Payment ──
    val bbanAccountNumber: String = "",         // BBAN account used for transfer (16 digits)
    val qrTransactionRef: String = "",          // QR transaction reference from scan
    val bankName: String = "",                  // Bank name (بنك الخرطوم, صح, etc.)

    // ── Recurring Donation ──
    val isRecurring: Boolean = false,
    val recurringInterval: String = "none",     // "none" | "daily" | "weekly" | "monthly"
    val recurringNextDate: Long = 0L,           // next scheduled donation date
    val recurringStatus: String = "none",       // "none" | "active" | "paused" | "cancelled"

    // ── Status ──
    val status: String = "قيد الجمع",          // "قيد الجمع" | "تم الاستلام" | "تم التوزيع"

    // ── Timestamps ──
    val timestamp: Long = 0L,                   // new field name
    val createdAt: Long = 0L,                   // backward compat
    val date: String = "",                      // legacy date string
    val receivedAt: Long? = null,
    val distributedAt: Long? = null
) {
    val isSaudiPayment: Boolean
        get() = paymentMethod.contains("مدى") || paymentMethod.contains("Mada") ||
                paymentMethod.contains("Apple") || paymentMethod.contains("STC") ||
                paymentMethod.contains("ائتمان") || paymentMethod.contains("Visa") ||
                paymentMethod.contains("Mastercard")

    /** Effective amount: tries numeric first, then parses the legacy string */
    val effectiveAmount: Double
        get() = if (amount > 0) amount
                else amountOrItem.replace(" ج.س", "").replace(" ر.س", "").replace(" ر.ي", "").replace(",", "").toDoubleOrNull() ?: 0.0

    /** Formatted amount for display */
    val amountFormatted: String
        get() = if (category == "عيني") amountOrItem
                else {
                    val cur = if (isSaudiPayment) "ر.س" else "ج.س"
                    "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(effectiveAmount)} $cur"
                }

    /** Effective timestamp */
    val effectiveTimestamp: Long get() = if (timestamp > 0) timestamp else createdAt

    /** Effective donor ID */
    val effectiveDonorId: String get() = donorId.ifBlank { userId }

    /** Effective case ID as string */
    val effectiveCaseId: String get() = caseId.ifBlank { caseIdLegacy?.toString() ?: "" }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "donationId"        to id,
        "caseId"            to effectiveCaseId,
        "caseTitle"         to caseTitle,
        "donorId"           to effectiveDonorId,
        "userId"            to effectiveDonorId,    // backward compat
        "donorName"         to donorName,
        "amount"            to effectiveAmount,
        "amountOrItem"      to (if (category == "عيني") amountOrItem else {
            val cur = if (isSaudiPayment) "ر.س" else "ج.س"
            "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(effectiveAmount)} $cur"
        }),
        "paymentMethod"     to paymentMethod,
        "category"          to category,
        "receiptUrl"        to receiptUrl,
        "paymentRef"        to paymentRef,
        "paymentGatewayName" to paymentGatewayName,
        "paymentStatus"    to paymentStatus,
        "receiptVerifiedBy" to receiptVerifiedBy,
        "paymentVerifiedAt" to paymentVerifiedAt,
        "paymentRejectedAt" to paymentRejectedAt,
        "donorMessage"      to donorMessage,
        "ocrText"           to ocrText,
        "aiSuspicionFlag"   to aiSuspicionFlag,
        "bbanAccountNumber" to bbanAccountNumber,
        "qrTransactionRef"  to qrTransactionRef,
        "bankName"          to bankName,
        "isRecurring"       to isRecurring,
        "recurringInterval" to recurringInterval,
        "recurringNextDate" to recurringNextDate,
        "recurringStatus"   to recurringStatus,
        "status"            to status,
        "timestamp"         to effectiveTimestamp,
        "createdAt"         to effectiveTimestamp,  // backward compat
        "date"              to date,
        "receivedAt"        to receivedAt,
        "distributedAt"     to distributedAt
    )


    companion object {
        fun fromFirestore(doc: com.google.firebase.firestore.DocumentSnapshot): DonationRecord? {
            if (!doc.exists()) return null
            val docId = doc.id

            val amount = getSafeDouble(doc, "amount")
            val amountOrItem = doc.getString("amountOrItem") ?: ""
            val ts = getSafeLong(doc, "timestamp").takeIf { it > 0 } ?: getSafeLong(doc, "createdAt")
            val donorId = doc.getString("donorId")?.takeIf { it.isNotBlank() }
                ?: doc.getString("userId") ?: ""
            val caseIdObj = doc.get("caseId")
            val caseId = if (caseIdObj is String) caseIdObj else ""
            val caseIdLegacy = if (caseIdObj is Number) caseIdObj.toInt() else null

            val receivedAtVal = doc.get("receivedAt")
            val receivedAt = when (receivedAtVal) {
                is com.google.firebase.Timestamp -> receivedAtVal.toDate().time
                is Long -> receivedAtVal
                else -> null
            }
            val distributedAtVal = doc.get("distributedAt")
            val distributedAt = when (distributedAtVal) {
                is com.google.firebase.Timestamp -> distributedAtVal.toDate().time
                is Long -> distributedAtVal
                else -> null
            }

            return DonationRecord(
                id                = docId,
                donationId        = doc.getString("donationId") ?: docId,
                caseId            = caseId,
                caseIdLegacy      = caseIdLegacy,
                caseTitle         = doc.getString("caseTitle") ?: "",
                donorId           = donorId,
                userId            = donorId,
                donorName         = doc.getString("donorName") ?: "",
                amount            = amount,
                amountOrItem      = amountOrItem,
                paymentMethod     = doc.getString("paymentMethod") ?: "",
                category          = doc.getString("category") ?: "مالي",
                receiptUrl        = doc.getString("receiptUrl") ?: "",
                paymentRef        = doc.getString("paymentRef") ?: "",
                paymentGatewayName = doc.getString("paymentGatewayName") ?: "",
                paymentStatus      = doc.getString("paymentStatus") ?: "pending",
                receiptVerifiedBy  = doc.getString("receiptVerifiedBy") ?: "",
                paymentVerifiedAt  = getSafeLongNullable(doc, "paymentVerifiedAt"),
                paymentRejectedAt  = getSafeLongNullable(doc, "paymentRejectedAt"),
                donorMessage       = doc.getString("donorMessage") ?: "",
                ocrText            = doc.getString("ocrText") ?: "",
                aiSuspicionFlag    = doc.getBoolean("aiSuspicionFlag") ?: false,
                bbanAccountNumber  = doc.getString("bbanAccountNumber") ?: "",
                qrTransactionRef  = doc.getString("qrTransactionRef") ?: "",
                bankName          = doc.getString("bankName") ?: "",
                isRecurring       = doc.getBoolean("isRecurring") ?: false,
                recurringInterval = doc.getString("recurringInterval") ?: "none",
                recurringNextDate = getSafeLong(doc, "recurringNextDate"),
                recurringStatus   = doc.getString("recurringStatus") ?: "none",
                status            = doc.getString("status") ?: "قيد الجمع",
                timestamp         = ts,
                createdAt         = ts,
                date              = doc.getString("date") ?: "",
                receivedAt        = receivedAt,
                distributedAt     = distributedAt
            )

        }

        private fun getSafeDouble(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Double {
            val obj = doc.get(field)
            return when (obj) {
                is Number -> obj.toDouble()
                is String -> obj.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        }

        private fun getSafeLong(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Long {
            val obj = doc.get(field)
            return when (obj) {
                is Number -> obj.toLong()
                is String -> obj.toLongOrNull() ?: 0L
                else -> 0L
            }
        }

        private fun getSafeLongNullable(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Long? {
            val obj = doc.get(field)
            return when (obj) {
                is Number -> obj.toLong()
                is Long -> obj
                is String -> obj.toLongOrNull()
                is com.google.firebase.Timestamp -> obj.toDate().time
                else -> null
            }
        }
    }
}
