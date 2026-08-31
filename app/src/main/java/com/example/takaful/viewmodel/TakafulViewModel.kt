package com.example.takaful.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.takaful.data.model.CaseItem
import com.example.takaful.data.model.DonationRecord
import com.example.takaful.data.model.NotificationRecord
import com.example.takaful.data.model.ShareRecord
import com.example.takaful.data.model.UserProfile
import com.example.takaful.data.repository.*
import com.example.takaful.data.model.SystemSettings
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.takaful.utils.SupabaseStorageHelper
import com.google.firebase.firestore.FirebaseFirestore

class TakafulViewModel : ViewModel() {

    val authRepository     = AuthRepository()
    val casesRepository    = CasesRepository()
    val donationsRepository = DonationsRepository()
    val userRepository     = UserRepository()
    val chatsRepository    = ChatsRepository()
    val sharesRepository   = SharesRepository()
    val notificationsRepository = NotificationsRepository()
    val settingsRepository = SettingsRepository()

    // ── Exposed State Flows ──

    val cases: StateFlow<List<CaseItem>>       = casesRepository.cases
    val assignedCases: StateFlow<List<CaseItem>> = casesRepository.assignedCases
    val isLoadingCases: StateFlow<Boolean>     = casesRepository.isLoading
    val donations: StateFlow<List<DonationRecord>> = donationsRepository.donations
    val userProfile: StateFlow<UserProfile>    = userRepository.userProfile
    val photoURL: StateFlow<String>            = userRepository.photoURL
    val chatThreads                            = chatsRepository.chatThreads
    val chatMessages                           = chatsRepository.messages
    val shares: StateFlow<List<ShareRecord>>   = sharesRepository.shares
    val notifications: StateFlow<List<NotificationRecord>> = notificationsRepository.notifications
    val sysSettings: StateFlow<SystemSettings> = settingsRepository.sysSettings

    private val _isLoadingChats = MutableStateFlow(false)
    val isLoadingChats: StateFlow<Boolean> = _isLoadingChats.asStateFlow()

    val isLoggedIn get() = authRepository.isLoggedIn
    val currentUser get() = authRepository.currentUser

    // ── Initialization ──

    init {
        // Start listening to global settings (like maintenance mode) immediately
        settingsRepository.startListening()
    }

    /**
     * Initialize real-time listeners for cases and donations.
     * Also starts volunteer listener if the user is a volunteer.
     */
    fun initializeListeners() {
        casesRepository.startListening()
        donationsRepository.startListening()
        sharesRepository.startListening()
        notificationsRepository.startListening()
        // settingsRepository.startListening() is now called in init block
        // Start volunteer listener if applicable
        val uid = currentUser?.uid ?: return
        val profile = userProfile.value
        if (profile.isVolunteer) {
            casesRepository.startVolunteerListener(uid)
        }
    }

    /**
     * Call after profile is loaded to set up role-specific listeners.
     */
    fun initializeRoleListeners() {
        val uid = currentUser?.uid ?: return
        if (userProfile.value.isVolunteer) {
            casesRepository.startVolunteerListener(uid)
        }
    }

    fun initializeChats() {
        _isLoadingChats.value = true
        chatsRepository.startListeningWithCallback(userProfile.value.name.ifBlank { "مستخدم" }) {
            _isLoadingChats.value = false
        }
    }

    fun loadUserProfile(onComplete: () -> Unit = {}) {
        userRepository.loadCurrentUserProfile {
            initializeRoleListeners()
            onComplete()
        }
    }

    // ── Auth Actions ──

    fun login(
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        authRepository.loginWithEmail(email, password, context, onSuccess, onFailure)
    }

    fun register(
        name: String,
        phone: String,
        email: String,
        password: String,
        role: String,
        profileImageUri: Uri?,
        identityType: String? = null,
        identityNumber: String? = null,
        identityFrontUri: Uri? = null,
        identityBackUri: Uri? = null,
        registrationReason: String? = null,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        authRepository.register(
            name, phone, email, password, role, profileImageUri,
            identityType, identityNumber, identityFrontUri, identityBackUri, registrationReason,
            context, onSuccess, onFailure
        )
    }

