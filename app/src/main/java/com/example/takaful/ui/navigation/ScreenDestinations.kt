package com.example.takaful.ui.navigation

import kotlinx.serialization.Serializable

// تعريف المسارات لجميع شاشات نظام الحسابات
@Serializable object WelcomeRoute
@Serializable object LoginRoute
@Serializable object RegisterRoute

// المسار للشاشة الرئيسية بعد تسجيل الدخول
@Serializable object HomeRoute

// مسار شاشة تفاصيل الحالة
@Serializable
data class CaseDetailsRoute(
    val caseId: Int,
    val title: String,
    val category: String,
    val amount: String,
    val progress: Float,
    val location: String,
    val story: String
)

// مسار شاشة التبرع
@Serializable
data class DonationRoute(
    val caseId: Int? = null,
    val caseTitle: String = "تبرع عام",
    val category: String = "مالي"
)

// مسار شاشة تتبع التبرع
@Serializable
data class TrackingRoute(
    val donationId: Int
)

// مسار شاشة التبرعات الدورية
@Serializable object RecurringDonationsRoute
