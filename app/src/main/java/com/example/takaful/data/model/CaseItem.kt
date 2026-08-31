package com.example.takaful.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Represents a humanitarian case/campaign in Takaful.
 * Supports both old and new Firestore field names for backward compatibility.
 */
data class CaseItem(
    // ── Core Identity ──
    val id: String = "",                        // Firestore document ID
    val caseId: String = "",                    // duplicate of id for convenience
    val beneficiaryId: String = "",             // UID of the user who submitted this case

    // ── Content ──
    val title: String = "",
    val description: String = "",               // new name (was "story")
    val story: String = "",                     // kept for backward compatibility
    val location: String = "",
    val category: String = "",                  // "مالي" | "عيني" | "طبي" | "صحة"
    val urgencyLevel: String = "normal",        // "normal" | "urgent"

    // ── Financials ──
    val targetAmount: Double = 0.0,             // new name (was "amountRequired")
    val amountRequired: Double = 0.0,           // backward compat
    val raisedAmount: Double = 0.0,             // new name (was "amountRaised")
    val amountRaised: Double = 0.0,             // backward compat
    val progress: Float = 0f,

    // ── Media ──
    val imageUrl: String = "",                  // legacy single image/document URL
    val documentUrl: String = "",              // legacy fallback
    val imageUrls: List<String> = emptyList(), // new: multiple attached images URLs


    // ── Status & Workflow ──
    val status: String = "pending",             // "pending" | "active" | "completed" | "rejected"
    val createdBy: String = "",                 // backward compat (was beneficiaryId)

    // ── Volunteer Assignment ──
    val assignedVolunteerId: String = "",       // UID of the assigned volunteer
    val volunteerReport: String = "",           // volunteer's completion report
    val volunteerReportImage: String = "",      // volunteer's proof of delivery image URL

    // ── Timestamps ──
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** Effective target amount (accepts both field names) */
    val effectiveTarget: Double get() = if (targetAmount > 0) targetAmount else amountRequired

    /** Effective raised amount (accepts both field names) */
    val effectiveRaised: Double get() = if (raisedAmount > 0) raisedAmount else amountRaised

    /** Effective description (accepts both field names) */
    val effectiveDescription: String get() = description.ifBlank { story }

    val progressPercent: Float
        get() = if (effectiveTarget > 0) (effectiveRaised / effectiveTarget).toFloat().coerceIn(0f, 1f)
                else progress

    val amountFormatted: String
        get() = "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(effectiveTarget)} ج.س"

    val remainingFormatted: String
        get() = "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(effectiveTarget - effectiveRaised)} ج.س"

    val raisedFormatted: String
        get() = "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(effectiveRaised)} ج.س"

    val isAssigned: Boolean get() = assignedVolunteerId.isNotBlank()
    val isCompleted: Boolean get() = status == "completed"
    val isPending: Boolean get() = status == "pending"
    val isActive: Boolean get() = status == "active" || status == "approved"

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "caseId"               to id,
        "beneficiaryId"        to beneficiaryId,
        "title"                to title,
        "description"          to description,
        "location"             to location,
        "category"             to category,
        "urgencyLevel"         to urgencyLevel,
        "targetAmount"         to effectiveTarget,
        "amountRequired"       to effectiveTarget,          // backward compat
        "raisedAmount"         to effectiveRaised,
        "amountRaised"         to effectiveRaised,          // backward compat
        "progress"             to progressPercent.toDouble(),
        "imageUrls"           to imageUrls,
        "imageUrl"             to (imageUrls.firstOrNull().orEmpty().ifBlank { imageUrl }),
        "documentUrl"          to documentUrl.ifBlank { imageUrls.firstOrNull().orEmpty() }.ifBlank { imageUrl }, // backward compat
        "status"               to status,
        "createdBy"            to beneficiaryId,            // backward compat
        "assignedVolunteerId"  to assignedVolunteerId,
        "volunteerReport"      to volunteerReport,
        "volunteerReportImage" to volunteerReportImage,
        "createdAt"            to createdAt,
        "updatedAt"            to System.currentTimeMillis()
    )


    companion object {
        fun fromFirestore(doc: com.google.firebase.firestore.DocumentSnapshot): CaseItem? {

            if (!doc.exists()) return null
            val docId = doc.id

            // Support both old numeric id and new string id
            val targetAmt = getSafeDouble(doc, "targetAmount").takeIf { it > 0 }
                ?: getSafeDouble(doc, "amountRequired")
            val raisedAmt = getSafeDouble(doc, "raisedAmount").takeIf { it > 0 }
                ?: getSafeDouble(doc, "amountRaised")
            val desc = doc.getString("description")?.takeIf { it.isNotBlank() }
                ?: doc.getString("story")
                ?: ""
            val legacyImgUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() }
                ?: doc.getString("documentUrl")
                ?: ""

            val imgUrls: List<String> = run {
                val raw = doc.get("imageUrls")
                when (raw) {
                    is List<*> -> raw.mapNotNull { it?.toString()?.takeIf { s -> !s.isNullOrBlank() } }
                    else -> emptyList()
                }
            }

            val benfId = doc.getString("beneficiaryId")?.takeIf { it.isNotBlank() }
                ?: doc.getString("createdBy")
                ?: ""

            return CaseItem(
                id                    = docId,
                caseId                = doc.getString("caseId") ?: docId,
                beneficiaryId         = benfId,
                title                 = doc.getString("title") ?: "",
                description           = desc,
                story                 = desc,
                location              = doc.getString("location") ?: "",
                category              = doc.getString("category") ?: "",
                urgencyLevel          = doc.getString("urgencyLevel") ?: "normal",
                targetAmount          = targetAmt,
                amountRequired        = targetAmt,
                raisedAmount          = raisedAmt,
                amountRaised          = raisedAmt,
                progress              = getSafeDouble(doc, "progress").toFloat(),
                imageUrl              = legacyImgUrl,
                documentUrl           = legacyImgUrl,
                imageUrls            = if (imgUrls.isNotEmpty()) imgUrls else if (legacyImgUrl.isNotBlank()) listOf(legacyImgUrl) else emptyList(),

                status                = doc.getString("status") ?: "pending",
                createdBy             = benfId,
                assignedVolunteerId   = doc.getString("assignedVolunteerId") ?: "",
                volunteerReport       = doc.getString("volunteerReport") ?: "",
                volunteerReportImage  = doc.getString("volunteerReportImage") ?: "",
                createdAt             = getSafeLong(doc, "createdAt"),
                updatedAt             = getSafeLong(doc, "updatedAt"),
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
    }
}