    fun loginWithGoogle(
        context: Context,
        scope: CoroutineScope,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        authRepository.loginWithGoogle(context, scope, onLoading, onSuccess, onError)
    }

    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        authRepository.sendPasswordResetEmail(email, onSuccess, onFailure)
    }

    fun logout(context: Context) {
        authRepository.logout(context)
        userRepository.clearProfile()
    }

    // ── Cases Actions ──

    fun addCase(
        title: String,
        location: String,
        category: String,
        urgencyLevel: String = "عادي",
        targetAmount: Double,
        description: String,
        imageUri: Uri? = null,
        context: Context? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        casesRepository.addCase(
            title        = title,
            location     = location,
            category     = category,
            targetAmount = targetAmount,
            description  = description,
            urgencyLevel = urgencyLevel,
            imageUri     = imageUri,
            context      = context,
            onSuccess    = { onSuccess() },
            onFailure    = onFailure
        )
    }

    fun approveCase(caseId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        casesRepository.approveCase(caseId, onSuccess = {
            val caseItem = cases.value.find { it.id == caseId }
            if (caseItem != null && caseItem.beneficiaryId.isNotBlank()) {
                notificationsRepository.addNotification(
                    userId = caseItem.beneficiaryId,
                    title = "تمت الموافقة على الحالة",
                    message = "تمت الموافقة على حالة \"${caseItem.title}\" وهي الآن متاحة للتبرع.",
                    type = "case_approved"
                )
            }
            onSuccess()
        }, onFailure = onFailure)
    }

    fun rejectCase(caseId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        casesRepository.rejectCase(caseId, onSuccess, onFailure)

    // ── Volunteer Actions ──

    /** Assign a volunteer to a case (called from admin/supervisor) */
    fun assignVolunteer(
        caseId: String,
        volunteerId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = casesRepository.assignVolunteer(caseId, volunteerId, onSuccess, onFailure)

    /** Volunteer submits an "إنجاز" report for an assigned case */
    fun submitVolunteerReport(
        caseId: String,
        report: String,
        imageUri: Uri? = null,
        context: Context? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        casesRepository.submitVolunteerReport(caseId, report, imageUri, context, onSuccess = {
            val caseItem = cases.value.find { it.id == caseId }
            if (caseItem != null && caseItem.beneficiaryId.isNotBlank()) {
                notificationsRepository.addNotification(
                    userId = caseItem.beneficiaryId,
                    title = "اكتملت الحالة بنجاح",
                    message = "تم إنجاز وتسليم حالة \"${caseItem.title}\" بنجاح. الحمد لله!",
                    type = "case_completed"
                )
            }
            onSuccess()
        }, onFailure = onFailure)
    }

    /** Toggle volunteer availability */
    fun updateAvailability(
        isAvailable: Boolean,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = userRepository.updateAvailability(isAvailable, onSuccess, onFailure)

    // ── Donations Actions ──

    /** Recurring donations list */
    val recurringDonationsList get() = donationsRepository.recurringDonations

    /** New-schema donation (amount: Double) with BBAN/QR support */
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
        bbanAccountNumber: String = "",
        qrTransactionRef: String = "",
        bankName: String = "",
        paymentRef: String = "",
        paymentGatewayName: String = "",
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ): String {

        return donationsRepository.addDonation(
            caseId            = caseId,
            caseTitle         = caseTitle,
            amount            = amount,
            category          = category,
            paymentMethod     = paymentMethod,
            isRecurring       = isRecurring,
            recurringInterval = recurringInterval,
            receiptUrl        = receiptUrl,
            donorMessage      = donorMessage,
            bbanAccountNumber = bbanAccountNumber,
            qrTransactionRef  = qrTransactionRef,
            bankName          = bankName,
            paymentRef        = paymentRef,
            paymentGatewayName = paymentGatewayName,
            casesRepository   = casesRepository,

            onSuccess         = { donationId ->
                currentUser?.uid?.let { uid ->
                    notificationsRepository.addNotification(
                        userId = uid,
                        title = "اكتمل التبرع بنجاح",
                        message = "تم تسجيل تبرعك بـ $amount ج.س للحالة \"$caseTitle\". شكراً لعطائك!",
                        type = "donation_success"
                    )
                }
                onSuccess(donationId)
            },
            onFailure         = onFailure
        )
    }

    /**
     * Uploads the receipt using Supabase Storage, validates it using ML Kit (AI),
     * then saves the donation record.
     */
    fun uploadReceiptAndAddDonation(
        context: Context,
        receiptUri: Uri?,
        caseId: String,
        caseTitle: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        isRecurring: Boolean = false,
        recurringInterval: String = "none",
        donorMessage: String = "",
        bbanAccountNumber: String = "",
        qrTransactionRef: String = "",
        bankName: String = "",
        paymentRef: String = "",
        paymentGatewayName: String = "",
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (receiptUri == null) {
            // No receipt provided, flag as suspicious
            donationsRepository.addDonation(
                caseId = caseId, caseTitle = caseTitle, amount = amount, category = category,
                paymentMethod = paymentMethod, isRecurring = isRecurring, recurringInterval = recurringInterval,
                receiptUrl = "", donorMessage = donorMessage, bbanAccountNumber = bbanAccountNumber,
                qrTransactionRef = qrTransactionRef, bankName = bankName, paymentRef = paymentRef,
                paymentGatewayName = paymentGatewayName, ocrText = "لم يتم إرفاق إيصال",
                aiSuspicionFlag = true, casesRepository = casesRepository, 
                onSuccess = { donationId ->
                    currentUser?.uid?.let { uid ->
                        notificationsRepository.addNotification(
                            userId = uid,
                            title = "اكتمل التبرع بنجاح",
                            message = "تم تسجيل تبرعك بـ $amount ج.س للحالة \"$caseTitle\" (بدون إيصال). شكراً لعطائك!",
                            type = "donation_success"
                        )
                    }
                    onSuccess(donationId)
                },
                onFailure = onFailure
            )
            return
        }

        val txnId = paymentRef.ifBlank { qrTransactionRef }

        val proceedWithDonation = { url: String, ocrText: String, aiSuspicionFlag: Boolean ->
            donationsRepository.addDonation(
                caseId = caseId, caseTitle = caseTitle, amount = amount, category = category,
                paymentMethod = paymentMethod, isRecurring = isRecurring, recurringInterval = recurringInterval,
                receiptUrl = url, donorMessage = donorMessage, bbanAccountNumber = bbanAccountNumber,
                qrTransactionRef = qrTransactionRef, bankName = bankName, paymentRef = paymentRef,
                paymentGatewayName = paymentGatewayName, ocrText = ocrText,
                aiSuspicionFlag = aiSuspicionFlag, casesRepository = casesRepository,
                onSuccess = { donationId ->
                    currentUser?.uid?.let { uid ->
                        notificationsRepository.addNotification(
                            userId = uid,
                            title = "اكتمل التبرع بنجاح",
                            message = "تم تسجيل تبرعك بـ $amount ج.س للحالة \"$caseTitle\" وإرفاق الإيصال. شكراً لعطائك!",
                            type = "donation_success"
                        )
                    }
                    onSuccess(donationId)
                },
                onFailure = onFailure
            )
        }

        try {
            val image = InputImage.fromFilePath(context, receiptUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val ocrText = visionText.text
                    val lowerText = ocrText.lowercase()
                    val hasTxn = txnId.isNotBlank() && lowerText.contains(txnId.lowercase())
                    val successKeywords = listOf("success", "نجاح", "مكتمل", "completed", "successful", "succesful")
                    val hasSuccess = successKeywords.any { lowerText.contains(it) }
                    
                    val aiSuspicionFlag = !hasTxn || !hasSuccess

                    val path = "receipts/${System.currentTimeMillis()}.jpg"
                    SupabaseStorageHelper.uploadImage(context, receiptUri, path,
                        onSuccess = { url -> proceedWithDonation(url, ocrText, aiSuspicionFlag) },
                        onFailure = onFailure
                    )
                }
                .addOnFailureListener { e ->
                    // AI failed
                    val path = "receipts/${System.currentTimeMillis()}.jpg"
                    SupabaseStorageHelper.uploadImage(context, receiptUri, path,
                        onSuccess = { url -> proceedWithDonation(url, "فشل الذكاء الاصطناعي: ${e.message}", true) },
                        onFailure = onFailure
                    )
                }
        } catch (e: Exception) {
            val path = "receipts/${System.currentTimeMillis()}.jpg"
            SupabaseStorageHelper.uploadImage(context, receiptUri, path,
                onSuccess = { url -> proceedWithDonation(url, "تعذر قراءة الصورة", true) },
                onFailure = onFailure
            )
        }
    }

    /** Legacy overload (amountOrItem: String) for backward compat */
    fun addDonation(
        caseId: Int?,
        caseTitle: String,
        category: String,
        amountOrItem: String,
        paymentMethod: String,
        onSuccess: (Int) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ): Int {
        return donationsRepository.addDonation(
            caseId          = caseId,
            caseTitle       = caseTitle,
            category        = category,
            amountOrItem    = amountOrItem,
            paymentMethod   = paymentMethod,
            casesRepository = casesRepository,
            onSuccess       = onSuccess,
            onFailure       = onFailure
        )
    }

    fun getDonationById(id: String): DonationRecord? = donationsRepository.getDonationById(id)
    fun getDonationById(id: Int): DonationRecord?    = donationsRepository.getDonationById(id)

    /** Confirm donation received from donor (تأكيد الاستلام) */
    fun confirmDonationReceived(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        donationsRepository.confirmReceived(donationId, {

            // Also notify the donor
            val donation = getDonationById(donationId)
            val donorId = donation?.donorId ?: donation?.userId ?: ""
            if (donorId.isNotBlank()) {
                val notifRef = FirebaseFirestore.getInstance().collection("notifications").document()
                val record = NotificationRecord(
                    id = notifRef.id,
                    userId = donorId,
                    title = "تأكيد الاستلام",
                    message = "تم استلام تبرعك لـ \"${donation?.caseTitle ?: "تبرع عام"}\" من قبل إدارة تكافل.",
                    type = "donation_update"
                )
                notifRef.set(record.toFirestoreMap())
            }
            onSuccess()
        }, onFailure)
    }

    /** Confirm donation distributed to beneficiary (تأكيد التوزيع) — also updates case progress */
    fun confirmDonationDistributed(
        donationId: String,
        caseId: String,
        amount: Double,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        donationsRepository.confirmDistributed(
            donationId      = donationId,
            caseId          = caseId,
            amount          = amount,
            casesRepository = casesRepository,
            onSuccess       = {
                // Also notify the donor
                val donation = getDonationById(donationId)
                val donorId = donation?.donorId ?: donation?.userId ?: ""
                if (donorId.isNotBlank()) {
                    val notifRef = FirebaseFirestore.getInstance().collection("notifications").document()
                    val record = NotificationRecord(
                        id = notifRef.id,
                        userId = donorId,
                        title = "اكتمل التوزيع",
                        message = "تم تسليم تبرعك لـ \"${donation?.caseTitle ?: "تبرع عام"}\" للمستفيد بنجاح. شكراً لعطائك!",
                        type = "donation_update"
                    )
                    notifRef.set(record.toFirestoreMap())
                }
                onSuccess()
            },
            onFailure       = onFailure
        )
    }

    /** Delete a donation record */
    fun deleteDonation(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = donationsRepository.deleteDonation(donationId, onSuccess, onFailure)

    /** Update arbitrary fields on a donation */
    fun updateDonation(
        donationId: String,
        fields: Map<String, Any?>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = donationsRepository.updateDonation(donationId, fields, onSuccess, onFailure)

    fun verifyPaymentManually(
        donationId: String,
        receiptVerifiedBy: String,
        paymentRef: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        donationsRepository.setPaymentStatusVerified(
            donationId = donationId,
            receiptVerifiedBy = receiptVerifiedBy,
            paymentRef = paymentRef,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun rejectPayment(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        donationsRepository.setPaymentStatusRejected(
            donationId = donationId,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }


    /** Update donation status to any value with Firestore timestamps and case increments */
    // ── Recurring Donation Management ──

    fun pauseRecurringDonation(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = donationsRepository.pauseRecurringDonation(donationId, onSuccess, onFailure)

    fun resumeRecurringDonation(
        donationId: String,
        recurringInterval: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = donationsRepository.resumeRecurringDonation(donationId, recurringInterval, onSuccess, onFailure)

    fun cancelRecurringDonation(
        donationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = donationsRepository.cancelRecurringDonation(donationId, onSuccess, onFailure)

    fun updateDonationStatus(
        donationId: String,
        status: String,
        caseId: String = "",
        amount: Double = 0.0,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = donationsRepository.updateDonationStatus(
        donationId      = donationId,
        status          = status,
        caseId          = caseId,
        amount          = amount,
        casesRepository = casesRepository,
        onSuccess       = onSuccess,
        onFailure       = onFailure
    )

    // ── Share Campaign ──

    fun shareCampaign(
        caseId: String,
        caseTitle: String,
        platform: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = sharesRepository.recordShare(caseId, caseTitle, platform, onSuccess, onFailure)

    // ── Profile Actions ──

    fun updateProfile(
        name: String,
        phone: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = userRepository.updateProfile(name, phone, onSuccess, onFailure)

    fun updateProfilePicture(
        context: Context,
        uri: Uri,
        localPath: String?,
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = userRepository.updateProfilePicture(context, uri, localPath, onSuccess, onFailure)

    fun deleteProfilePicture(
        context: Context,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = userRepository.deleteProfilePicture(context, onSuccess, onFailure)

    // ── Chat Actions ──

    fun sendChatMessage(
        chatId: String,
        text: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = chatsRepository.sendMessage(
        chatId       = chatId,
        text         = text,
        userName     = userProfile.value.name.ifBlank { "مستخدم" },
        userPhotoUrl = userProfile.value.photoURL,
        onSuccess    = onSuccess,
        onFailure    = onFailure
    )

    fun editChatMessage(
        chatId: String,
        messageId: String,
        newText: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = chatsRepository.editMessage(chatId, messageId, newText, onSuccess, onFailure)

    fun deleteChatMessage(
        chatId: String,
        messageId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = chatsRepository.deleteMessage(chatId, messageId, onSuccess, onFailure)

    fun listenToMessages(chatId: String) = chatsRepository.listenToMessages(chatId)

    // ── Notifications ──

    fun markNotificationAsRead(notificationId: String) {
        notificationsRepository.markAsRead(notificationId)
    }

    fun markAllNotificationsAsRead() {
        notificationsRepository.markAllAsRead()
    }

    // ── Statistics & Smart Features ──

    fun getTotalDonationsFormatted(): String = donationsRepository.calculateTotalDonations()
    fun getCasesHelpedCount(): Int = donationsRepository.calculateCasesHelped()
    fun getMonthlyDonationTotals(): Map<String, Double> = donationsRepository.getMonthlyTotals()

    /** Get the suggested donation amount based on user's last donation */
    fun getSuggestedDonationAmount(): Double {
        val lastDonation = donations.value.maxByOrNull { it.effectiveTimestamp }
        return if (lastDonation != null && lastDonation.amount > 0) {
            lastDonation.amount
        } else {
            50.0 // Default suggested amount
        }
    }

    /** Recommend cases based on user's preferred donation category */
    fun getSmartRecommendations(): List<CaseItem> {
        val allApproved = cases.value.filter { it.status == "approved" && !it.isCompleted }
        if (allApproved.isEmpty()) return emptyList()

        // Find preferred category
        val categoryCount = mutableMapOf<String, Int>()
        donations.value.forEach {
            categoryCount[it.category] = (categoryCount[it.category] ?: 0) + 1
        }
        val preferredCategory = categoryCount.maxByOrNull { it.value }?.key

        return allApproved.sortedWith(
            compareByDescending<CaseItem> { it.category == preferredCategory }
                .thenByDescending { it.urgencyLevel == "عاجل" }
                .thenByDescending { (it.amountRaised ?: 0.0) / ((it.amountRequired ?: 1.0).coerceAtLeast(1.0)) }
        )
    }

    /** Get the single most urgent case for quick action */
    fun getMostUrgentCase(): CaseItem? {
        val allApproved = cases.value.filter { it.status == "approved" && !it.isCompleted }
        return allApproved.sortedWith(
            compareByDescending<CaseItem> { it.urgencyLevel == "عاجل" }
                .thenByDescending { (it.amountRaised ?: 0.0) / ((it.amountRequired ?: 1.0).coerceAtLeast(1.0)) }
        ).firstOrNull()
    }

    // ── Lifecycle ──

    override fun onCleared() {
        super.onCleared()
        casesRepository.stopListening()
        donationsRepository.stopListening()
        chatsRepository.stopListening()
        sharesRepository.stopListening()
        notificationsRepository.stopListening()
        settingsRepository.stopListening()
    }
}
